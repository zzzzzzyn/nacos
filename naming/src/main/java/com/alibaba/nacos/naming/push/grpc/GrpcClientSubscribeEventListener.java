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
import com.alibaba.nacos.api.naming.NamingCommonEventSinks;
import com.alibaba.nacos.api.naming.push.SubscribeMetadata;
import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.grpc.interactive.GrpcRequestStreamInteractive;
import com.alibaba.nacos.naming.controllers.InstanceController;
import com.alibaba.nacos.naming.push.DataSource;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * @author pbting
 * @date 2019-08-31 4:17 PM
 */
@Component
public class GrpcClientSubscribeEventListener extends AbstractGrpcClientEventListener {

    private DataSource dataSource;

    @Override
    public boolean onEvent(Event event, int listenerIndex) {

        GrpcRequestStreamInteractive requestStreamInteractive = event.getValue();

        String subscribeMetadataJson =
            requestStreamInteractive.getRequestPayload().getPayload().toStringUtf8();

        SubscribeMetadata subscribeMetadata =
            JSON.parseObject(subscribeMetadataJson, SubscribeMetadata.class);

        GrpcPushClient abstractPushClient =
            (GrpcPushClient) grpcEmitterService.getPushClientFactory().newPushClient(subscribeMetadata, dataSource, requestStreamInteractive);
        pushService.addClient(abstractPushClient);
        return true;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Collection<InstanceController> instanceControllers = this.applicationContext.getBeansOfType(InstanceController.class).values();
        this.dataSource = instanceControllers.iterator().next().getPushDataSource();
        super.afterSingletonsInstantiated();
    }

    @Override
    public String[] interestSinks() {

        return new String[]{NamingCommonEventSinks.SUBSCRIBE_SINK};
    }
}
