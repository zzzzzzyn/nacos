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

import com.alibaba.nacos.core.remoting.event.IAttachListenerHook;
import com.alibaba.nacos.core.remoting.event.StartupEvent;
import com.alibaba.nacos.core.remoting.event.reactive.IEventPipelineReactive;
import com.alibaba.nacos.core.remoting.grpc.manager.GrpcServerRemotingManager;
import com.alibaba.nacos.core.remoting.grpc.reactive.GrpcServerEventReactive;
import com.alibaba.nacos.core.remoting.manager.IServerRemotingManager;
import com.alibaba.nacos.core.utils.InetUtils;
import com.alibaba.nacos.naming.controllers.InstanceController;
import com.alibaba.nacos.naming.core.Service;
import com.alibaba.nacos.naming.push.AbstractEmitter;
import com.alibaba.nacos.naming.push.AbstractPushClient;
import com.alibaba.nacos.naming.push.DataSource;
import com.alibaba.nacos.naming.push.PushService;
import org.springframework.context.ApplicationContext;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * support emitter with gRPC
 *
 * @author pbting
 * @date 2019-08-30 10:11 PM
 */
public class GrpcEmitterService extends AbstractEmitter {

    /**
     * the grpc server port
     */
    private static final int GRPC_SERVER_PORT = 28848;
    private PushService pushService;

    public GrpcEmitterService(ApplicationContext applicationContext,
                              PushService pushServic) {
        super(applicationContext);
        this.pushService = pushServic;
    }

    /**
     * @param sourceKey
     * @return
     */
    @Override
    public Map<String, AbstractPushClient> getEmitSource(String sourceKey) {
        return pushService.getPushClients(sourceKey);
    }

    @Override
    public void initEmitter() {

        // 1. initialize a gRPC server remoting manager
        IServerRemotingManager serverRemotingManager = new GrpcServerRemotingManager();

        // 2. attach some event pipeline listener
        IAttachListenerHook attachListenerHook = (IAttachListenerHook) serverRemotingManager;
        attachListenerHook.attachListeners(serverRemotingManager.initStartupServerEventListener());
        attachListenerHook.attachListeners(new StartupEventMappingListener());

        DataSource dataSource = this.applicationContext.getBean(InstanceController.class).getPushDataSource();
        attachListenerHook.attachListeners(new GrpcClientSubscribeEventListener(this, pushService, dataSource));

        // 3. get the type of gRPC server event reactive partition。
        IEventPipelineReactive serverEventPipelineReactive =
            serverRemotingManager.getAbstractEventPipelineReactive(GrpcServerEventReactive.class);

        // 4. reactive a startup server event to the grpc server
        serverEventPipelineReactive.reactive(new StartupEvent(serverRemotingManager, new InetSocketAddress(InetUtils.getSelfIp(), GRPC_SERVER_PORT)));
    }

    @Override
    public void emitter(Service service) {

    }
}
