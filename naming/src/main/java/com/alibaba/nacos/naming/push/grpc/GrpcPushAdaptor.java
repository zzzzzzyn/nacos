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
package com.alibaba.nacos.naming.push.grpc;

import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import com.alibaba.nacos.core.remoting.event.RecyclableEvent;
import com.alibaba.nacos.core.remoting.event.listener.StartupServerEventListener;
import com.alibaba.nacos.core.remoting.event.reactive.IEventReactive;
import com.alibaba.nacos.core.remoting.grpc.manager.GrpcServerRemotingManager;
import com.alibaba.nacos.core.remoting.manager.AbstractRemotingManager;
import com.alibaba.nacos.naming.core.Service;
import com.alibaba.nacos.naming.misc.UtilsAndCommons;
import com.alibaba.nacos.naming.push.AbstractPushAdaptor;
import com.alibaba.nacos.naming.push.AbstractPushClient;
import com.alibaba.nacos.naming.push.IPushClientFactory;
import com.alibaba.nacos.naming.push.grpc.factory.GrpcPushClientFactory;
import com.alibaba.nacos.naming.push.grpc.listener.AbstractGrpcPushEventListener;
import com.alibaba.nacos.naming.push.grpc.reactive.NamingGrpcClientEventReactive;
import com.alibaba.nacos.naming.push.grpc.reactive.NamingGrpcPushEventReactive;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * support push with gRPC
 *
 * @author pbting
 * @date 2019-08-30 10:11 PM
 */
@Component
public class GrpcPushAdaptor extends AbstractPushAdaptor {

    /**
     * the grpc server port
     */
    private static final int GRPC_SERVER_PORT = 28848;

    private final IPushClientFactory grpcPushClientFactory = new GrpcPushClientFactory();

    private final AbstractRemotingManager serverRemotingManager = new GrpcServerRemotingManager();

    public GrpcPushAdaptor(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    /**
     * @param sourceKey
     * @return
     */
    @Override
    public Map<String, AbstractPushClient> getPushSource(String sourceKey) {
        return pushService.getPushClients(sourceKey);
    }

    @Override
    public void initAdaptor() {

        // 1. Assembly event reactive
        try {
            Collection<IEventReactive> eventReactiveList = this.applicationContext.getBeansOfType(IEventReactive.class).values();
            eventReactiveList.forEach(eventReactive -> serverRemotingManager.initEventReactive(eventReactive));
        } catch (BeansException e) {
        }

        // 2. attach some event pipeline listenersSinkRegistry
        serverRemotingManager.attachListeners(((GrpcServerRemotingManager) serverRemotingManager).initStartupServerEventListener());
        // 3. Assembly event listener
        try {
            Collection<IPipelineEventListener> eventListeners = this.applicationContext.getBeansOfType(IPipelineEventListener.class).values();
            eventListeners.forEach(eventListener -> serverRemotingManager.attachListeners(eventListener));
        } catch (BeansException e) {
            e.printStackTrace();
        }
        // 4. notify some event listeners
        Event event = new Event(serverRemotingManager, new InetSocketAddress("0.0.0.0", GRPC_SERVER_PORT), StartupServerEventListener.SINK);
        event.setParameter(StartupServerEventListener.CLIENT_EVENT_REACTIVE, NamingGrpcClientEventReactive.class);
        serverRemotingManager.notifyListeners(event);
    }

    @Override
    public IPushClientFactory getPushClientFactory() {
        return grpcPushClientFactory;
    }

    @Override
    public void push(Service service) {
        final String namespaceId = service.getNamespaceId();
        final String serviceName = service.getName();
        // merge some change events to reduce the push frequency:
        String pushKey = UtilsAndCommons.assembleFullServiceName(namespaceId, serviceName);
        if (isContainsFutureMap(pushKey)) {
            return;
        }

        final Map<String, AbstractPushClient> clients = pushService.getPushClients(pushKey);
        if (MapUtils.isEmpty(clients)) {
            return;
        }
        // filter gRPC push client
        final Map<String, AbstractPushClient> targetClients = new HashMap<>(8);
        clients.forEach((key, value) -> {
            if (value instanceof GrpcPushClient) {
                targetClients.put(key, value);
            }
        });

        NamingGrpcPushEventReactive namingGrpcPushEventReactive =
            (NamingGrpcPushEventReactive) serverRemotingManager.getAbstractEventReactive(NamingGrpcPushEventReactive.class);

        PushRecyclableEvent event = new PushRecyclableEvent(this, targetClients, 1);
        event.setParameter(AbstractGrpcPushEventListener.PUSH_MAX_RETRY_TIMES, switchDomain.getMaxPushRetryTimes());
        event.setParameter(AbstractGrpcPushEventListener.PUSH_CACHE_MILLS, switchDomain.getDefaultCacheMillis());
        namingGrpcPushEventReactive.reactive(event);
    }

    public static class PushRecyclableEvent extends RecyclableEvent {

        public PushRecyclableEvent(Object source, Object value, int recycleInterval) {
            super(source, value, EMPTY_SINK, recycleInterval);
        }
    }
}
