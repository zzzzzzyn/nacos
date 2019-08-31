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
import com.alibaba.nacos.core.remoting.grpc.manager.GrpcServerRemotingManager;
import com.alibaba.nacos.core.remoting.manager.IServerRemotingManager;
import com.alibaba.nacos.naming.core.Service;
import com.alibaba.nacos.naming.push.AbstractEmitter;
import org.springframework.context.ApplicationContext;

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

    public GrpcEmitterService(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    public <T> T getEmitSource() {
        return null;
    }

    @Override
    public void initEmitter() {

        // 1. startup the grpc server
        IServerRemotingManager serverRemotingManager = new GrpcServerRemotingManager();
        IAttachListenerHook attachListenerHook = (IAttachListenerHook) serverRemotingManager;
        attachListenerHook.attachListeners(serverRemotingManager.initStartupServerEventListener());

    }

    @Override
    public void emitter(Service service) {

    }
}
