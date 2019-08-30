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
package com.alibaba.nacos.client.naming.core.builder;

import com.alibaba.nacos.client.naming.core.EventDispatcher;
import com.alibaba.nacos.client.naming.core.IServiceChangedAwareStrategy;
import com.alibaba.nacos.client.naming.core.udp.UdpServiceChangedAwareStrategy;
import com.alibaba.nacos.client.naming.net.NamingProxy;
import com.alibaba.nacos.client.naming.utils.UtilAndComs;

/**
 * @author pbting
 * @date 2019-08-30 11:41 AM
 */
public final class ServiceChangedAwareStrategyBuilder {

    /**
     * give an default service changed aware strategy when initialize cause an exception
     */
    private static final IServiceChangedAwareStrategy UDP_SERVICE_CHANGED_AWARE_STRATEGY = new UdpServiceChangedAwareStrategy();

    private ServiceChangedStrategyConfig serviceChangedStrategyConfig;

    private ServiceChangedAwareStrategyBuilder(ServiceChangedStrategyConfig serviceChangedStrategyConfig) {
        this.serviceChangedStrategyConfig = serviceChangedStrategyConfig;
    }

    public static ServiceChangedAwareStrategyBuilder builder() {

        return new ServiceChangedAwareStrategyBuilder(new ServiceChangedStrategyConfig());
    }

    public static ServiceChangedAwareStrategyBuilder builder(ServiceChangedStrategyConfig serviceChangedStrategyConfig) {

        return new ServiceChangedAwareStrategyBuilder(serviceChangedStrategyConfig);
    }

    public ServiceChangedAwareStrategyBuilder setEventDispatcher(EventDispatcher eventDispatcher) {
        this.serviceChangedStrategyConfig.setEventDispatcher(eventDispatcher);
        return this;
    }

    public ServiceChangedAwareStrategyBuilder setNamingProxy(NamingProxy namingProxy) {
        this.serviceChangedStrategyConfig.setServerProxy(namingProxy);
        return this;
    }

    public ServiceChangedAwareStrategyBuilder setCacheDir(String cacheDir) {
        this.serviceChangedStrategyConfig.setCacheDir(cacheDir);
        return this;
    }

    public ServiceChangedAwareStrategyBuilder isLoadCacheAtStart(boolean loadCacheAtStart) {
        this.serviceChangedStrategyConfig.setLoadCacheAtStart(loadCacheAtStart);
        return this;
    }

    public ServiceChangedAwareStrategyBuilder setPollingThreadCount(int pollingThreadCount) {
        this.serviceChangedStrategyConfig.setPollingThreadCount(pollingThreadCount);
        return this;
    }

    public IServiceChangedAwareStrategy build(Class<? extends IServiceChangedAwareStrategy> clazz) {

        try {
            IServiceChangedAwareStrategy serviceChangedAwareStrategy = clazz.newInstance();
            serviceChangedAwareStrategy.initServiceChangedAwareStrategy(serviceChangedStrategyConfig);
            return serviceChangedAwareStrategy;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return UDP_SERVICE_CHANGED_AWARE_STRATEGY;
    }

    /**
     * some configuration for construct a {@IServiceChangedAwareStrategy}
     */
    public static class ServiceChangedStrategyConfig {
        private EventDispatcher eventDispatcher;
        private NamingProxy serverProxy;
        private String cacheDir;
        private boolean loadCacheAtStart;
        private int pollingThreadCount = UtilAndComs.DEFAULT_POLLING_THREAD_COUNT;

        public EventDispatcher getEventDispatcher() {
            return eventDispatcher;
        }

        public void setEventDispatcher(EventDispatcher eventDispatcher) {
            this.eventDispatcher = eventDispatcher;
        }

        public NamingProxy getServerProxy() {
            return serverProxy;
        }

        public void setServerProxy(NamingProxy serverProxy) {
            this.serverProxy = serverProxy;
        }

        public String getCacheDir() {
            return cacheDir;
        }

        public void setCacheDir(String cacheDir) {
            this.cacheDir = cacheDir;
        }

        public boolean isLoadCacheAtStart() {
            return loadCacheAtStart;
        }

        public void setLoadCacheAtStart(boolean loadCacheAtStart) {
            this.loadCacheAtStart = loadCacheAtStart;
        }

        public int getPollingThreadCount() {
            return pollingThreadCount;
        }

        public void setPollingThreadCount(int pollingThreadCount) {
            this.pollingThreadCount = pollingThreadCount;
        }
    }
}
