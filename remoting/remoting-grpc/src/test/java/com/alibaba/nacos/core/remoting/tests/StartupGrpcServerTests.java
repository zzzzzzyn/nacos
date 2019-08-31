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
package com.alibaba.nacos.core.remoting.tests;

import com.alibaba.nacos.core.remoting.channel.AbstractRemotingChannel;
import com.alibaba.nacos.core.remoting.event.*;
import com.alibaba.nacos.core.remoting.event.reactive.IEventPipelineReactive;
import com.alibaba.nacos.core.remoting.grpc.GrpcRemotingChannel;
import com.alibaba.nacos.core.remoting.grpc.entrance.GrpcClientEntranceServiceImpl;
import com.alibaba.nacos.core.remoting.grpc.event.GrpcBiStreamEvent;
import com.alibaba.nacos.core.remoting.grpc.manager.GrpcClientRemotingManager;
import com.alibaba.nacos.core.remoting.grpc.manager.GrpcServerRemotingManager;
import com.alibaba.nacos.core.remoting.grpc.reactive.GrpcClientEventReactive;
import com.alibaba.nacos.core.remoting.grpc.reactive.GrpcServerEventReactive;
import com.alibaba.nacos.core.remoting.interactive.IInteractive;
import com.alibaba.nacos.core.remoting.manager.IClientRemotingManager;
import com.alibaba.nacos.core.remoting.manager.IServerRemotingManager;
import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import com.alibaba.nacos.core.remoting.stream.IRemotingRequestStreamObserver;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author pbting
 * @date 2019-08-22 11:21 PM
 */
public class StartupGrpcServerTests {

    public static final int DEBUG_EVENT_TYPE = 1;
    public static final int DEBUG_STREAM_EVENT_TYPE = 2;

    @Test
    public void rawStartupServer() throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("0.0.0.0", 28848);
        NettyServerBuilder nettyServerBuilder = NettyServerBuilder.forAddress(inetSocketAddress);
        Server server = nettyServerBuilder.addService(new GrpcClientEntranceServiceImpl(new GrpcClientEventReactive()))
            .flowControlWindow(NettyServerBuilder.DEFAULT_FLOW_CONTROL_WINDOW)
            .build().start();

        System.err.println(server.toString());

