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
package com.alibaba.nacos.core.remoting.event;

/**
 * an event pipeline reactive.
 * A pipeline consisting of listeners with multiple event types.
 * <p>
 * You can use the abstract interface in this class to add event listeners of different time types.
 *
 * @author pbting
 * @date 2019-08-22 5:14 PM
 */
public interface IEventPipelineReactive {

    /**
     * Wake up a set of event listeners. This set of event listeners is executed in order
     *
     * @param event
     */
    void pipelineReactive(Event event);

    /**
     * Whether to include a practice type
     *
     * @param eventType
     * @return
     */
    boolean containsEventType(Integer eventType);

    /**
     * Add a listener for an event type. An eventType can correspond to multiple object listeners
     *
     * @param pipelineEventListener
     * @param eventType
     */
    void addListener(IPipelineEventListener pipelineEventListener, Integer eventType);

    /**
     * Remove an object listener from the specified event type
     *
     * @param objectListener
     * @param eventType
     */
    default void removeListener(IPipelineEventListener objectListener, Integer eventType) {

        // support remove a listener
    }

    /**
     * clear all of listeners
     */
    default void clearListener() {

        // suport clear all of listeners
    }

    /**
     * Add a new object listener after an event type
     *
     * @param objectListener
     * @param eventType
     */
    default void addLast(IPipelineEventListener objectListener, Integer eventType) {

        // add the listener to the last in the queue
    }

    /**
     * Add a new event listener in front of an event type
     */
    default void addFirst(IPipelineEventListener objectListener, Integer eventType) {

        // add the listener to the first in the queue
    }
}
