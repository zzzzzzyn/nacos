/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nacos.client.naming.core.udp;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.naming.push.PushPacket;
import com.alibaba.nacos.client.utils.StringUtils;
import com.alibaba.nacos.common.utils.IoUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.Charset;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;

/**
 * @author xuanyin
 */
public class UdpPushReceiver implements Runnable {

    private ScheduledExecutorService executorService;

    private static final int UDP_MSS = 64 * 1024;

    private DatagramSocket udpSocket;

    private UdpServiceAwareStrategy changedAwareStrategy;

    private volatile boolean isDestroy = false;

    public UdpPushReceiver(UdpServiceAwareStrategy changedAwareStrategy) {
        try {
            this.changedAwareStrategy = changedAwareStrategy;
            udpSocket = new DatagramSocket();
            initUdpReceiver();
        } catch (Exception e) {
            NAMING_LOGGER.error("[NA] init udp socket failed", e);
        }
    }

    private void initUdpReceiver() {
        executorService = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("com.alibaba.nacos.naming.push.receiver");
                return thread;
            }
        });

        executorService.execute(this);
    }

    @Override
    public void run() {
        while (!isDestroy) {
            try {
                // byte[] is initialized with 0 full filled by default
                byte[] buffer = new byte[UDP_MSS];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                String json = new String(IoUtils.tryDecompress(packet.getData()), "UTF-8").trim();
                NAMING_LOGGER.info("received push data: " + json + " from " + packet.getAddress().toString());

                PushPacket pushPacket = JSON.parseObject(json, PushPacket.class);
                PushPacket pushResponseAck;
                if ("dom".equals(pushPacket.getType()) || "service".equals(pushPacket.getType())) {
                    changedAwareStrategy.processServiceAwareResult(pushPacket.getData());

                    // send ack to server
                    pushResponseAck = new PushPacket("push-ack", pushPacket.getLastRefTime(), StringUtils.EMPTY);
                } else if ("dump".equals(pushPacket.getType())) {
                    // dump data to server
                    pushResponseAck = new PushPacket("dump-ack", pushPacket.getLastRefTime(), StringUtils.escapeJavaScript(JSON.toJSONString(changedAwareStrategy.getServiceInfoMap())));
                } else {
                    // do nothing send ack only
                    pushResponseAck = new PushPacket("unknown-ack", pushPacket.getLastRefTime(), StringUtils.EMPTY);
                }

                byte[] ack = JSON.toJSONString(pushResponseAck).getBytes(Charset.forName("UTF-8"));
                udpSocket.send(new DatagramPacket(ack, ack.length, packet.getSocketAddress()));
            } catch (Exception e) {
                NAMING_LOGGER.error("[NA] error while receiving push data", e);
            }
        }
    }

    public int getUDPPort() {
        return udpSocket.getLocalPort();
    }

    /**
     * destroy some resource
     */
    public void destroy() {
        isDestroy = true;
        udpSocket.close();
        executorService.shutdown();
        executorService = null;
        udpSocket = null;
    }
}
