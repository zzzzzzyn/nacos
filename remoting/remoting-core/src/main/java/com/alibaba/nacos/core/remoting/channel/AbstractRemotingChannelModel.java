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
package com.alibaba.nacos.core.remoting.channel;

import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import com.alibaba.nacos.core.remoting.stream.IRemotingRequestStreamObserver;

/**
 * Implementation of an empty Remoting Channel Model
 *
 * @author pbting
 */
public abstract class AbstractRemotingChannelModel extends AbstractRemotingChannel {

    @Override
    public String identify() {
        return this.getClass().getCanonicalName();
    }

    /**
     * @param addressPort
     */
    public AbstractRemotingChannelModel(String addressPort) {
        super(addressPort);
    }

    @Override
    public InteractivePayload requestResponse(InteractivePayload requestPayload) {
        throw new UnsupportedOperationException("request/response does not implement");
    }

    @Override
    public <OUT_BOUND, IN_BOUND> OUT_BOUND requestChannel(IN_BOUND inBound) {
        throw new UnsupportedOperationException("request/channel does not implement");
    }

    @Override
    public void requestStream(InteractivePayload requestPayload, IRemotingRequestStreamObserver requestStreamObserver) {
        throw new UnsupportedOperationException("request/stream does not implement");
    }

    @Override
    public void fireAndForget(InteractivePayload inBound) {
        throw new UnsupportedOperationException("fireAndForget does not implement");
    }

    @Override
    public boolean isNetUnavailable(Exception e) {
        return false;
    }
}
