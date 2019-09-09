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
import com.alibaba.nacos.client.naming.core.IServiceAwareStrategy;
import com.alibaba.nacos.client.naming.core.udp.UdpServiceAwareStrategy;
import com.alibaba.nacos.client.naming.net.NamingProxy;
import com.alibaba.nacos.client.naming.utils.UtilAndComs;

import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;

/**
 * @author pbting
 * @date 2019-08-30 11:41 AM
 */
public final class ServiceAwareStrategyBuilder {

    /**
     * give an default service changed aware strategy when initialize cause an exception
     */
    private static final IServiceAwareStrategy UDP_SERVICE_CHANGED_AWARE_STRATEGY = new UdpServiceAwareStrategy();

    private ServiceAwareStrategyConfig serviceChangedStrategyConfig;

    private ServiceAwareStrategyBuilder(ServiceAwareStrategyConfig serviceChangedStrategyConfig) {
        this.serviceChangedStrategyConfig = serviceChangedStrategyConfig;
    }

    public static ServiceAwareStrategyBuilder builder() {

        return new ServiceAwareStrategyBuilder(new ServiceAwareStrategyConfig());
    }

    public static ServiceAwareStrategyBuilder builder(ServiceAwareStrategyConfig serviceChangedStrategyConfig) {

        return new ServiceAwareStrategyBuilder(serviceChangedStrategyConfig);
    }

    public ServiceAwareStrategyBuilder setEventDispatcher(EventDispatcher eventDispatcher) {
        this.serviceChangedStrategyConfig.setEventDispatcher(eventDispatcher);
        return this;
    }

    public ServiceAwareStrategyBuilder setNamingProxy(NamingProxy namingProxy) {
        this.serviceChangedStrategyConfig.setServerProxy(namingProxy);
        return this;
    }

    public ServiceAwareStrategyBuilder setCacheDir(String cacheDir) {
        this.serviceChangedStrategyConfig.setCacheDir(cacheDir);
        return this;
    }

    public ServiceAwareStrategyBuilder isLoadCacheAtStart(boolean loadCacheAtStart) {
        this.serviceChangedStrategyConfig.setLoadCacheAtStart(loadCacheAtStart);
        return this;
    }

    public ServiceAwareStrategyBuilder setPollingThreadCount(int pollingThreadCount) {
        this.serviceChangedStrategyConfig.setPollingThreadCount(pollingThreadCount);
        return this;
    }

    public IServiceAwareStrategy build(Class<? extends IServiceAwareStrategy> clazz) {
        NAMING_LOGGER.info("build service aware strategy with " + clazz);
        try {
            IServiceAwareStrategy serviceChangedAwareStrategy = clazz.newInstance();
            serviceChangedAwareStrategy.initServiceAwareStrategy(serviceChangedStrategyConfig);
            return serviceChangedAwareStrategy;
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }

        return UDP_SERVICE_CHANGED_AWARE_STRATEGY;
    }

    /**
     * some configuration for construct a {@IServiceChangedAwareStrategy}
     */
    public static class ServiceAwareStrategyConfig {
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
