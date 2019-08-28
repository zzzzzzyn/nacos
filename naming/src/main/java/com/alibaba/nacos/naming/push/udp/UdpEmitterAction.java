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

import com.alibaba.nacos.api.naming.push.AckEntry;
import com.alibaba.nacos.api.naming.push.AckPacket;
import com.alibaba.nacos.naming.core.Service;
import com.alibaba.nacos.naming.misc.Loggers;
import com.alibaba.nacos.naming.misc.UtilsAndCommons;
import com.alibaba.nacos.naming.push.AbstractPushClient;
import org.javatuples.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pbting
 * @date 2019-08-28 6:26 PM
 */
public class UdpEmitterAction implements Runnable {

    private Service service;
    private Collection<AbstractPushClient> pushClients;
    private long pushCacheMillis;
    private UdpEmitterService udpEmitterService;

    public UdpEmitterAction(Service service, Collection<AbstractPushClient> pushClients,
                            long pushCacheMillis, UdpEmitterService udpEmitterService) {
        this.service = service;
        this.pushClients = pushClients;
        this.pushCacheMillis = pushCacheMillis;
        this.udpEmitterService = udpEmitterService;
    }

    @Override
    public void run() {
        final String serviceName = service.getName();
        final String namespaceId = service.getNamespaceId();
        final long lastRefTime = System.nanoTime();
        final Map<String, Pair> cache = new HashMap<>(16);
        final long pushThreshodCacheMillis = 20000;
        try {
            pushClients.forEach(client -> {
                // 2. start to push
                String key = udpEmitterService.getPushCacheKey(service.getName(), client.getAgent());

                AckEntry ackEntry = null;
                {
                    // 2.1. get from cache
                    if (pushCacheMillis >= pushThreshodCacheMillis && cache.containsKey(key)) {
                        Pair pair = cache.get(key);
                        byte[] cacheRawDatum = (byte[]) pair.getValue0();
                        AckPacket ackPacket = (AckPacket) pair.getValue1();
                        ackEntry = udpEmitterService.prepareAckEntry(client, cacheRawDatum, ackPacket, lastRefTime);
                    }
                }

                {
                    // 2.2. cache is not hit,so will get from data source
                    if (ackEntry == null) {
                        ackEntry = udpEmitterService.prepareAckEntry(client, udpEmitterService.prepareHostsData(client), lastRefTime);
                        if (ackEntry != null) {
                            cache.put(key, new org.javatuples.Pair<>(ackEntry.getOrigin().getData(), ackEntry.getData()));
                        }
                    }
                }

                // 2.3. push by the ack entry
                udpEmitterService.udpPush(ackEntry);
                Loggers.PUSH.info("serviceName: {} changed, schedule push for: {}, agent: {}, key: {}",
                    client.getServiceName(), client.getAddrStr(), client.getAgent(), (ackEntry == null ? null : ackEntry.getKey()));
            });
        } finally {
            udpEmitterService.removeFutureMap(UtilsAndCommons.assembleFullServiceName(namespaceId, serviceName));
        }
    }
}
