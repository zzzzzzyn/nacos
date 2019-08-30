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
package com.alibaba.nacos.client.naming.core.udp;

import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.client.naming.core.AbstractServiceChangedAwareStrategy;

import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;

/**
 * @author pbting
 * @date 2019-08-30 1:56 PM
 */
class ServiceUpdateTask implements Runnable {

    private UdpServiceChangedAwareStrategy udpServiceChangedAwareStrategy;
    private String clusters;
    private String serviceName;
    private long lastRefTime = Long.MAX_VALUE;

    public ServiceUpdateTask(AbstractServiceChangedAwareStrategy serviceChangedAwareStrategy,
                             String serviceName, String clusters) {
        this.udpServiceChangedAwareStrategy = (UdpServiceChangedAwareStrategy) serviceChangedAwareStrategy;
        this.serviceName = serviceName;
        this.clusters = clusters;
    }

    @Override
    public void run() {
        try {
            ServiceInfo serviceObj = udpServiceChangedAwareStrategy.getServiceInfo0(serviceName, clusters);

            if (serviceObj == null) {
                ServiceInfo serviceInfo = udpServiceChangedAwareStrategy.updateServiceAndNotify(serviceName, clusters);
                long delayMillis = serviceInfo != null ? serviceInfo.getCacheMillis() : AbstractServiceChangedAwareStrategy.DEFAULT_DELAY;
                udpServiceChangedAwareStrategy.reScheduleServiceUpdateTask(this, delayMillis);
                return;
            }

            if (serviceObj.getLastRefTime() <= lastRefTime) {
                serviceObj = udpServiceChangedAwareStrategy.updateServiceAndNotify(serviceName, clusters);
            } else {
                // if serviceName already updated by push, we should not override it
                // since the push data may be different from pull through force push
                udpServiceChangedAwareStrategy.refreshOnly(serviceName, clusters);
            }

            udpServiceChangedAwareStrategy.reScheduleServiceUpdateTask(this, serviceObj.getCacheMillis());

            if (serviceObj != null) {
                lastRefTime = serviceObj.getLastRefTime();
            }
        } catch (Throwable e) {
            NAMING_LOGGER.warn("[NA] failed to update serviceName: " + serviceName, e);
        }
    }
}
