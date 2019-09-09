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
package com.alibaba.nacos.client.naming.core.grpc;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.core.remoting.channel.AbstractRemotingChannel;
import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.event.filter.IEventReactiveFilter;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;

import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;

/**
 * @author pbting
 * @date 2019-09-05 9:52 PM
 */
public class RemotingActiveCheckFilter implements IEventReactiveFilter {

    private GrpcServiceAwareStrategy grpcServiceChangedAwareStrategy;

    public RemotingActiveCheckFilter(GrpcServiceAwareStrategy grpcServiceChangedAwareStrategy) {

        this.grpcServiceChangedAwareStrategy = grpcServiceChangedAwareStrategy;
    }

    @Override
    public boolean aheadFilter(Event event) {
        AbstractRemotingChannel remotingChannel =
            event.getParameter(GrpcServiceAwareStrategy.EVENT_CONTEXT_CHANNEL);
        ManagedChannel managedChannel = remotingChannel.getRawChannel();
        ConnectivityState connectivityState = managedChannel.getState(true);
        if (connectivityState != ConnectivityState.TRANSIENT_FAILURE) {
            return true;
        }

        boolean isDoFilter = true;
        try {
            grpcServiceChangedAwareStrategy.initRemotingChannel();
            event.setParameter(GrpcServiceAwareStrategy.EVENT_CONTEXT_CHANNEL,
                grpcServiceChangedAwareStrategy.getRemotingChannel());
        } catch (NacosException e) {
            NAMING_LOGGER.error("init remoting channel cause an exception.", e);
            isDoFilter = false;
        }
        return isDoFilter;
    }

}
