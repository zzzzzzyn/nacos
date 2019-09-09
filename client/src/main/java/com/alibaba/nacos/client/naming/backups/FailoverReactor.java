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
package com.alibaba.nacos.client.naming.backups;

import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.client.naming.core.AbstractServiceAwareStrategy;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @author nkorange
 */
public class FailoverReactor {

    private String failoverDir;
    private AbstractServiceAwareStrategy serviceChangedAwareStrategy;
    private Map<String, ServiceInfo> serviceMap = new ConcurrentHashMap<String, ServiceInfo>();
    private Map<String, String> switchParams = new ConcurrentHashMap<String, String>();
    private static final long DAY_PERIOD_MINUTES = 24 * 60;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("com.alibaba.nacos.naming.failover");
            return thread;
        }
    });

    public FailoverReactor(AbstractServiceAwareStrategy serviceChangedAwareStrategy, String cacheDir) {
        this.serviceChangedAwareStrategy = serviceChangedAwareStrategy;
        this.failoverDir = cacheDir + "/failover";
        this.init();
    }

    public void init() {

        executorService.scheduleWithFixedDelay(new SwitchRefresher(this), 0L, 5000L, TimeUnit.MILLISECONDS);
        executorService.scheduleWithFixedDelay(new DiskFileWriterTask(serviceChangedAwareStrategy, getFailoverDir()), 30, DAY_PERIOD_MINUTES, TimeUnit.MINUTES);
        // backup file on startup if failover directory is empty.
        executorService.schedule(new BackupFileCheckOnStartup(serviceChangedAwareStrategy, getFailoverDir()), 10000L, TimeUnit.MILLISECONDS);
    }

    public String getFailoverDir() {
        return failoverDir;
    }

    public void putSwitchParam(String key, String param) {
        switchParams.put(key, param);
    }

    public boolean isFailoverSwitch() {
        return Boolean.parseBoolean(switchParams.get("failover-mode"));
    }

    public void overrideServiceMap(Map<String, ServiceInfo> serviceMap) {

        this.serviceMap = serviceMap;
    }

    public ServiceInfo getService(String key) {
        ServiceInfo serviceInfo = serviceMap.get(key);

        if (serviceInfo == null) {
            serviceInfo = new ServiceInfo();
            serviceInfo.setName(key);
        }

        return serviceInfo;
    }
}