        System.in.read();
    }

    public IServerRemotingManager startupServer() throws Exception {

        IServerRemotingManager serverRemotingManager = new GrpcServerRemotingManager();
        IAttachListenerHook attachListenerHook = (IAttachListenerHook) serverRemotingManager;
        attachListenerHook.attachListeners(serverRemotingManager.initStartupServerEventListener());

        // 1. register request/response event listener
        attachListenerHook.attachListeners(new IPipelineEventListener<ClientRequestResponseEvent>() {
            @Override
            public boolean onEvent(ClientRequestResponseEvent event, int listenerIndex) {

                IInteractive interactive = event.getValue();
                InteractivePayload requestPayload = interactive.getRequestPayload();
                System.err.println("----> receive request payload " + requestPayload.getPayload().toStringUtf8());

                InteractivePayload responsePayload = InteractivePayload.newBuilder(requestPayload)
                    .setPayload(ByteString.copyFrom(String.format("[Server Response] %s", requestPayload.getPayload().toStringUtf8()).getBytes())).buildPartial();
                interactive.sendResponsePayload(responsePayload);
                return true;
            }

            @Override
            public Class<? extends Event>[] interestEventTypes() {
                return new Class[]{ClientRequestResponseEvent.class};
            }

            @Override
            public Class<? extends IEventPipelineReactive> pipelineReactivePartition() {
                return GrpcClientEventReactive.class;
            }
        });

        // 2. register request/stream event listener
        attachListenerHook.attachListeners(new IPipelineEventListener<ClientRequestStreamEvent>() {
            @Override
            public boolean onEvent(ClientRequestStreamEvent event, int listenerIndex) {
                IInteractive interactive = event.getValue();
                System.err.println("receive client request/stream ,payload is " + interactive.getRequestPayload().getPayload().toStringUtf8());
                for (int i = 0; i < Integer.MAX_VALUE; i++) {
                    InteractivePayload.Builder builder = InteractivePayload.newBuilder();
                    builder.setPayload(ByteString.copyFrom(String.format("data content is the current time [%s]", new Date().toString()).getBytes()));
                    interactive.sendResponsePayload(builder.build());
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                    }
                }
                return true;
            }

            @Override
            public Class<? extends Event>[] interestEventTypes() {
                return new Class[]{ClientRequestStreamEvent.class};
            }

            @Override
            public Class<? extends IEventPipelineReactive> pipelineReactivePartition() {
                return GrpcClientEventReactive.class;
            }
        });

        // 3. attach bi-stream
        attachListenerHook.attachListeners(new IPipelineEventListener<GrpcBiStreamEvent>() {
            private final AtomicLong count = new AtomicLong();

            @Override
            public boolean onEvent(GrpcBiStreamEvent event, int listenerIndex) {
                IInteractive interactive = event.getValue();
                System.err.println("receive client request/channel ,payload is " + interactive.getRequestPayload().getPayload().toStringUtf8());
                if (count.incrementAndGet() > 1) {
                    InteractivePayload.Builder builder = InteractivePayload.newBuilder();
                    builder.setPayload(ByteString.copyFrom(String.format("[ Response Server ]data content is the current time [%s]", new Date().toString()).getBytes()));
                    interactive.sendResponsePayload(builder.build());
                }
                return true;
            }

            @Override
            public Class<? extends Event>[] interestEventTypes() {
                return new Class[]{GrpcBiStreamEvent.class};
            }

            @Override
            public Class<? extends IEventPipelineReactive> pipelineReactivePartition() {
                return GrpcClientEventReactive.class;
            }
        });

        Event event =
            new StartupEvent(serverRemotingManager, new InetSocketAddress("0.0.0.0", 28848));
        IEventPipelineReactive eventPipelineReactive = serverRemotingManager.getAbstractEventPipelineReactive(GrpcServerEventReactive.class);
        eventPipelineReactive.reactive(event);

        return serverRemotingManager;
    }

    @Test
    public void startup() throws Exception {
        startupServer();
        System.in.read();
    }

    @Test
    public void clientRequest() throws Exception {

        IClientRemotingManager clientRemotingManager = new GrpcClientRemotingManager();
        AbstractRemotingChannel remotingChannel =
            clientRemotingManager
                .getRemotingChannelFactory()
                .newRemotingChannel("0.0.0.0:28848", "default");

        GrpcRemotingChannel grpcRemotingChannel = (GrpcRemotingChannel) remotingChannel;

        for (int i = 1; i < 10000; i++) {
            InteractivePayload responsePayload;
            try {
                System.err.println("channel state :" + grpcRemotingChannel.getRawChannel().getState(true));
                responsePayload = remotingChannel
                    .requestResponse(InteractivePayload.newBuilder().setEventType(DEBUG_EVENT_TYPE)
                        .setPayload(ByteString.copyFrom(String.format(" Hello Word %s", i).getBytes())).build());
                System.err.println(responsePayload.getPayload().toStringUtf8());
            } catch (Exception e) {
                e.printStackTrace();
            }
            TimeUnit.SECONDS.sleep(1);
        }
        System.in.read();
    }

    @Test
    public void clientRequestStream() throws Exception {
        // support client request stream
        IClientRemotingManager clientRemotingManager = new GrpcClientRemotingManager();
        final GrpcRemotingChannel remotingChannel = (GrpcRemotingChannel)
            clientRemotingManager.getRemotingChannelFactory().newRemotingChannel("0.0.0.0:28848", "default");
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setEventType(DEBUG_STREAM_EVENT_TYPE);
        builder.setPayload(ByteString.copyFrom("dataId:production.yaml,group:default".getBytes()));
        remotingChannel.requestStream(builder.build(), new IRemotingRequestStreamObserver() {
            @Override
            public void onNext(InteractivePayload interactivePayload) {
                System.err.println("receive server push :" + interactivePayload.getPayload().toStringUtf8());
            }
        });
        Executors.newSingleThreadScheduledExecutor().execute(() -> {
            while (true) {
                System.out.println("isShutdown " + remotingChannel.getRawChannel().isShutdown() + "； is termniated= " + remotingChannel.getRawChannel().isTerminated());
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        System.in.read();
    }

    @Test
    public void ThreadLocalTest() throws Exception {
        ThreadLocal threadLocal = new ThreadLocal();
        threadLocal.set("abc");
        Thread.sleep(2);
        threadLocal = new ThreadLocal();
        System.err.println(threadLocal.get());
    }
}
