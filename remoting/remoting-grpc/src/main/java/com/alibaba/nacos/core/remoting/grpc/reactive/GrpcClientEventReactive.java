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
package com.alibaba.nacos.core.remoting.grpc.reactive;

import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.event.reactive.AsyncEventPipelineReactive;
import io.netty.util.concurrent.EventExecutor;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * support async process client request
 *
 * @author pbting
 * @date 2019-08-23 12:08 AM
 */
public class GrpcClientEventReactive extends AsyncEventPipelineReactive {

    private HashMap<Integer, EventExecutor> eventExecutorMapping = new HashMap<>();

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    @Override
    public void reactive(Event event) {
        EventExecutor eventExecutor;
        final Lock readLock = readWriteLock.readLock();

        readLock.lock();
        try {
            eventExecutor = eventExecutorMapping.get(event.getEventType());
        } finally {
            readLock.unlock();
        }

        if (eventExecutor != null) {
            super.reactive0(event);
            return;
        }

        Lock writeLock = readWriteLock.writeLock();
        try {
            writeLock.lock();
            eventExecutor = eventExecutorMapping.get(event.getEventType());
            if (eventExecutor == null) {
                eventExecutor = eventExecutorGroup.next();
                eventExecutorMapping.put(event.getEventType(), eventExecutor);
            }
        } finally {
            writeLock.unlock();
        }
        event.setEventExecutor(eventExecutor);
        super.reactive0(event);
    }
}
