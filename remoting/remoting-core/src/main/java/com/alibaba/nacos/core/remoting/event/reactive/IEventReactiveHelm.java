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
package com.alibaba.nacos.core.remoting.event.reactive;

import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import com.alibaba.nacos.core.remoting.event.filter.IEventReactiveFilter;

import java.util.Collection;

/**
 * an event pipeline reactive.
 * A pipeline consisting of listeners with multiple event types.
 * <p>
 * You can use the abstract interface in this class to add event listeners of different time types.
 *
 * @author pbting
 * @date 2019-08-22 5:14 PM
 */
public interface IEventReactiveHelm {

    /**
     * register event reactive filter
     *
     * @param reactiveFilters
     */
    void registerEventReactiveFilter(Collection<? extends IEventReactiveFilter> reactiveFilters);

    /**
     * Whether to include a practice type
     *
     * @param eventSink
     * @return
     */
    boolean containsEventSink(String eventSink);

    /**
     * Add a listenersSinkRegistry for an event type. An eventType can correspond to multiple object listeners
     *
     * @param pipelineEventListener
     */
    void addListener(IPipelineEventListener pipelineEventListener);

    /**
     * Remove an object listenersSinkRegistry from the specified event type
     *
     * @param objectListener
     */
    default void removeListener(IPipelineEventListener objectListener) {

        // support remove a listenersSinkRegistry
    }

    /**
     * clear all of listeners
     */
    default void clearListener() {

        // suport clear all of listeners
    }

    /**
     * Add a new object listenersSinkRegistry after an event type
     *
     * @param objectListener
     */
    default void addLast(IPipelineEventListener objectListener) {

        // add the listenersSinkRegistry to the last in the queue
    }

    /**
     * Add a new event listenersSinkRegistry in front of an event type
     */
    default void addFirst(IPipelineEventListener objectListener) {

        // add the listenersSinkRegistry to the first in the queue
    }
}
