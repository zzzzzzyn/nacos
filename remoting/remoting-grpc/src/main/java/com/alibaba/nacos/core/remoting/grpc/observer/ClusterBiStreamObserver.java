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
package com.alibaba.nacos.core.remoting.grpc.observer;

import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.event.reactive.SimpleRemotingEventPipelineReactive;
import com.alibaba.nacos.core.remoting.grpc.event.GrpcBiStreamEvent;
import com.alibaba.nacos.core.remoting.grpc.interactive.GrpcBiStreamInteractive;
import com.alibaba.nacos.core.remoting.interactive.IInteractive;
import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import io.grpc.stub.CallStreamObserver;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author pbting
 * 服务端异步请求的处理入口。即客户端对服务端请求，服务端的处理入口
 */
public class ClusterBiStreamObserver extends AbstractCallStreamObserver
    implements IInteractive {

    private SimpleRemotingEventPipelineReactive remotingEventPipelineReactive;

    private final static InternalLogger logger = InternalLoggerFactory
        .getInstance(ClusterBiStreamObserver.class);

    public ClusterBiStreamObserver(SimpleRemotingEventPipelineReactive remotingEventPipelineReactive,
                                   CallStreamObserver<InteractivePayload> responseStreamObserver) {
        super(responseStreamObserver);
        this.remotingEventPipelineReactive = remotingEventPipelineReactive;
    }

    /**
     * The entrance of Client Request
     *
     * @param payload
     */
    @Override
    public void onNext(InteractivePayload payload) {

        int eventTypeIndex = payload.getEventType();
        if (logger.isDebugEnabled()) {
            logger.debug("receive client request with the event type :" + eventTypeIndex);
        }

        Class<? extends Event> eventType = remotingEventPipelineReactive.getEventType(eventTypeIndex, GrpcBiStreamEvent.class);
        remotingEventPipelineReactive
            .reactive(new GrpcBiStreamEvent(this, new GrpcBiStreamInteractive(payload, interactiveStream), eventType));
    }

    @Override
    public AbstractCallStreamObserver getAbstractCallStreamObserver() {
        return this;
    }

    @Override
    public InteractivePayload getRequestPayload() {

        throw new UnsupportedOperationException();
    }
}
