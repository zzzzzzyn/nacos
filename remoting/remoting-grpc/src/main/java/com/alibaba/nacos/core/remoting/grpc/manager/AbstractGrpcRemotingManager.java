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
package com.alibaba.nacos.core.remoting.grpc.manager;

import com.alibaba.nacos.core.remoting.event.IAttachListenerHook;
import com.alibaba.nacos.core.remoting.event.reactive.IEventPipelineReactive;
import com.alibaba.nacos.core.remoting.manager.IRemotingManager;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author pbting
 * @date 2019-08-22 10:55 PM
 */
public abstract class AbstractGrpcRemotingManager implements IRemotingManager, IAttachListenerHook {

    private ConcurrentHashMap<String, StreamObserver> stringStreamObserverConcurrentHashMap =
        new ConcurrentHashMap<>();

    private HashMap<Class<? extends IEventPipelineReactive>, IEventPipelineReactive> eventReactivePartition = new HashMap<>();

    public <V> void mappingStreamReplayProcessor(String streamTopicIdentify,
                                                 V streamReplayProcessor) {
        if (stringStreamObserverConcurrentHashMap.contains(streamTopicIdentify)) {
            return;
        }

        stringStreamObserverConcurrentHashMap.put(streamTopicIdentify,
            (StreamObserver) streamReplayProcessor);
    }

    @Override
    public IEventPipelineReactive getAbstractEventPipelineReactive(Class<? extends IEventPipelineReactive> eventReactivePartition) {
        return this.eventReactivePartition.get(eventReactivePartition);
    }

    @Override
    public void initEventPipelineReactive(IEventPipelineReactive eventReactive) {
        eventReactivePartition.put(eventReactive.getClass(), eventReactive);
    }
}
