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
import com.alibaba.nacos.naming.core.Service;
import com.alibaba.nacos.naming.misc.Loggers;
import com.alibaba.nacos.naming.misc.SwitchDomain;
import com.alibaba.nacos.naming.misc.UtilsAndCommons;
import com.alibaba.nacos.naming.push.AbstractEmitterSupport;
import com.alibaba.nacos.naming.push.AbstractPushClientSupport;
import com.alibaba.nacos.naming.push.PushService;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author pbting
 * @date 2019-08-28 9:00 AM
 */
public class UdpEmitterService extends AbstractEmitterSupport {

    private DatagramSocket udpSocket;
    private Map<String, Future> futureMap = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<String, Long> udpSendTimeMap
        = new ConcurrentHashMap<>();

    private final ScheduledExecutorService udpSender = Executors.newSingleThreadScheduledExecutor(runnable -> {
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
            Thread inThread = new Thread(receiver);
            inThread.setDaemon(true);
            inThread.setName("com.alibaba.nacos.naming.push.receiver");
            inThread.start();
        } catch (SocketException e) {
            Loggers.SRV_LOG.error("[NACOS-PUSH] failed to init push service");
        }
    }

    @Override
    public DatagramSocket getEmitSource() {
        return udpSocket;
    }

    @Override
    public void emitter(Service service) {
        // merge some change events to reduce the push frequency:
        if (futureMap.containsKey(UtilsAndCommons.assembleFullServiceName(service.getNamespaceId(), service.getName()))) {
            return;
        }

        final PushService pushService = applicationContext.getBean(PushService.class);
        final SwitchDomain switchDomain = applicationContext.getBean(SwitchDomain.class);

        final String serviceName = service.getName();
        final String namespaceId = service.getNamespaceId();
        final ConcurrentMap<String, AbstractPushClientSupport> clients =
            pushService.getClientMap().get(UtilsAndCommons.assembleFullServiceName(namespaceId, serviceName));

        Future future = udpSender.schedule(() -> {
            try {
                Loggers.PUSH.info(serviceName + " is changed, add it to push queue.");
                if (MapUtils.isEmpty(clients)) {
                    return;
                }

                Map<String, Object> cache = new HashMap<>(16);
                long lastRefTime = System.nanoTime();
                for (AbstractPushClientSupport client : clients.values()) {
                    if (client.zombie(switchDomain)) {
                        Loggers.PUSH.debug("client is zombie: " + client.toString());
                        clients.remove(client.toString());
                        Loggers.PUSH.debug("client is zombie: " + client.toString());
                        continue;
                    }

                    AckEntry ackEntry;
                    Loggers.PUSH.debug("push serviceName: {} to client: {}", serviceName, client.toString());
                    String key = getPushCacheKey(serviceName, client.getAgent());
                    byte[] compressData = null;
                    Map<String, Object> data = null;
                    if (switchDomain.getDefaultPushCacheMillis() >= 20000 && cache.containsKey(key)) {
                        org.javatuples.Pair pair = (org.javatuples.Pair) cache.get(key);
                        compressData = (byte[]) (pair.getValue0());
                        data = (Map<String, Object>) pair.getValue1();

                        Loggers.PUSH.debug("[PUSH-CACHE] cache hit: {}:{}", serviceName, client.getAddrStr());
                    }

                    if (compressData != null) {
                        ackEntry = prepareAckEntry(client, compressData, data, lastRefTime);
                    } else {
                        ackEntry = prepareAckEntry(client, prepareHostsData(client), lastRefTime);
                        if (ackEntry != null) {
                            cache.put(key, new org.javatuples.Pair<>(ackEntry.getOrigin().getData(), ackEntry.getData()));
                        }
                    }

                    Loggers.PUSH.info("serviceName: {} changed, schedule push for: {}, agent: {}, key: {}",
                        client.getServiceName(), client.getAddrStr(), client.getAgent(), (ackEntry == null ? null : ackEntry.getKey()));

                    udpPush(ackEntry);
                }
            } catch (Exception e) {
                Loggers.PUSH.error("[NACOS-PUSH] failed to push serviceName: {} to client, error: {}", serviceName, e);

            } finally {
                futureMap.remove(UtilsAndCommons.assembleFullServiceName(namespaceId, serviceName));
            }
        }, 1000, TimeUnit.MILLISECONDS);

        futureMap.put(UtilsAndCommons.assembleFullServiceName(namespaceId, serviceName), future);
    }

