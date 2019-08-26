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

import com.alibaba.nacos.core.remoting.grpc.interactive.GrpcRequestStreamInteractive;
import com.alibaba.nacos.core.remoting.interactive.IInteractive;
import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import io.grpc.stub.CallStreamObserver;

/**
 * @author pbting
 */
public abstract class AbstractCallStreamObserver
    extends CallStreamObserver<InteractivePayload>
    implements IInteractive, IIdentifyStreamObserver {

    private String identifyStream;
    /**
     * if you have some payload to send other nodes,it very useful.
     */
    protected CallStreamObserver<InteractivePayload> interactiveStream;

    public AbstractCallStreamObserver(
        CallStreamObserver<InteractivePayload> interactiveStream) {
        this.interactiveStream = interactiveStream;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setOnReadyHandler(Runnable onReadyHandler) {

    }

    @Override
    public void disableAutoInboundFlowControl() {

    }

    @Override
    public void request(int count) {

    }

    @Override
    public void setMessageCompression(boolean enable) {

    }

    public void onCompleted() {

    }

    public void onNext(InteractivePayload value) {
        request(new GrpcRequestStreamInteractive(value, interactiveStream));
    }

    public abstract void request(GrpcRequestStreamInteractive wareSwiftInteractive);

    public boolean sendResponsePayload(InteractivePayload responsePayload) {
        if (interactiveStream.isReady()) {
            interactiveStream.onNext(responsePayload);
            return true;
        }

        return false;
    }

    /**
     * the client is disconnect will call this method
     *
     * @param t
     */
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @Override
    public String getIdentify() {
        return identifyStream;
    }

    @Override
    public void setIdentify(String identify) {
        this.identifyStream = identify;
    }
}
