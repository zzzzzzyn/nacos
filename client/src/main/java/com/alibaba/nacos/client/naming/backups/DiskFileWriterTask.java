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
package com.alibaba.nacos.client.naming.backups;

import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.client.naming.cache.DiskCache;
import com.alibaba.nacos.client.naming.core.IServiceAwareStrategy;
import com.alibaba.nacos.client.naming.utils.UtilAndComs;
import com.alibaba.nacos.client.utils.StringUtils;

import java.util.Map;
import java.util.TimerTask;

/**
 * @author pbting
 * @date 2019-08-30 1:34 PM
 */
class DiskFileWriterTask extends TimerTask {

    private IServiceAwareStrategy hostReactor;
    private String failoverDir;

    public DiskFileWriterTask(IServiceAwareStrategy hostReactor, String failoverDir) {
        this.hostReactor = hostReactor;
        this.failoverDir = failoverDir;
    }

    @Override
    public void run() {
        Map<String, ServiceInfo> map = hostReactor.getServiceInfoMap();
        for (Map.Entry<String, ServiceInfo> entry : map.entrySet()) {
            ServiceInfo serviceInfo = entry.getValue();
            if (StringUtils.equals(serviceInfo.getKey(), UtilAndComs.ALL_IPS) || StringUtils.equals(
                serviceInfo.getName(), UtilAndComs.ENV_LIST_KEY)
                || StringUtils.equals(serviceInfo.getName(), "00-00---000-ENV_CONFIGS-000---00-00")
                || StringUtils.equals(serviceInfo.getName(), "vipclient.properties")
                || StringUtils.equals(serviceInfo.getName(), "00-00---000-ALL_HOSTS-000---00-00")) {
                continue;
            }

            DiskCache.write(serviceInfo, failoverDir);
        }
    }
}