    public String getACKKey(String host, int port, long lastRefTime) {
        return StringUtils.strip(host) + "," + port + "," + lastRefTime;
    }

    private AckEntry prepareAckEntry(AbstractPushClientSupport client, Map<String, Object> data, long lastRefTime) {
        if (MapUtils.isEmpty(data)) {
            Loggers.PUSH.error("[NACOS-PUSH] pushing empty data for client is not allowed: {}", client);
            return null;
        }

        data.put("lastRefTime", lastRefTime);

        // we apply lastRefTime as sequence num for further ack
        String key = getACKKey(client.getIp(), client.getPort(), lastRefTime);

        String dataStr = JSON.toJSONString(data);
        UdpPushClient udpPushClient = (UdpPushClient) client;
        try {
            byte[] dataBytes = dataStr.getBytes(StandardCharsets.UTF_8);
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
        if (ackEntry == null) {
            Loggers.PUSH.error("[NACOS-PUSH] ackEntry is null.");
            return null;
        }

        if (ackEntry.getRetryTimes() > PushService.MAX_RETRY_TIMES) {
            Loggers.PUSH.warn("max re-push times reached, retry times {}, key: {}", ackEntry.getRetryTimes(), ackEntry.getKey());
            pushService.removeAckEntry(ackEntry.getKey());
            udpSendTimeMap.remove(ackEntry.getKey());
            pushService.increFailedPush();
            return ackEntry;
        }

        try {
            if (!pushService.containsAckEntry(ackEntry.getKey())) {
                pushService.increTotalPush();
            }
            pushService.putAckEntry(ackEntry.getKey(), ackEntry);
            udpSendTimeMap.put(ackEntry.getKey(), System.currentTimeMillis());

            Loggers.PUSH.info("send udp packet: " + ackEntry.getKey());
            udpSocket.send(ackEntry.getOrigin());

            ackEntry.increaseRetryTime();
            pushService.schedulerReTransmitter(new UdpReTransmitter(ackEntry, this));
            return ackEntry;
        } catch (Exception e) {
            Loggers.PUSH.error("[NACOS-PUSH] failed to push data: {} to client: {}, error: {}",
                ackEntry.getData(), ackEntry.getOrigin().getAddress().getHostAddress(), e);
            pushService.removeAckEntry(ackEntry.getKey());
            udpSendTimeMap.remove(ackEntry.getKey());
            pushService.increFailedPush();
            return null;
        }
    }

    private AckEntry prepareAckEntry(AbstractPushClientSupport client, byte[] dataBytes, Map<String, Object> data,
                                     long lastRefTime) {
        UdpPushClient udpPushClient = (UdpPushClient) client;
        String key = getACKKey(udpPushClient.getSocketAddr().getAddress().getHostAddress(),
            udpPushClient.getSocketAddr().getPort(),
            lastRefTime);
        DatagramPacket packet;
        try {
            packet = new DatagramPacket(dataBytes, dataBytes.length, udpPushClient.getSocketAddr());
            AckEntry ackEntry = new AckEntry(key, packet);
            // we must store the key be fore send, otherwise there will be a chance the
            // ack returns before we put in
            ackEntry.setData(data);
            return ackEntry;
        } catch (Exception e) {
            Loggers.PUSH.error("[NACOS-PUSH] failed to prepare data: {} to client: {}, error: {}",
                data, udpPushClient.getSocketAddr(), e);
        }

        return null;
    }

    public long getSendTime(String key) {
        return udpSendTimeMap.get(key);
    }

    public void removeSendTime(String key) {

        udpSendTimeMap.remove(key);
    }
}
