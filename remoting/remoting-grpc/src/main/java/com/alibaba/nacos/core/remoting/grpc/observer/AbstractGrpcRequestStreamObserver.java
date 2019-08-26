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
package com.alibaba.nacos.core.remoting.grpc.observer;

import com.alibaba.nacos.core.remoting.stream.IRemotingRequestStreamObserver;
import com.alibaba.nacos.core.remoting.stream.IRequestStreamObserverRegistrar;
import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import io.grpc.stub.CallStreamObserver;

/**
 * @author pbting
 */
public abstract class AbstractGrpcRequestStreamObserver
    extends CallStreamObserver<InteractivePayload>
    implements IRequestStreamObserverRegistrar {

    protected IRemotingRequestStreamObserver callStreamObserver;

    @Override
    public void registryRequestStreamObserver(IRemotingRequestStreamObserver value) {
        this.callStreamObserver = value;
    }

    @Override
    public boolean isReady() {
        return callStreamObserver.isReady();
    }

    @Override
    public void setOnReadyHandler(Runnable onReadyHandler) {
        callStreamObserver.setOnReadyHandler(onReadyHandler);
    }

    @Override
    public void disableAutoInboundFlowControl() {
        callStreamObserver.disableAutoInboundFlowControl();
    }

    @Override
    public void request(int count) {
        callStreamObserver.request(count);
    }

    @Override
    public void setMessageCompression(boolean enable) {
        callStreamObserver.setMessageCompression(enable);
    }

    @Override
    public abstract void onNext(InteractivePayload value);

    @Override
    public void onError(Throwable t) {
        this.callStreamObserver.onError(t);
    }

    @Override
    public void onCompleted() {
        this.callStreamObserver.onCompleted();
    }
}
