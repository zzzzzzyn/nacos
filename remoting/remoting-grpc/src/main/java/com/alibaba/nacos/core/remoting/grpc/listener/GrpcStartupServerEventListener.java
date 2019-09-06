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
package com.alibaba.nacos.core.remoting.grpc.listener;

import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.event.listener.StartupServerEventListener;
import com.alibaba.nacos.core.remoting.event.reactive.IEventReactiveHelm;
import com.alibaba.nacos.core.remoting.grpc.entrance.GrpcClientEntranceServiceImpl;
import com.alibaba.nacos.core.remoting.grpc.reactive.GrpcClientEventReactive;
import com.alibaba.nacos.core.remoting.grpc.reactive.GrpcServerEventReactive;
import com.alibaba.nacos.core.remoting.manager.IServerRemotingManager;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author pbting
 * 使用 Grpc 来启动 Server
 */
public class GrpcStartupServerEventListener extends StartupServerEventListener {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(GrpcStartupServerEventListener.class);

    @Override
    public boolean onEvent(Event event, int listenerIndex) {
        IServerRemotingManager source = (IServerRemotingManager) event.getSource();
        Class reactiveClass = event.getParameter(CLIENT_EVENT_REACTIVE);
        InetSocketAddress inetSocketAddress = event.getValue();
        NettyServerBuilder nettyServerBuilder = NettyServerBuilder.forAddress(inetSocketAddress);
        Server server;
        try {
            GrpcClientEventReactive clientEventPipelineReactive =
                (GrpcClientEventReactive) source.getAbstractEventReactive(reactiveClass);
            server = nettyServerBuilder.addService(new GrpcClientEntranceServiceImpl(clientEventPipelineReactive))
                .flowControlWindow(NettyServerBuilder.DEFAULT_FLOW_CONTROL_WINDOW)
                .build().start();
            source.setServer(server);
            log.info("Grpc Server startup successfully with " + inetSocketAddress.toString());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Class<? extends IEventReactiveHelm> listenerReactivePartition() {
        return GrpcServerEventReactive.class;
    }
}
