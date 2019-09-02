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

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.naming.push.GrpcClientSubscribeEvent;
import com.alibaba.nacos.api.naming.push.SubscribeMetadata;
import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import com.alibaba.nacos.core.remoting.grpc.interactive.GrpcRequestStreamInteractive;
import com.alibaba.nacos.naming.push.DataSource;
import com.alibaba.nacos.naming.push.PushService;

/**
 * @author pbting
 * @date 2019-08-31 4:17 PM
 */
public class GrpcClientSubscribeEventListener extends GrpcClientEventListenerSupport
    implements IPipelineEventListener<GrpcClientSubscribeEvent> {

    private PushService pushService;
    private DataSource dataSource;

    public GrpcClientSubscribeEventListener(GrpcEmitterService grpcEmitterService,
                                            PushService pushService, DataSource dataSource) {
        super(grpcEmitterService);
        this.pushService = pushService;
        this.dataSource = dataSource;
    }

    @Override
    public boolean onEvent(GrpcClientSubscribeEvent event, int listenerIndex) {

        GrpcRequestStreamInteractive requestStreamInteractive = event.getValue();

        String subscribeMetadataJson =
            requestStreamInteractive.getRequestPayload().getPayload().toStringUtf8();

        SubscribeMetadata subscribeMetadata =
            JSON.parseObject(subscribeMetadataJson, SubscribeMetadata.class);
        this.pushService.addClient(new GrpcPushClient(subscribeMetadata, dataSource,
            requestStreamInteractive));
        return true;
    }

    @Override
    public Class<? extends Event>[] interestEventTypes() {

        return new Class[]{GrpcClientSubscribeEvent.class};
    }
}
