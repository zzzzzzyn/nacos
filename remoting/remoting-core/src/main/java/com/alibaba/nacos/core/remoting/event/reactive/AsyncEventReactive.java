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
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;

/**
 * @author pbting
 * @date 2019-08-23 1:48 PM
 */
public class AsyncEventReactive extends DefaultEventReactive {

    private static final MultithreadEventExecutorGroup DEFAULT_EVENT_EXECUTOR =
        new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() * 2,
            (runnable) -> {
                Thread thread = new Thread(runnable);
                thread.setDaemon(false);
                thread.setName(AsyncEventReactive.class.getName());
                return thread;
            });

    protected MultithreadEventExecutorGroup eventExecutorGroup = DEFAULT_EVENT_EXECUTOR;

    public AsyncEventReactive() {
    }

    public AsyncEventReactive(MultithreadEventExecutorGroup multithreadEventExecutorGroup) {
        this.eventExecutorGroup = multithreadEventExecutorGroup;
    }

    @Override
    public void reactive(final Event event) {

        eventExecutorGroup.execute(() -> super.reactive0(event));
    }
}
