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

import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import com.alibaba.nacos.core.remoting.event.RecyclableEvent;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author pbting
 * @date 2019-08-28 2:39 PM
 */
public class EventLoopReactive extends AsyncEventReactive {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(EventLoopReactive.class);

    public EventLoopReactive() {
    }

    public EventLoopReactive(MultithreadEventExecutorGroup multithreadEventExecutorGroup) {
        super(multithreadEventExecutorGroup);
    }

    @Override
    public void reactive(final Event event) {
        if (!(event instanceof RecyclableEvent)) {
            logger.error("the event in loop does not matcher the recycle type . {}", event.getClass().getCanonicalName());
            return;
        }

        RecyclableEvent recyclableEvent = (RecyclableEvent) event;
        eventExecutorGroup.schedule(() -> super.reactive0(event), recyclableEvent.getRecycleInterval(), TimeUnit.SECONDS);
    }

    @Override
    protected <T extends Event> void listenerPerform(final List<IPipelineEventListener> objectListeners, final T event) {

        this.listenerPerform0(objectListeners, event);
    }

    @Override
    protected <T extends Event> void listenerPerform0(List<IPipelineEventListener> objectListeners, T event) {

        super.listenerPerform0(objectListeners, event);

        RecyclableEvent recyclableEvent = (RecyclableEvent) event;
        if (recyclableEvent.isCancel()) {
            // exit event loop
            return;
        }

        // Enter the next event cycle
        eventExecutorGroup.schedule(() -> super.reactive0(event), recyclableEvent.getRecycleInterval(), TimeUnit.SECONDS);
    }
}
