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
package com.alibaba.nacos.core.remoting.interactive;


import com.alibaba.nacos.core.remoting.proto.InteractivePayload;

/**
 * a high-level abstraction for network communication between two nodes。
 * <p>
 * This abstraction describes the two top-level api interfaces.
 * {@IInteractive#getRequestPayload} and {@IInteractive#sendResponsePayload}.
 *
 * @author pbting
 */
public interface IInteractive {

    /**
     * When the server receives the client's request,
     * it can use this method to get the specific message sent by the client.
     *
     * @return the specific message sent by the client
     */
    InteractivePayload getRequestPayload();

    /**
     * When the server needs to respond to the client after processing the client request,
     * you can use this method to respond to the client.
     *
     * @param responsePayload
     * @return If true, the transmission is successful or the transmission fails.
     */
    boolean sendResponsePayload(InteractivePayload responsePayload);
}
