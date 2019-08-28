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

import com.alibaba.nacos.naming.push.AbstractPushClient;
import com.alibaba.nacos.naming.push.DataSource;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;

/**
 * @author pbting
 * @date 2019-08-27 10:23 PM
 */
public class UdpPushClient extends AbstractPushClient {

    private InetSocketAddress socketAddr;
    private DataSource dataSource;
    private Map<String, String[]> params;

    public Map<String, String[]> getParams() {
        return params;
    }

    public void setParams(Map<String, String[]> params) {
        this.params = params;
    }

    public UdpPushClient(String namespaceId,
                         String serviceName,
                         String clusters,
                         String agent,
                         InetSocketAddress socketAddr,
                         DataSource dataSource,
                         String tenant,
                         String app) {
        super(namespaceId, serviceName, clusters, agent, tenant, app);
        this.socketAddr = socketAddr;
        this.dataSource = dataSource;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    public UdpPushClient(InetSocketAddress socketAddr) {
        this.socketAddr = socketAddr;
    }

    @Override
    public String getAddrStr() {
        return getIp() + ":" + getPort();
    }

    @Override
    public String getIp() {
        return getSocketAddr().getAddress().getHostAddress();
    }

    @Override
    public int getPort() {
        return getSocketAddr().getPort();
    }

    public InetSocketAddress getSocketAddr() {
        return socketAddr;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, clusters, socketAddr);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UdpPushClient)) {
            return false;
        }

        UdpPushClient other = (UdpPushClient) obj;
        return serviceName.equals(other.serviceName) && clusters.equals(other.clusters) && socketAddr.equals(other.socketAddr);
    }

    @Override
    public String toString() {
        return serviceName + "@" + clusters + "@" + getAddrStr() + "@" + agent;
    }

}
