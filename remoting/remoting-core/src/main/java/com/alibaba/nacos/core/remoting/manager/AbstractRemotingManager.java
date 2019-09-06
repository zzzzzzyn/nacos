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
package com.alibaba.nacos.core.remoting.manager;

import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import com.alibaba.nacos.core.remoting.event.reactive.IEventReactiveHelm;
import com.alibaba.nacos.core.remoting.event.reactive.IEventReactive;

import java.util.HashMap;

/**
 * @author pbting
 * @date 2019-09-04 4:52 PM
 */
public abstract class AbstractRemotingManager implements IRemotingManager {

    protected HashMap<Class<? extends IEventReactive>, IEventReactive> eventReactivePartition = new HashMap<>();

    @Override
    public IEventReactive getAbstractEventReactive(Class<? extends IEventReactive> eventReactivePartition) {
        return this.eventReactivePartition.get(eventReactivePartition);
    }

    @Override
    public void initEventReactive(IEventReactive eventReactive) {
        eventReactivePartition.put(eventReactive.getClass(), eventReactive);
    }

    @Override
    public void notifyListeners(final Event event) {
        eventReactivePartition.values().forEach(eventReactive -> eventReactive.reactive(event));
    }

    /**
     * @param pipelineEventListeners
     */
    @Override
    public void attachListeners(IPipelineEventListener... pipelineEventListeners) {

        if (pipelineEventListeners == null || pipelineEventListeners.length == 0) {
            return;
        }

        for (IPipelineEventListener iPipelineEventListener : pipelineEventListeners) {
            String[] interestEventSinks = iPipelineEventListener.interestSinks();
            if (interestEventSinks == null || interestEventSinks.length == 0) {
                continue;
            }

            Class<? extends IEventReactive> eventPipelineReactive = iPipelineEventListener.pipelineReactivePartition();
            IEventReactive eventReactive = getAbstractEventReactive(eventPipelineReactive);
            if (eventReactive == null) {
                try {
                    eventReactive = eventPipelineReactive.newInstance();
                    initEventReactive(eventReactive);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            // add pipeline event listenersSinkRegistry
            if (eventReactive instanceof IEventReactiveHelm) {
                IEventReactiveHelm eventListenerHelm = (IEventReactiveHelm) eventReactive;
                eventListenerHelm.addLast(iPipelineEventListener);
            }
        }
    }
}
