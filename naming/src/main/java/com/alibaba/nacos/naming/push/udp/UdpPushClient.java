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

import com.alibaba.nacos.api.naming.push.SubscribeMetadata;
import com.alibaba.nacos.naming.push.AbstractPushClient;
import com.alibaba.nacos.naming.push.DataSource;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;

/**
 * @author pbting
 * @date 2019-08-27 10:23 PM
 */
public class UdpPushClient extends AbstractPushClient<DatagramSocket> {

    private InetSocketAddress socketAddr;
    private Map<String, String[]> params;

    public UdpPushClient(SubscribeMetadata subscribeMetadata,
                         DataSource dataSource,
                         DatagramSocket pusher) {
        super(subscribeMetadata, dataSource, pusher);
        this.socketAddr = new InetSocketAddress(subscribeMetadata.getClientIp(), (int) subscribeMetadata.getPort());
    }

    public Map<String, String[]> getParams() {
        return params;
    }

    public void setParams(Map<String, String[]> params) {
        this.params = params;
    }


    public InetSocketAddress getSocketAddr() {
        return socketAddr;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSubscribeMetadata().getServiceName(), getSubscribeMetadata().getClusters(), socketAddr);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UdpPushClient)) {
            return false;
        }

        UdpPushClient other = (UdpPushClient) obj;
        SubscribeMetadata otherSubscribeMetadata = other.getSubscribeMetadata();
        SubscribeMetadata thisSubscribeMetadata = getSubscribeMetadata();
        return thisSubscribeMetadata.getServiceName().equals(otherSubscribeMetadata.getServiceName())
            && thisSubscribeMetadata.getClusters().equals(otherSubscribeMetadata.getClusters())
            && socketAddr.equals(other.socketAddr);
    }

}
