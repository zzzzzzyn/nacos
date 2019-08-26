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
package com.alibaba.nacos.core.remoting.grpc.interactive;

import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import io.grpc.stub.CallStreamObserver;

/**
 * @author pbting
 * 基于 Grpc 实现的两个节点间的通信模型。默认是异步的
 */
public class GrpcRequestStreamInteractive extends AbstractGrpcInteractive {

    public GrpcRequestStreamInteractive(InteractivePayload interactivePayload,
                                        CallStreamObserver responseStream) {
        super(interactivePayload, responseStream);
    }

    /**
     * @param responsePayload
     * @return
     */
    public boolean sendResponsePayload(InteractivePayload responsePayload) {
        if (responseStream.isReady()) {
            responseStream.onNext(responsePayload);
            return true;
        }

        return false;
    }
}
