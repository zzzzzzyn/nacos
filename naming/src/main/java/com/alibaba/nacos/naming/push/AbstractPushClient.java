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
package com.alibaba.nacos.naming.push;

import com.alibaba.nacos.naming.misc.SwitchDomain;

/**
 * @author pbting
 * @date 2019-08-28 9:46 AM
 */
public abstract class AbstractPushClient implements IPushClient {

    protected String namespaceId;
    protected String serviceName;
    protected String clusters;
    protected String agent;
    protected String tenant;
    protected String app;
    protected long lastRefTime = System.currentTimeMillis();

    public AbstractPushClient() {

    }

    public AbstractPushClient(String namespaceId, String serviceName,
                              String clusters, String agent,
                              String tenant, String app) {
        this.namespaceId = namespaceId;
        this.serviceName = serviceName;
        this.clusters = clusters;
        this.agent = agent;
        this.tenant = tenant;
        this.app = app;
    }

    public String getNamespaceId() {
        return namespaceId;
    }

    public void setNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getClusters() {
        return clusters;
    }

    public void setClusters(String clusters) {
        this.clusters = clusters;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    @Override
    public boolean zombie(SwitchDomain switchDomain) {
        return System.currentTimeMillis() - lastRefTime > switchDomain.getPushCacheMillis(serviceName);
    }

    @Override
    public void refresh() {
        lastRefTime = System.currentTimeMillis();
    }
}
