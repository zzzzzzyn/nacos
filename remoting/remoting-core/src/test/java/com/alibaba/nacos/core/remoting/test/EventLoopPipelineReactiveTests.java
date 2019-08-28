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
package com.alibaba.nacos.core.remoting.test;

import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import com.alibaba.nacos.core.remoting.event.RecyclableEvent;
import com.alibaba.nacos.core.remoting.event.reactive.EventLoopPipelineReactive;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author pbting
 * @date 2019-08-28 3:06 PM
 */
public class EventLoopPipelineReactiveTests {


    @Test
    public void eventLoopTest() throws Exception {

        EventLoopPipelineReactive eventLoopPipelineReactive =
            new EventLoopPipelineReactive();

        eventLoopPipelineReactive.addListener(new IPipelineEventListener<RecyclableEvent>() {
            @Override
            public boolean onEvent(RecyclableEvent event, int listenerIndex) {
                AtomicLong loopCount = event.getParameter("count");
                if (loopCount == null) {
                    loopCount = new AtomicLong();
                    event.setParameter("count", loopCount);
                }
                if (loopCount.incrementAndGet() > 10) {
                    System.err.println("[exit]event loop count is " + loopCount.get());
                    event.cancel();
                } else {
                    System.err.println("event loop count is " + loopCount.get());
                }
                return true;
            }

            @Override
            public int[] interestEventTypes() {
                return new int[]{1};
            }
        });

        RecyclableEvent recyclableEvent = new RecyclableEvent(eventLoopPipelineReactive, "Event Loop", 1, 1);
        eventLoopPipelineReactive.reactive(recyclableEvent);

        System.in.read();
    }
}
