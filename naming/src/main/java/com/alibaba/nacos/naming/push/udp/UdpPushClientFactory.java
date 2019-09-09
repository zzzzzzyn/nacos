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

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.naming.push.SubscribeMetadata;
import com.alibaba.nacos.naming.push.AbstractPushClient;
import com.alibaba.nacos.naming.push.DataSource;
import com.alibaba.nacos.naming.push.IPushClientFactory;

import java.net.DatagramSocket;

/**
 * @author pbting
 * @date 2019-09-04 2:23 PM
 */
public class UdpPushClientFactory implements IPushClientFactory {

    private DatagramSocket pusher;

    public UdpPushClientFactory(DatagramSocket pusher) {
        this.pusher = pusher;
    }

    @Override
    public AbstractPushClient newPushClient(SubscribeMetadata subscribeMetadata, DataSource dataSource) {

        long port = subscribeMetadata.getPort();
        if (port <= Constants.PORT_IDENTIFY_NNTS || port > Constants.PORT_IDENTIFY_GRPC_BIGGER) {

            return null;
        }

        return new UdpPushClient(subscribeMetadata, dataSource, pusher);
    }
}
