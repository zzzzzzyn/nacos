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
package com.alibaba.nacos.naming.push.grpc;

import com.alibaba.nacos.api.naming.push.SubscribeMetadata;
import com.alibaba.nacos.core.remoting.grpc.interactive.GrpcRequestStreamInteractive;
import com.alibaba.nacos.naming.push.AbstractPushClient;
import com.alibaba.nacos.naming.push.DataSource;

/**
 * @author pbting
 * @date 2019-08-31 6:21 PM
 */
public class GrpcPushClient extends AbstractPushClient {

    private GrpcRequestStreamInteractive responseStream;

    public GrpcPushClient(SubscribeMetadata subscribeMetadata, DataSource dataSource,
                          GrpcRequestStreamInteractive responseStream) {
        super(subscribeMetadata, dataSource);
        this.responseStream = responseStream;
    }

    @Override
    public String getAddrStr() {
        return null;
    }

    @Override
    public String getIp() {
        return null;
    }

    @Override
    public int getPort() {
        return 0;
    }
}
