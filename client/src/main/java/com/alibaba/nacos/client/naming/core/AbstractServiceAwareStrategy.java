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
package com.alibaba.nacos.client.naming.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.client.monitor.MetricsMonitor;
import com.alibaba.nacos.client.naming.backups.FailoverReactor;
import com.alibaba.nacos.client.naming.cache.DiskCache;
import com.alibaba.nacos.client.naming.core.builder.ServiceAwareStrategyBuilder;
import com.alibaba.nacos.client.naming.net.NamingProxy;
import com.alibaba.nacos.client.utils.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;

/**
 * an abstract service change aware strategy
 *
 * @author pbting
 * @date 2019-08-30 11:38 AM
 */
public abstract class AbstractServiceAwareStrategy implements IServiceAwareStrategy {

    public static final long DEFAULT_DELAY = 1000L;
    public static final long UPDATE_HOLD_INTERVAL = 5000L;

    protected NamingProxy serverProxy;
    protected Map<String, ServiceInfo> serviceInfoMap;
    protected Map<String, Object> updatingMap;
    protected FailoverReactor failoverReactor;
    protected EventDispatcher eventDispatcher;
    protected String cacheDir;

    /**
     * @param serviceChangedStrategyConfig
     */
    protected void initCommonState(ServiceAwareStrategyBuilder.ServiceAwareStrategyConfig
                                       serviceChangedStrategyConfig) {

        this.eventDispatcher = serviceChangedStrategyConfig.getEventDispatcher();
        this.serverProxy = serviceChangedStrategyConfig.getServerProxy();
        this.cacheDir = serviceChangedStrategyConfig.getCacheDir();
        if (serviceChangedStrategyConfig.isLoadCacheAtStart()) {
            this.serviceInfoMap = new ConcurrentHashMap<String, ServiceInfo>(DiskCache.read(this.cacheDir));
        } else {
            this.serviceInfoMap = new ConcurrentHashMap<String, ServiceInfo>(16);
        }

        this.updatingMap = new ConcurrentHashMap<String, Object>(16);
        this.failoverReactor = new FailoverReactor(this, cacheDir);
    }

    @Override
    public ServiceInfo getServiceInfo(String serviceName, String clusters) {
        NAMING_LOGGER.debug("failover-mode: " + failoverReactor.isFailoverSwitch());
        String key = ServiceInfo.getKey(serviceName, clusters);
        if (failoverReactor.isFailoverSwitch()) {
            return failoverReactor.getService(key);
        }

        ServiceInfo serviceObj = getServiceInfo0(serviceName, clusters);

        if (null == serviceObj) {
            serviceObj = new ServiceInfo(serviceName, clusters);

            serviceInfoMap.put(serviceObj.getKey(), serviceObj);
            updatingMap.put(serviceName, new Object());
            updateServiceAndNotify(serviceName, clusters);
            updatingMap.remove(serviceName);

        } else if (updatingMap.containsKey(serviceName)) {
            if (UPDATE_HOLD_INTERVAL > 0) {
                // hold a moment waiting for update finish
                synchronized (serviceObj) {
                    try {
                        serviceObj.wait(UPDATE_HOLD_INTERVAL);
                    } catch (InterruptedException e) {
                        NAMING_LOGGER.error("[getServiceInfo] serviceName:" + serviceName + ", clusters:" + clusters, e);
                    }
                }
            }
        }
        return serviceInfoMap.get(serviceObj.getKey());
    }

    public ServiceInfo getServiceInfo0(String serviceName, String clusters) {

        String key = ServiceInfo.getKey(serviceName, clusters);

        return serviceInfoMap.get(key);
    }

    @Override
    public ServiceInfo getServiceInfoDirectlyFromServer(String serviceName, String clusters) throws NacosException {
        String result = serverProxy.queryList(serviceName, clusters, Constants.PORT_IDENTIFY_NNTS, false);
        if (StringUtils.isNotEmpty(result)) {
            return JSON.parseObject(result, ServiceInfo.class);
        }
        return null;
    }

    @Override
    public Map<String, ServiceInfo> getServiceInfoMap() {
        return Collections.unmodifiableMap(serviceInfoMap);
    }

