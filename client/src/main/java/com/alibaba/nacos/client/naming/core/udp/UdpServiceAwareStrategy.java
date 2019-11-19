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

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.client.naming.core.AbstractServiceAwareStrategy;
import com.alibaba.nacos.client.naming.core.builder.ServiceAwareStrategyBuilder;
import com.alibaba.nacos.client.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;

/**
 * aware service changed by udp strategy
 *
 * @author xuanyin
 */
public class UdpServiceAwareStrategy extends AbstractServiceAwareStrategy {

    private final Map<String, ScheduledFuture<?>> futureMap = new HashMap<String, ScheduledFuture<?>>();
    private UdpPushReceiver pushReceiver;
    private ScheduledExecutorService executor;

    /**
     * An empty constructor must be given.
     */
    public UdpServiceAwareStrategy() {
    }

    /**
     * @param serviceChangedStrategyConfig some configuration for init service changed aware strategy
     */
    @Override
    public void initServiceAwareStrategy(ServiceAwareStrategyBuilder.ServiceAwareStrategyConfig serviceChangedStrategyConfig) {

        executor = new ScheduledThreadPoolExecutor(serviceChangedStrategyConfig.getPollingThreadCount(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("com.alibaba.nacos.client.naming.updater");
                return thread;
            }
        });

        initCommonState(serviceChangedStrategyConfig);
        this.pushReceiver = new UdpPushReceiver(this);
    }

    @Override
    public void destroy() {
        super.destroy();
        executor.shutdown();
        pushReceiver.destroy();
        executor = null;
    }

    public ScheduledFuture<?> addTask(ServiceUpdateTask task) {
        return executor.schedule(task, DEFAULT_DELAY, TimeUnit.MILLISECONDS);
    }

    @Override
    public ServiceInfo getServiceInfo(String serviceName, String clusters) {
        try {
            return super.getServiceInfo(serviceName, clusters);
        } finally {
            scheduleUpdateIfAbsent(serviceName, clusters);
        }
    }

    public void scheduleUpdateIfAbsent(String serviceName, String clusters) {
        if (futureMap.get(ServiceInfo.getKey(serviceName, clusters)) != null) {
            return;
        }

        synchronized (futureMap) {
            if (futureMap.get(ServiceInfo.getKey(serviceName, clusters)) != null) {
                return;
            }

            ScheduledFuture<?> future = addTask(new ServiceUpdateTask(this, serviceName, clusters));
            futureMap.put(ServiceInfo.getKey(serviceName, clusters), future);
        }
    }

    /**
     * will query service info from server
     *
     * @param serviceName
     * @param clusters
     * @return
     */
    @Override
    public void updateService(String serviceName, String clusters) throws NacosException {
        String result = serverProxy.queryList(serviceName, clusters, pushReceiver.getUDPPort(), false);
        if (StringUtils.isNotEmpty(result)) {
            processServiceAwareResult(result);
        }
    }

    /**
     * when after execute {@ServiceUpdateTask} will re-schedule
     *
     * @param serviceUpdateTask
     */
    public void reScheduleServiceUpdateTask(ServiceUpdateTask serviceUpdateTask, long delayMillis) {
        executor.schedule(serviceUpdateTask, delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * refresh the pushClient for nacos server.
     *
     * @param serviceName
     * @param clusters
     * @return
     */
    public String refreshOnly(String serviceName, String clusters) {
        try {
            return serverProxy.queryList(serviceName, clusters, pushReceiver.getUDPPort(), false);
        } catch (Exception e) {
            NAMING_LOGGER.error("[NA] failed to update serviceName: " + serviceName, e);
        }

        return StringUtils.EMPTY;
    }
}
