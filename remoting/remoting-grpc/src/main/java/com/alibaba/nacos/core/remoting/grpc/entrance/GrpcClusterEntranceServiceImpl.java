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
package com.alibaba.nacos.core.remoting.grpc.entrance;

import com.alibaba.nacos.core.remoting.event.IEventPipelineReactive;
import com.alibaba.nacos.core.remoting.event.ClientRequestResponseEvent;
import com.alibaba.nacos.core.remoting.grpc.interactive.AbstractGrpcInteractive;
import com.alibaba.nacos.core.remoting.grpc.interactive.GrpcRequestResponseInteractive;
import com.alibaba.nacos.core.remoting.grpc.interactive.GrpcRequestStreamInteractive;
import com.alibaba.nacos.core.remoting.grpc.observer.ClusterRequestStreamObserver;
import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import com.alibaba.nacos.core.remoting.proto.InteractiveServiceGrpc;
import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Entrance to handle node communication between clusters
 *
 * @author pbting
 * @date 2019-08-23 11:02 AM
 */
public class GrpcClusterEntranceServiceImpl
    extends InteractiveServiceGrpc.InteractiveServiceImplBase {

    private final static InternalLogger logger = InternalLoggerFactory
        .getInstance(GrpcClusterEntranceServiceImpl.class);

    private IEventPipelineReactive eventPipelineReactive;

    public GrpcClusterEntranceServiceImpl(IEventPipelineReactive eventPipelineReactive) {
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

        return new ClusterRequestStreamObserver(this.eventPipelineReactive,
            (CallStreamObserver<InteractivePayload>) responseObserver);
    }

    @Override
    public void requestResponse(InteractivePayload request,
                                StreamObserver<InteractivePayload> responseObserver) {
        int eventType = request.getEventType();
        logger.debug("receive client request with the event type :" + eventType);
        AbstractGrpcInteractive wareSwiftInteractive = new GrpcRequestResponseInteractive(
            request, (CallStreamObserver) responseObserver);
        eventPipelineReactive
            .pipelineReactive(new ClientRequestResponseEvent(this, wareSwiftInteractive, eventType));
    }

    @Override
    public void requestStream(InteractivePayload request,
                              StreamObserver<InteractivePayload> responseObserver) {
        int eventType = request.getEventType();
        logger.debug("receive client request with the event type :" + eventType);
        AbstractGrpcInteractive grpcInteractive = new GrpcRequestStreamInteractive(
            request, (CallStreamObserver) responseObserver);
        eventPipelineReactive
            .pipelineReactive(new ClientRequestResponseEvent(this, grpcInteractive, eventType));
    }
}
