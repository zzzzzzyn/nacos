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
import com.alibaba.nacos.naming.misc.Loggers;
import com.alibaba.nacos.naming.push.AbstractPushClient;
import com.alibaba.nacos.naming.push.DataSource;

/**
 * @author pbting
 * @date 2019-08-31 6:21 PM
 */
public class GrpcPushClient extends AbstractPushClient<GrpcRequestStreamInteractive> {

    public GrpcPushClient(SubscribeMetadata subscribeMetadata, DataSource dataSource,
                          GrpcRequestStreamInteractive responseStream) {
        super(subscribeMetadata, dataSource, responseStream);
    }

    @Override
    public GrpcRequestStreamInteractive getPusher() {
        return super.getPusher();
    }

    @Override
    public void destroy() {
        Loggers.GRPC_PUSH.info("destroy an gRPC push client for {}", this.toString());
        /**
         * must be call this method to tell http/2 the HttpStream is complete and will recover .
         *  Otherwise it may cause a memory leak
         */
        GrpcRequestStreamInteractive grpcRequestStreamInteractive = getPusher();
        try {
            grpcRequestStreamInteractive.onCompleted();
        } catch (Exception e) {
            Loggers.GRPC_PUSH.error("destroy a gRPC push client cause an exception.", e);
        }
    }
}
