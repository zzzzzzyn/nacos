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

import com.alibaba.nacos.core.remoting.Identify;

/**
 * a abstract remoting channel implement
 * .
 *
 * @author pbting
 */
public abstract class AbstractRemotingChannel
    implements Identify, IRequestResponseChannel, IRequestStreamChannel,
    IBiRequestChannel, IFireAndForgetChannel {

    /**
     * The {@AbstractRemotingChannel#addressPort} that will be used to connect to the remote node
     */
    private String addressPort;

    public AbstractRemotingChannel(String addressPort) {
        this.addressPort = addressPort;
    }


    /**
     * The remote node to which the current channel is connected
     *
     * @return
     */
    public String getAddressPort() {
        return addressPort;
    }

    /**
     * The current unique identifier of this channel
     *
     * @return
     */
    @Override
    public abstract String identify();

    /**
     * Close a remote connection
     */
    public abstract void close();

    /**
     * Whether the network is unavailable
     *
     * @param e
     * @return
     */
    public abstract boolean isNetUnavailable(Exception e);

    /**
     * retrieve the raw channel
     *
     * @param <T>
     * @return
     */
    public abstract <T> T getRawChannel();
}
