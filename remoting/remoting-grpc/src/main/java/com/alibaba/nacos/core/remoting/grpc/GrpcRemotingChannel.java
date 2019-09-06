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
package com.alibaba.nacos.core.remoting.grpc;

import com.alibaba.nacos.core.remoting.channel.AbstractRemotingChannelModel;
import com.alibaba.nacos.core.remoting.grpc.observer.AbstractGrpcRequestStreamObserver;
import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import com.alibaba.nacos.core.remoting.proto.InteractiveServiceGrpc;
import com.alibaba.nacos.core.remoting.stream.IRemotingRequestStreamObserver;
import io.grpc.ManagedChannel;
import io.grpc.stub.CallStreamObserver;

/**
 * @author pbting
 */
public class GrpcRemotingChannel extends AbstractRemotingChannelModel {

    private volatile ManagedChannel managedChannel;

    private String identify;

    public GrpcRemotingChannel(ManagedChannel managedChannel, String addressPort) {
        super(addressPort);
        this.managedChannel = managedChannel;
    }

    public InteractiveServiceGrpc.InteractiveServiceStub getStub() {
        return InteractiveServiceGrpc.newStub(this.getRawChannel());
    }

    public InteractiveServiceGrpc.InteractiveServiceBlockingStub getBlockingStub() {
        return InteractiveServiceGrpc.newBlockingStub(this.getRawChannel());
    }

    @Override
    public ManagedChannel getRawChannel() {
        return managedChannel;
    }

    public void setIdentify(String identify) {
        this.identify = identify;
    }

    @Override
    public String identify() {
        return identify == null ? super.identify() : identify;
    }

    @Override
    public boolean isNetUnavailable(Exception e) {
        return GrpcNetExceptionUtils.isNetUnavailable(e, this.managedChannel);
    }

    @Override
    public void close() {
        managedChannel.resetConnectBackoff();
        managedChannel.shutdown();
    }

    @Override
    public void fireAndForget(InteractivePayload inBound) {
        requestResponse(inBound);
    }

    @Override
    public InteractivePayload requestResponse(InteractivePayload requestPayload) {
        InteractiveServiceGrpc.InteractiveServiceBlockingStub stub = getBlockingStub();
        InteractivePayload response = stub.requestResponse(requestPayload);
        return response;
    }

    /**
     * 返回的是一个: AbstractGrpcRequestStreamObserver,业务层必须调用一次 (IRemotingRequestStreamObserver)}
     * (IRemotingRequestStreamObserver)} 来处理接收服务端数据时的处理
     *
     * @param requestPayload
     * @return
     */
    @Override
    public void requestStream(InteractivePayload requestPayload, IRemotingRequestStreamObserver requestStreamObserver) {
        InteractiveServiceGrpc.InteractiveServiceStub stub = getStub();
        AbstractGrpcRequestStreamObserver streamObserver = new AbstractGrpcRequestStreamObserver() {
            @Override
            public void onNext(InteractivePayload value) {
                callStreamObserver.onNext(value);
            }
        };
        streamObserver.registryRequestStreamObserver(requestStreamObserver);
        stub.requestStream(requestPayload, streamObserver);
    }

    @Override
    public <OUT_BOUND, IN_BOUND> OUT_BOUND requestChannel(IN_BOUND inBound) {
        CallStreamObserver<InteractivePayload> inBoundStreamObserver = (CallStreamObserver<InteractivePayload>) inBound;
        return (OUT_BOUND) getStub().requestChannel(inBoundStreamObserver);
    }
}
