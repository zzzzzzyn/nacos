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
package com.alibaba.nacos.client.naming.core.grpc;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.naming.NamingCommonEventSinks;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.api.naming.push.SubscribeMetadata;
import com.alibaba.nacos.core.remoting.channel.AbstractRemotingChannel;
import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import com.alibaba.nacos.core.remoting.event.RecyclableEvent;
import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import com.google.protobuf.ByteString;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;

/**
 * @author pbting
 * @date 2019-09-05 9:33 PM
 */
public class SubscribeDurationEventListener implements IPipelineEventListener<RecyclableEvent> {

    private GrpcServiceAwareStrategy grpcServiceChangedAwareStrategy;

    public SubscribeDurationEventListener(GrpcServiceAwareStrategy grpcServiceChangedAwareStrategy) {
        this.grpcServiceChangedAwareStrategy = grpcServiceChangedAwareStrategy;
    }

    @Override
    public boolean onEvent(RecyclableEvent event, int listenerIndex) {
        SubscribeMetadata subscribeMetadata = event.getValue();
        String jsonContent = JSON.toJSONString(subscribeMetadata);
        AbstractRemotingChannel remotingChannel =
            event.getParameter(GrpcServiceAwareStrategy.EVENT_CONTEXT_CHANNEL);

        InteractivePayload response = remotingChannel.requestResponse(InteractivePayload.newBuilder().setSink(NamingCommonEventSinks.SUBSCRIBE_DURATION_SINK)
            .setPayload(ByteString.copyFrom(jsonContent.getBytes(Charset.forName("utf-8")))).build());

        ServiceInfo serviceInfo = event.getParameter(GrpcServiceAwareStrategy.PUSH_PACKET_DOM_SERVICE);
        String responsePayload = response.getPayload().toStringUtf8();

        if (NamingCommonEventSinks.SUBSCRIBE_SINK.equals(responsePayload)) {
            grpcServiceChangedAwareStrategy.requestSubscribeStream(subscribeMetadata, true);
            grpcServiceChangedAwareStrategy.updateSubscribeDuration(subscribeMetadata, serviceInfo);
            event.setCancel(true);
        } else if (!Constants.NULL_STRING.equals(responsePayload)
            && !serviceInfo.getChecksum().equals(responsePayload)) {
            NAMING_LOGGER.info("[gRPC] subscribe duration aware checksum is different from server response. local is {},server is {}", serviceInfo.getChecksum(), responsePayload);
            grpcServiceChangedAwareStrategy.updateServiceAndNotify(subscribeMetadata.getServiceName(), subscribeMetadata.getClusters());
            serviceInfo = grpcServiceChangedAwareStrategy.getServiceInfo(subscribeMetadata.getServiceName(), subscribeMetadata.getClusters());
            // use the newest to override
            event.setParameter(GrpcServiceAwareStrategy.PUSH_PACKET_DOM_SERVICE, serviceInfo);
            event.setRecycleInterval((int) TimeUnit.MILLISECONDS.toSeconds(serviceInfo.getCacheMillis()));
        }
        return true;
    }

    @Override
    public String[] interestSinks() {
        return new String[]{NamingCommonEventSinks.SUBSCRIBE_DURATION_SINK};
    }
}
