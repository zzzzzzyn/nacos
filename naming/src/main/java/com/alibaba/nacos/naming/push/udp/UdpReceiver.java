/*
 * Copyright (C) 2019 the original author or authors.
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
package com.alibaba.nacos.naming.push.udp;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.naming.push.AckPacket;
import com.alibaba.nacos.naming.misc.Loggers;
import com.alibaba.nacos.naming.push.PushService;
import org.apache.commons.lang3.StringUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * @author pbting
 * @date 2019-08-28 9:19 AM
 */
public class UdpReceiver implements Runnable {

    private UdpEmitterService udpEmitter;
    private PushService pushService;

    public UdpReceiver(UdpEmitterService udpEmitter, PushService pushService) {
        this.udpEmitter = udpEmitter;
        this.pushService = pushService;
    }

    @Override
    public void run() {
        DatagramSocket source = udpEmitter.getEmitSource(StringUtils.EMPTY);
        while (true) {
            byte[] buffer = new byte[1024 * 64];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                source.receive(packet);

                String json = new String(packet.getData(), 0, packet.getLength(), Charset.forName("UTF-8")).trim();
                AckPacket ackPacket = JSON.parseObject(json, AckPacket.class);

                InetSocketAddress socketAddress = (InetSocketAddress) packet.getSocketAddress();
                String ip = socketAddress.getAddress().getHostAddress();
                int port = socketAddress.getPort();

                if (System.nanoTime() - ackPacket.getLastRefTime() > PushService.ACK_TIMEOUT_NANOS) {
                    Loggers.PUSH.warn("ack takes too long from {} ack json: {}", packet.getSocketAddress(), json);
                }

                String ackKey = udpEmitter.getACKKey(ip, port, ackPacket.getLastRefTime());
                pushService.removeAckEntry(ackKey);
                long pushCost = System.currentTimeMillis() - udpEmitter.getAndRemoveSendTime(ackKey, ackPacket.getLastRefTime());
                Loggers.PUSH.info("received ack: {} from: {}:, cost: {} ms, unacked: {}, total push: {}",
                    json, ip, port, pushCost, pushService.getAckMapSize(), pushService.getTotalPush());
                pushService.putPushCost(ackKey, pushCost);
            } catch (Throwable e) {
                Loggers.PUSH.error("[NACOS-PUSH] error while receiving ack data", e);
            }
        }
    }
}
