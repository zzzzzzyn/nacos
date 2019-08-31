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

import com.alibaba.nacos.core.remoting.grpc.GrpcRemotingChannel;
import com.alibaba.nacos.core.remoting.grpc.manager.GrpcClientRemotingManager;
import com.alibaba.nacos.core.remoting.grpc.observer.AbstractGrpcRequestStreamObserver;
import com.alibaba.nacos.core.remoting.manager.IClientRemotingManager;
import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import com.alibaba.nacos.core.remoting.stream.IRemotingRequestStreamObserver;
import com.google.protobuf.ByteString;
import io.grpc.stub.CallStreamObserver;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author pbting
 * @date 2019-08-30 11:23 PM
 */
public class BiRequestStreamTests extends StartupGrpcServerTests {

    public void startupServer01() throws Exception {

        super.startupServer();
    }

    @Test
    public void clienBitRequestStream() throws Exception {
        // support client request stream
        IClientRemotingManager clientRemotingManager = new GrpcClientRemotingManager();
        final GrpcRemotingChannel remotingChannel = (GrpcRemotingChannel)
            clientRemotingManager.getRemotingChannelFactory().newRemotingChannel("0.0.0.0:28848", "default");
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setEventType(DEBUG_STREAM_EVENT_TYPE);
        builder.setPayload(ByteString.copyFrom("dataId:production.yaml,group:default".getBytes()));

        AbstractGrpcRequestStreamObserver streamObserver =
            new AbstractGrpcRequestStreamObserver() {
                @Override
                public void onNext(InteractivePayload value) {
                    callStreamObserver.onNext(value);
                }
            };
        streamObserver.registryRequestStreamObserver(new IRemotingRequestStreamObserver() {
            @Override
            public void onNext(InteractivePayload interactivePayload) {
                System.err.println(interactivePayload.getPayload().toStringUtf8());
            }
        });

        CallStreamObserver requestStream = remotingChannel.requestChannel(streamObserver);

        while (true) {
            InteractivePayload.Builder Bi = InteractivePayload.newBuilder();
            Bi.setPayload(ByteString.copyFrom(String.format("data content is the current time [%s]", new Date().toString()).getBytes()));
            Bi.setEventType(DEBUG_EVENT_TYPE);
            requestStream.onNext(Bi.build());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }

}
