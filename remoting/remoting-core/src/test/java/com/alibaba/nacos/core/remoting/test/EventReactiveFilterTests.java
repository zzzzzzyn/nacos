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
import com.alibaba.nacos.core.remoting.event.filter.IEventReactiveFilter;
import com.alibaba.nacos.core.remoting.event.reactive.DefaultEventReactive;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author pbting
 * @date 2019-09-06 10:54 PM
 */
public class EventReactiveFilterTests {

    @Test
    public void aheadFilterTrue() {

        DefaultEventReactive eventReactive = new DefaultEventReactive();
        eventReactive.registerEventReactiveFilter(Arrays.asList(new IEventReactiveFilter() {
            @Override
            public boolean aheadFilter(Event event) {
                System.err.println(event.getSink() + " => " + this.order());
                return true;
            }

            @Override
            public int order() {
                return 1;
            }
        }, new IEventReactiveFilter() {
            @Override
            public boolean aheadFilter(Event event) {
                System.err.println(event.getSink() + " => " + this.order());
                return true;
            }

            @Override
            public int order() {
                return 2;
            }
        }));

        eventReactive.reactive(new Event(eventReactive, "", "example"));
    }

    @Test
    public void aheadSortFilterTrue() {

        DefaultEventReactive eventReactive = new DefaultEventReactive();
        eventReactive.registerEventReactiveFilter(Arrays.asList(new IEventReactiveFilter() {
            @Override
            public boolean aheadFilter(Event event) {
                System.err.println(event.getSink() + " => " + this.order());
                return true;
            }

            @Override
            public int order() {
                return 10;
            }
        }, new IEventReactiveFilter() {
            @Override
            public boolean aheadFilter(Event event) {
                System.err.println(event.getSink() + " => " + this.order());
                return true;
            }

            @Override
            public int order() {
                return 2;
            }
        }));

        eventReactive.reactive(new Event(eventReactive, "", "example"));
    }

    @Test
    public void aheadFilterFalse() {

        DefaultEventReactive eventReactive = new DefaultEventReactive();
        eventReactive.registerEventReactiveFilter(Arrays.asList(new IEventReactiveFilter() {
            @Override
            public boolean aheadFilter(Event event) {
                System.err.println(event.getSink() + " => " + this.order());
                return false;
            }

            @Override
            public int order() {
                return 1;
            }
        }, new IEventReactiveFilter() {
            @Override
            public boolean aheadFilter(Event event) {
                System.err.println(event.getSink() + " => " + this.order());
                return true;
            }

            @Override
            public int order() {
                return 2;
            }
        }));

        eventReactive.reactive(new Event(eventReactive, "", "example"));
    }


    @Test
    public void backFilterTrue() {

        DefaultEventReactive eventReactive = new DefaultEventReactive();
        eventReactive.registerEventReactiveFilter(Arrays.asList(new IEventReactiveFilter() {
            @Override
            public boolean backFilter(Event event) {
                System.err.println(event.getSink() + " => back => " + this.order());
                return true;
            }

            @Override
            public int order() {
                return 10;
            }
        }, new IEventReactiveFilter() {
            @Override
            public boolean backFilter(Event event) {
                System.err.println(event.getSink() + " => back =>" + this.order());
                return true;
            }

            @Override
            public int order() {
                return 12;
            }
        }));

        eventReactive.reactive(new Event(eventReactive, "", "example"));
    }

    @Test
    public void backSortFilterTrue() {

        DefaultEventReactive eventReactive = new DefaultEventReactive();
        eventReactive.registerEventReactiveFilter(Arrays.asList(new IEventReactiveFilter() {
            @Override
            public boolean backFilter(Event event) {
                System.err.println(event.getSink() + " => back =>" + this.order());
                return true;
            }

            @Override
            public int order() {
                return 10;
            }
        }, new IEventReactiveFilter() {
            @Override
            public boolean backFilter(Event event) {
                System.err.println(event.getSink() + " => back =>" + this.order());
                return false;
            }

            @Override
            public int order() {
                return 2;
            }
        }));

        eventReactive.reactive(new Event(eventReactive, "", "example"));
    }

    @Test
    public void backFilterFalse() {

        DefaultEventReactive eventReactive = new DefaultEventReactive();
        eventReactive.registerEventReactiveFilter(Arrays.asList(new IEventReactiveFilter() {
            @Override
            public boolean backFilter(Event event) {
                System.err.println(event.getSink() + " => back =>" + this.order());
                return false;
            }

            @Override
            public int order() {
                return 1;
            }
        }, new IEventReactiveFilter() {
            @Override
            public boolean backFilter(Event event) {
                System.err.println(event.getSink() + " => back =>" + this.order());
                return false;
            }

            @Override
            public int order() {
                return 2;
            }
        }));

        eventReactive.reactive(new Event(eventReactive, "", "example"));
    }
}
