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

import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import com.alibaba.nacos.core.remoting.event.RemotingEvent;
import com.alibaba.nacos.core.remoting.event.reactive.IEventPipelineReactive;
import com.alibaba.nacos.core.remoting.grpc.reactive.GrpcClientEventReactive;

/**
 * @author pbting
 * @date 2019-08-30 10:43 PM
 */
public abstract class GrpcClientEventListenerSupport<T extends RemotingEvent>
    implements IPipelineEventListener<RemotingEvent> {

    @Override
    public Class<? extends IEventPipelineReactive> pipelineReactivePartition() {
        return GrpcClientEventReactive.class;
    }

    @Override
    public boolean onEvent(RemotingEvent event, int listenerIndex) {

        return onRemotingEvent((T) event, listenerIndex);
    }

    public abstract boolean onRemotingEvent(T event, int listenerIndex);
}
