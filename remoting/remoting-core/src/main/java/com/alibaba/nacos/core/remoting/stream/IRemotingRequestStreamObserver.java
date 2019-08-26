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
package com.alibaba.nacos.core.remoting.stream;

import com.alibaba.nacos.core.remoting.proto.InteractivePayload;

/**
 * @author pbting
 * <p>
 * When a request of type request/stream is initiated,
 * the implementation of this interface can be used to aware the response of remote data.
 * <p>
 * Referenced in Grpc {@link io.grpc.stub.StreamObserver}
 */
public interface IRemotingRequestStreamObserver {

    default boolean isReady() {
        return true;
    }

    default void setOnReadyHandler(Runnable onReadyHandler) {
        // nothing to do
    }

    default void disableAutoInboundFlowControl() {
        // nothing to do
    }

    default void request(int count) {
        // nothing to do
    }

    default void setMessageCompression(boolean enable) {
        // nothing to do
    }

    /**
     * must be set
     *
     * @param interactivePayload
     */
    void onNext(InteractivePayload interactivePayload);

    default void onError(Throwable t) {
        // nothing to do
    }

    default void onCompleted() {
        // nothing to do
    }
}
