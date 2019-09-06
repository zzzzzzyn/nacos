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
import com.alibaba.nacos.api.naming.utils.NamingUtils;
import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.interactive.IInteractive;
import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import com.alibaba.nacos.naming.core.Service;
import com.alibaba.nacos.naming.core.ServiceManager;
import com.alibaba.nacos.naming.misc.UtilsAndCommons;
import com.alibaba.nacos.naming.push.AbstractPushClient;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

/**
 * @author pbting
 * @date 2019-09-05 3:22 PM
 */
@Component
public class SubscribeDurationEventListener extends AbstractGrpcClientEventListener {

    private ServiceManager serviceManager;

    @Override
    public boolean onEvent(Event event, int listenerIndex) {

        IInteractive interactive = event.getValue();
        InteractivePayload requestPayload = interactive.getRequestPayload();

        String subscribeMetadataJson = requestPayload.getPayload().toStringUtf8();

        SubscribeMetadata subscribeMetadata = JSON.parseObject(subscribeMetadataJson, SubscribeMetadata.class);
        String serviceKey =
            UtilsAndCommons.assembleFullServiceName(subscribeMetadata.getNamespaceId(),
                subscribeMetadata.getServiceName());
        AbstractPushClient abstractPushClient = pushService.getPushClient(serviceKey, NamingUtils.getPushClientKey(subscribeMetadata));
        if (abstractPushClient == null) {
            // tell client re-request subscribe stream
            interactive.sendResponsePayload(InteractivePayload.newBuilder()
                .setPayload(ByteString.copyFrom(NamingCommonEventSinks.SUBSCRIBE_SINK.getBytes(Charset.forName("utf-8")))).build());
        } else {
            abstractPushClient.refresh();
            Service service = serviceManager.getService(subscribeMetadata.getNamespaceId(), subscribeMetadata.getServiceName());
            interactive.sendResponsePayload(InteractivePayload.newBuilder()
                .setPayload(ByteString.copyFrom(service.getChecksum().getBytes(Charset.forName("utf-8")))).build());
        }
        return true;
    }

    @Override
    public void afterSingletonsInstantiated() {
        serviceManager = this.applicationContext.getBean(ServiceManager.class);
        super.afterSingletonsInstantiated();
    }

    @Override
    public String[] interestSinks() {
        return new String[]{NamingCommonEventSinks.SUBSCRIBE_DURATION_SINK};
    }
}
