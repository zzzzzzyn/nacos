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
package com.alibaba.nacos.naming.push;

import com.alibaba.nacos.api.naming.push.AckEntry;
import com.alibaba.nacos.naming.client.ClientInfo;
import com.alibaba.nacos.naming.core.Service;
import com.alibaba.nacos.naming.misc.Loggers;
import com.alibaba.nacos.naming.misc.SwitchDomain;
import com.alibaba.nacos.naming.misc.UtilsAndCommons;
import com.alibaba.nacos.naming.pojo.Subscriber;
import com.alibaba.nacos.naming.push.udp.UdpPushClient;
import org.codehaus.jackson.util.VersionUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author nacos
 */
@Component
public class PushService implements ApplicationContextAware, SmartInitializingSingleton {

    @Autowired
    private SwitchDomain switchDomain;

    private ApplicationContext applicationContext;

    public static final long ACK_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(10L);
    public static final int MAX_RETRY_TIMES = 1;
    private AtomicInteger totalPush = new AtomicInteger();
    private AtomicInteger failedPush = new AtomicInteger();

    private volatile ConcurrentMap<String, AckEntry> ackMap
        = new ConcurrentHashMap<>();
    private ConcurrentMap<String, ConcurrentMap<String, AbstractPushClientSupport>> clientMap
        = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<String, Long> pushCostMap = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.setName("com.alibaba.nacos.naming.push.retransmitter");
        return t;
    });

    @Override
    public void afterSingletonsInstantiated() {
        executorService.scheduleWithFixedDelay(() -> {
            try {
                removeClientIfZombie();
            } catch (Throwable e) {
                Loggers.PUSH.warn("[NACOS-PUSH] failed to remove client zombie");
            }
        }, 0, 20, TimeUnit.SECONDS);
    }

    public Map<String, Long> getPushCostMap() {
        return new HashMap<>(pushCostMap);
    }

    public void clearPushCostMap() {
        pushCostMap.clear();
    }

    public void putPushCost(String key, long costTime) {
        pushCostMap.put(key, costTime);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public int getTotalPush() {
        return totalPush.get();
    }

    public int increTotalPush() {
        return totalPush.incrementAndGet();
    }

    public void setTotalPush(int totalPush) {
        this.totalPush.set(totalPush);
    }

    public void addUdpPushClient(String namespaceId,
                                 String serviceName,
                                 String clusters,
                                 String agent,
                                 InetSocketAddress socketAddr,
                                 DataSource dataSource,
                                 String tenant,
                                 String app) {

        UdpPushClient client = new UdpPushClient(namespaceId,
            serviceName,
            clusters,
            agent,
            socketAddr,
            dataSource,
            tenant,
            app);
        addClient(client);
    }

    public void addClient(AbstractPushClientSupport client) {
        // client is stored by key 'serviceName' because notify event is driven by serviceName change
        String serviceKey = UtilsAndCommons.assembleFullServiceName(client.getNamespaceId(), client.getServiceName());
        ConcurrentMap<String, AbstractPushClientSupport> clients = clientMap.get(serviceKey);
        if (clients == null) {
            clientMap.putIfAbsent(serviceKey, new ConcurrentHashMap<>(1024));
            clients = clientMap.get(serviceKey);
        }

        AbstractPushClientSupport oldClient = clients.get(client.toString());
        if (oldClient != null) {
            oldClient.refresh();
        } else {
            AbstractPushClientSupport res = clients.putIfAbsent(client.toString(), client);
            if (res != null) {
                Loggers.PUSH.warn("client: {} already associated with key {}", res.getAddrStr(), res.toString());
            }
            Loggers.PUSH.debug("client: {} added for serviceName: {}", client.getAddrStr(), client.getServiceName());
        }
    }

    public Map<String, ConcurrentMap<String, AbstractPushClientSupport>> getClientMap() {
        return Collections.unmodifiableMap(clientMap);
    }

    public List<Subscriber> getClients(String serviceName, String namespaceId) {
        String serviceKey = UtilsAndCommons.assembleFullServiceName(namespaceId, serviceName);
        ConcurrentMap<String, AbstractPushClientSupport> clientConcurrentMap = clientMap.get(serviceKey);
        if (Objects.isNull(clientConcurrentMap)) {
            return null;
        }
        List<Subscriber> clients = new ArrayList<>();
        clientConcurrentMap.forEach((key, client) ->
            clients.add(new Subscriber(client.getAddrStr(), client.getAgent(), client.getApp(), client.getIp(), namespaceId, serviceName))
        );
        return clients;
    }

    public void removeClientIfZombie() {
        int size = 0;
        for (Map.Entry<String, ConcurrentMap<String, AbstractPushClientSupport>> entry : clientMap.entrySet()) {
            ConcurrentMap<String, AbstractPushClientSupport> clientConcurrentMap = entry.getValue();
            for (Map.Entry<String, AbstractPushClientSupport> entry1 : clientConcurrentMap.entrySet()) {
                AbstractPushClientSupport client = entry1.getValue();
                if (client.zombie(switchDomain)) {
                    clientConcurrentMap.remove(entry1.getKey());
                }
            }

            size += clientConcurrentMap.size();
        }
        if (Loggers.PUSH.isDebugEnabled()) {
            Loggers.PUSH.debug("[NACOS-PUSH] clientMap size: {}", size);
        }
    }

    public void schedulerReTransmitter(IReTransmitter reTransmitter) {
        executorService.schedule(reTransmitter, TimeUnit.NANOSECONDS.toMillis(PushService.ACK_TIMEOUT_NANOS),
            TimeUnit.MILLISECONDS);
    }

    public void putAckEntry(String key, AckEntry ackEntry) {

        ackMap.put(key, ackEntry);
    }

    public AckEntry removeAckEntry(String key) {
        return ackMap.remove(key);
    }

    public boolean containsAckEntry(String key) {
        return ackMap.containsKey(key);
    }

    public int getAckMapSize() {
        return ackMap.size();
    }

    public void serviceChanged(Service service) {
        this.applicationContext.publishEvent(new ServiceChangeEvent(this, service));
    }

    public boolean canEnablePush(String agent) {

        if (!switchDomain.isPushEnabled()) {
            return false;
        }

        ClientInfo clientInfo = new ClientInfo(agent);

        if (ClientInfo.ClientType.JAVA == clientInfo.type
            && clientInfo.version.compareTo(VersionUtil.parseVersion(switchDomain.getPushJavaVersion())) >= 0) {
            return true;
        } else if (ClientInfo.ClientType.DNS == clientInfo.type
            && clientInfo.version.compareTo(VersionUtil.parseVersion(switchDomain.getPushPythonVersion())) >= 0) {
            return true;
        } else if (ClientInfo.ClientType.C == clientInfo.type
            && clientInfo.version.compareTo(VersionUtil.parseVersion(switchDomain.getPushCVersion())) >= 0) {
            return true;
        } else if (ClientInfo.ClientType.GO == clientInfo.type
            && clientInfo.version.compareTo(VersionUtil.parseVersion(switchDomain.getPushGoVersion())) >= 0) {
            return true;
        }

        return false;
    }

    public List<AckEntry> getFailedPushes() {
        return new ArrayList(ackMap.values());
    }

    public int getFailedPushCount() {
        return ackMap.size() + failedPush.get();
    }

    public void setFailedPush(int failedPush) {
        this.failedPush.set(failedPush);
    }

    public int increFailedPush() {
        return this.failedPush.incrementAndGet();
    }

    public void resetPushState() {
        ackMap.clear();
    }

}
