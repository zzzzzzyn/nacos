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

}
