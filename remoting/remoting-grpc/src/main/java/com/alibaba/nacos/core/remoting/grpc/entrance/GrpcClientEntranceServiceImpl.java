/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.nacos.core.remoting.grpc.entrance;

import com.alibaba.nacos.core.remoting.event.ClientRequestResponseEvent;
import com.alibaba.nacos.core.remoting.event.ClientRequestStreamEvent;
import com.alibaba.nacos.core.remoting.event.reactive.IEventPipelineReactive;
import com.alibaba.nacos.core.remoting.grpc.interactive.AbstractGrpcInteractive;
import com.alibaba.nacos.core.remoting.grpc.interactive.GrpcRequestResponseInteractive;
import com.alibaba.nacos.core.remoting.grpc.interactive.GrpcRequestStreamInteractive;
import com.alibaba.nacos.core.remoting.grpc.observer.ClientRequestStreamObserver;
import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import com.alibaba.nacos.core.remoting.proto.InteractiveServiceGrpc;
import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author pbting
 * Processing client request entry
 */
public class GrpcClientEntranceServiceImpl
    extends InteractiveServiceGrpc.InteractiveServiceImplBase {
    private final static InternalLogger logger = InternalLoggerFactory
        .getInstance(GrpcClientEntranceServiceImpl.class);

    private IEventPipelineReactive eventPipelineReactive;

    public GrpcClientEntranceServiceImpl(IEventPipelineReactive eventPipelineReactive) {
        this.eventPipelineReactive = eventPipelineReactive;
    }

    /**
     * @param responseObserver if you have some payload send to other nodes,you can use
     *                         the responseObserver
     * @return stream observer handler the the nodes request
     */
    @Override
    public StreamObserver<InteractivePayload> requestChannel(
        StreamObserver<InteractivePayload> responseObserver) {

        return new ClientRequestStreamObserver(this.eventPipelineReactive,
            (CallStreamObserver<InteractivePayload>) responseObserver);
    }

    @Override
    public void requestResponse(InteractivePayload request,
                                StreamObserver<InteractivePayload> responseObserver) {
        int eventType = request.getEventType();
        logger.info("receive client request with the event type :" + eventType);
        AbstractGrpcInteractive grpcInteractive = new GrpcRequestResponseInteractive(
            request, (CallStreamObserver) responseObserver);
        eventPipelineReactive
            .reactive(new ClientRequestResponseEvent(eventPipelineReactive, grpcInteractive, eventType));
    }

    @Override
    public void requestStream(InteractivePayload request,
                              StreamObserver<InteractivePayload> responseObserver) {
        int eventType = request.getEventType();
        logger.debug("receive client request with the event type :" + eventType);
        AbstractGrpcInteractive grpcRequestStreamInteractive = new GrpcRequestStreamInteractive(
            request, (CallStreamObserver) responseObserver);
        eventPipelineReactive
            .reactive(new ClientRequestStreamEvent(eventPipelineReactive, grpcRequestStreamInteractive, eventType));
    }

}
