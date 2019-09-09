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
import com.alibaba.nacos.api.naming.push.PushPacket;
import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import com.alibaba.nacos.core.remoting.stream.IRemotingRequestStreamObserver;

import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;

/**
 * @author pbting
 * @date 2019-09-09 11:08 AM
 */
public class GrpcPushReceiver implements IRemotingRequestStreamObserver {

    private GrpcServiceAwareStrategy grpcServiceChangedAwareStrategy;

    public GrpcPushReceiver(GrpcServiceAwareStrategy grpcServiceChangedAwareStrategy) {
        this.grpcServiceChangedAwareStrategy = grpcServiceChangedAwareStrategy;
    }

    @Override
    public void onNext(InteractivePayload interactivePayload) {
        String pushPackJson = interactivePayload.getPayload().toStringUtf8();
        PushPacket pushPacket = JSON.parseObject(pushPackJson, PushPacket.class);
        NAMING_LOGGER.info("[gRPC]receive server push with, the data is " + pushPackJson);
        if (GrpcServiceAwareStrategy.PUSH_PACKET_DOM_TYPE.equals(pushPacket.getType()) ||
            GrpcServiceAwareStrategy.PUSH_PACKET_DOM_SERVICE.equals(pushPacket.getType())) {
            grpcServiceChangedAwareStrategy.processDataStreamResponse(pushPacket.getData());
        } else if (GrpcServiceAwareStrategy.PUSH_PACKET_DOM_DUMP.equals(pushPacket.getType())) {
            // dump data to server
        } else {
            // do nothing send ack only
        }
    }
}
