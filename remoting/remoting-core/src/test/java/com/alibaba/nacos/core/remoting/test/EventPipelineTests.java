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

import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import com.alibaba.nacos.core.remoting.event.reactive.DefaultEventReactive;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author pbting
 * @date 2019-09-06 5:12 PM
 */
public class EventPipelineTests {

    @Test
    public void eventPipeline() {

        DefaultEventReactive defaultEventReactive =
            new DefaultEventReactive();

        defaultEventReactive.addListener(new IPipelineEventListener() {
            @Override
            public boolean onEvent(Event event, int listenerIndex) {

                System.err.println("-->" + this.pipelineOrder());
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public String[] interestSinks() {
                return new String[]{"example"};
            }

            @Override
            public int pipelineOrder() {
                return 2;
            }
        });

        defaultEventReactive.addListener(new IPipelineEventListener() {
            @Override
            public boolean onEvent(Event event, int listenerIndex) {
                System.err.println("-->" + this.pipelineOrder());
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public String[] interestSinks() {
                return new String[]{"example"};
            }

            @Override
            public int pipelineOrder() {
                return 1;
            }
        });

        defaultEventReactive.reactive(new Event(defaultEventReactive, "Hello", "example"));
    }
}
