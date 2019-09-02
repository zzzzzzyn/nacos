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
import com.alibaba.nacos.api.naming.push.AckEntry;
import com.alibaba.nacos.api.naming.push.AckPacket;
import com.alibaba.nacos.naming.core.Service;
import com.alibaba.nacos.naming.misc.Loggers;
import com.alibaba.nacos.naming.misc.SwitchDomain;
import com.alibaba.nacos.naming.misc.UtilsAndCommons;
import com.alibaba.nacos.naming.push.AbstractEmitter;
import com.alibaba.nacos.naming.push.AbstractPushClient;
import com.alibaba.nacos.naming.push.PushService;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author nacos
 * @author pbting
 * @date 2019-08-28 9:00 AM
 */
public class UdpEmitterService extends AbstractEmitter {

    private DatagramSocket udpSocket;

    private final ScheduledExecutorService udpSender = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2, (runnable) -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.setName(UdpEmitterService.class.getCanonicalName());
        return t;
    });

    public UdpEmitterService(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    public void initEmitter() {
        initReceiver();
    }

    public void initReceiver() {
        try {
            udpSocket = new DatagramSocket();
            UdpReceiver receiver = new UdpReceiver(this, applicationContext.getBean(PushService.class));
            ThreadPoolExecutor threadPoolExecutor =
                new ThreadPoolExecutor(1, 1, 1, TimeUnit.DAYS, new LinkedBlockingQueue<>(), (runnable) -> {
                    Thread inThread = new Thread(runnable);
                    inThread.setDaemon(true);
                    inThread.setName("com.alibaba.nacos.naming.push.receiver");
                    return inThread;
                });
            threadPoolExecutor.execute(receiver);
        } catch (SocketException e) {
            Loggers.SRV_LOG.error("[NACOS-PUSH] failed to init push service");
        }
    }

    @Override
    public DatagramSocket getEmitSource(String sourceKey) {
        return udpSocket;
    }

    @Override
    public void emitter(Service service) {
        final String serviceName = service.getName();
        final String namespaceId = service.getNamespaceId();
        // merge some change events to reduce the push frequency:
        String emitterKey = UtilsAndCommons.assembleFullServiceName(namespaceId, serviceName);
        if (isContainsFutureMap(emitterKey)) {
            return;
        }

        final PushService pushService = applicationContext.getBean(PushService.class);
        final SwitchDomain switchDomain = applicationContext.getBean(SwitchDomain.class);
        final Map<String, AbstractPushClient> clients = pushService.getPushClients(emitterKey);
        if (MapUtils.isEmpty(clients)) {
            return;
        }
        Future future =
            udpSender.schedule(new UdpEmitterAction(service, clients.values(),
                    switchDomain.getDefaultPushCacheMillis(), this),
                1000, TimeUnit.MILLISECONDS);

        putFutureMap(emitterKey, future);
    }

    public String getACKKey(String host, int port, long lastRefTime) {
        return StringUtils.strip(host) + "," + port + "," + lastRefTime;
    }

    public AckEntry prepareAckEntry(AbstractPushClient client, AckPacket data, long lastRefTime) {
        if (data == null) {
            Loggers.PUSH.error("[NACOS-PUSH] pushing empty data for client is not allowed: {}", client);
            return null;
        }

        data.setLastRefTime(lastRefTime);
        // we apply lastRefTime as sequence num for further ack
        String key = getACKKey(client.getIp(), client.getPort(), lastRefTime);

        String dataStr = JSON.toJSONString(data);
        UdpPushClient udpPushClient = (UdpPushClient) client;
        byte[] dataBytes = dataStr.getBytes(StandardCharsets.UTF_8);
        try {
            dataBytes = compressIfNecessary(dataBytes);
            DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length, udpPushClient.getSocketAddr());
            // we must store the key be fore send, otherwise there will be a chance the
            // ack returns before we put in
            AckEntry ackEntry = new AckEntry(key, packet);
            ackEntry.setData(data);
            return ackEntry;
        } catch (Exception e) {
            Loggers.PUSH.error("[NACOS-PUSH] failed to prepare data: {} to client: {}, error: {}",
                data, udpPushClient.getSocketAddr(), e);
            return null;
        }
    }

    public AckEntry udpPush(AckEntry ackEntry) {
        PushService pushService = applicationContext.getBean(PushService.class);
        // 1. check conditions for udp send
        {
            if (checkSendConditions(ackEntry, pushService)) {
                return ackEntry;
            }
        }

        // 2. put some udp push metric
        {
            if (null == pushService.putAckEntry(ackEntry.getKey(), ackEntry)) {
                pushService.increTotalPush();
            }
            // override the send time. remove will in more than max retry times and receive udp response successfully
            sendTimeMap.put(ackEntry.getKey(), System.currentTimeMillis());
        }

        // 3. do udp send
        {
            try {
                udpSocket.send(ackEntry.getOrigin());
                ackEntry.increaseRetryTime();
                return ackEntry;
            } catch (Exception e) {
                Loggers.PUSH.error("[NACOS-PUSH] failed to push data: {} to client: {}, error: {}",
                    ackEntry.getData(), ackEntry.getOrigin().getAddress().getHostAddress(), e);
                pushService.increFailedPush();
                return null;
            } finally {
                pushService.schedulerReTransmitter(new UdpReTransmitter(pushService, ackEntry, this));
            }
        }
    }

    private boolean checkSendConditions(AckEntry ackEntry, PushService pushService) {
        if (ackEntry == null) {
            return true;
        }

        if (ackEntry.getRetryTimes() > PushService.MAX_RETRY_TIMES) {
            Loggers.PUSH.warn("max re-push times reached, retry times {}, key: {}", ackEntry.getRetryTimes(), ackEntry.getKey());
            pushService.removeAckEntry(ackEntry.getKey());
            getAndRemoveSendTime(ackEntry.getKey(), -1);
            pushService.increFailedPush();
            return true;
        }
        return false;
    }

    public AckEntry prepareAckEntry(AbstractPushClient client, byte[] dataBytes,
                                    AckPacket data, long lastRefTime) {
        UdpPushClient udpPushClient = (UdpPushClient) client;
        String key = getACKKey(udpPushClient.getSocketAddr().getAddress().getHostAddress(),
            udpPushClient.getSocketAddr().getPort(), lastRefTime);
        DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length, udpPushClient.getSocketAddr());
        AckEntry ackEntry = new AckEntry(key, packet);
        // we must store the key be fore send, otherwise there will be a chance the
        // ack returns before we put in
        ackEntry.setData(data);
        return ackEntry;
    }
}
