/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
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

import com.alibaba.nacos.core.remoting.event.IEventPipelineReactive;
import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import com.alibaba.nacos.core.remoting.event.listener.StartupServerEventListener;
import com.alibaba.nacos.core.remoting.grpc.listener.GrpcStartupServerEventListener;
import com.alibaba.nacos.core.remoting.manager.IServerRemotingManager;
import io.grpc.Server;

/**
 * @author pbting
 * grpc server side remoting manager
 */
public class GrpcServerRemotingManager extends AbstractGrpcRemotingManager implements IServerRemotingManager {

    private Server server;

    @Override
    public <T> void setServer(T server) {
        this.server = (Server) server;
    }

    public Server getServer() {
        return server;
    }

    @Override
    public StartupServerEventListener initStartupServerEventListener() {
        return new GrpcStartupServerEventListener();
    }

    @Override
    public String builderIdentify(String key) {
        return "[ Server ] " + key;
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
            int[] interestEventTypes = iPipelineEventListener.interestEventTypes();
            if (interestEventTypes == null || interestEventTypes.length == 0) {
                continue;
            }

            Class<? extends IEventPipelineReactive> eventPipelineReactive =
                iPipelineEventListener.eventListenerPartition();
            IEventPipelineReactive eventReactive = getAbstractEventPipelineReactive(eventPipelineReactive);
            if (eventReactive == null) {
                try {
                    eventReactive = eventPipelineReactive.newInstance();
                    initEventPipelineReactive(eventReactive);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            // add pipeline event listener
            for (int eventType : interestEventTypes) {
                eventReactive.addLast(iPipelineEventListener, eventType);
            }
        }
    }
}