    @Override
    public ServiceInfo processDataStreamResponse(String json) {
        ServiceInfo serviceInfo = JSON.parseObject(json, ServiceInfo.class);
        ServiceInfo oldService = serviceInfoMap.get(serviceInfo.getKey());
        if (serviceInfo.getHosts() == null || !serviceInfo.validate()) {
            //empty or error push, just ignore
            return oldService;
        }
        serviceInfo.setJsonFromServer(json);
        serviceInfoMap.put(serviceInfo.getKey(), serviceInfo);

        boolean changed;
        if (oldService != null) {
            if (oldService.getLastRefTime() > serviceInfo.getLastRefTime()) {
                NAMING_LOGGER.warn("out of date data received, old-t: " + oldService.getLastRefTime()
                    + ", new-t: " + serviceInfo.getLastRefTime());
            }
            changed = checkServiceIsChanged(serviceInfo, oldService);
        } else {
            changed = true;
            NAMING_LOGGER.info("init new ips(" + serviceInfo.ipCount() + ") service: " + serviceInfo.getKey() + " -> " + JSON
                .toJSONString(serviceInfo.getHosts()));
        }

        if (changed) {
            eventDispatcher.serviceChanged(serviceInfo);
            DiskCache.write(serviceInfo, cacheDir);
            NAMING_LOGGER.info("current ips:(" + serviceInfo.ipCount() + ") service: " + serviceInfo.getKey() +
                " -> " + JSON.toJSONString(serviceInfo.getHosts()));
        }

        MetricsMonitor.getServiceInfoMapSizeMonitor().set(serviceInfoMap.size());
        return serviceInfo;
    }

    /**
     * check service is changed from last
     *
     * @param serviceInfo
     * @param oldService
     * @return
     */
    protected boolean checkServiceIsChanged(ServiceInfo serviceInfo, ServiceInfo oldService) {

        // checksum is not equals ,must hosts is add or delete
        if (!serviceInfo.getChecksum().equals(oldService.getChecksum())) {
            return true;
        }

        if (serviceInfo.getHosts().size() != oldService.getHosts().size()) {
            return true;
        }

        // check is modify states of instance
        Set<Instance> modHosts = new HashSet<Instance>();
        Map<String, Instance> oldHostMap = new HashMap<String, Instance>(oldService.getHosts().size());
        for (Instance host : oldService.getHosts()) {
            oldHostMap.put(host.toInetAddr(), host);
        }
        Map<String, Instance> newHostMap = new HashMap<String, Instance>(serviceInfo.getHosts().size());
        for (Instance host : serviceInfo.getHosts()) {
            newHostMap.put(host.toInetAddr(), host);
        }

        checkStateIsChanged(modHosts, newHostMap, oldHostMap);
        checkStateIsChanged(modHosts, oldHostMap, newHostMap);
        return modHosts.size() > 0;
    }

    private void checkStateIsChanged(Set<Instance> modHosts, Map<String, Instance> oldHostMap, Map<String, Instance> newHostMap) {
        for (Map.Entry<String, Instance> entry : oldHostMap.entrySet()) {
            Instance host = entry.getValue();
            String key = entry.getKey();
            if (newHostMap.containsKey(key) &&
                !StringUtils.equals(host.toString(), newHostMap.get(key).toString())) {
                modHosts.add(host);
            }
        }
    }

    /**
     * will query service info from server and notify all of wait with oldService
     *
     * @param serviceName
     * @param clusters
     * @return
     */
    public void updateServiceAndNotify(String serviceName, String clusters) {
        ServiceInfo oldService = getServiceInfo0(serviceName, clusters);
        try {
            updateService(serviceName, clusters);
        } catch (Exception e) {
            NAMING_LOGGER.error("[NA] failed to update serviceName: " + serviceName, e);
        } finally {
            if (oldService != null) {
                synchronized (oldService) {
                    oldService.notifyAll();
                }
            }
        }
    }

    /**
     * update service
     *
     * @param serviceName
     * @param clusters
     * @throws NacosException
     */
    public abstract void updateService(String serviceName, String clusters) throws NacosException;

}
