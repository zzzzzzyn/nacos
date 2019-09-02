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

import com.alibaba.nacos.api.naming.push.EventTypeMapping;
import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.event.StartupEvent;
import com.alibaba.nacos.core.remoting.event.listener.StartupServerEventListener;
import com.alibaba.nacos.core.remoting.event.reactive.IEventPipelineReactive;
import com.alibaba.nacos.core.remoting.event.reactive.SimpleRemotingEventPipelineReactive;
import com.alibaba.nacos.core.remoting.grpc.reactive.GrpcServerEventReactive;
import com.alibaba.nacos.core.remoting.manager.IServerRemotingManager;

import java.util.stream.Stream;

/**
 * @author pbting
 * @date 2019-08-31 4:46 PM
 */
public class StartupEventMappingListener extends StartupServerEventListener {

    @Override
    public boolean onStartup(StartupEvent event) {

        IServerRemotingManager serverRemotingManager = (IServerRemotingManager) event.getSource();

        final SimpleRemotingEventPipelineReactive remotingEventPipelineReactive =
            (SimpleRemotingEventPipelineReactive) serverRemotingManager.getAbstractEventPipelineReactive(GrpcServerEventReactive.class);

        Stream.of(EventTypeMapping.values())
            .forEach(eventTypeMapping ->
                remotingEventPipelineReactive.registerEventTypeMapping(eventTypeMapping.getEventTypeLabel(), eventTypeMapping.getEventType()));
        return true;
    }

    @Override
    public Class<? extends Event>[] interestEventTypes() {
        return new Class[]{StartupEvent.class};
    }

    @Override
    public Class<? extends IEventPipelineReactive> pipelineReactivePartition() {
        return GrpcServerEventReactive.class;
    }
}
