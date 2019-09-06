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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pbting
 * @date 2019-09-05 9:52 PM
 */
public class RemotingActiveCheckFilter implements IEventReactiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(RemotingActiveCheckFilter.class);
    private GrpcServiceChangedAwareStrategy grpcServiceChangedAwareStrategy;

    public RemotingActiveCheckFilter(GrpcServiceChangedAwareStrategy grpcServiceChangedAwareStrategy) {

        this.grpcServiceChangedAwareStrategy = grpcServiceChangedAwareStrategy;
    }

    @Override
    public boolean aheadFilter(Event event) {
        AbstractRemotingChannel remotingChannel =
            event.getParameter(GrpcServiceChangedAwareStrategy.EVENT_CONTEXT_CHANNEL);
        ManagedChannel managedChannel = remotingChannel.getRawChannel();
        ConnectivityState connectivityState = managedChannel.getState(true);
        if (connectivityState != ConnectivityState.TRANSIENT_FAILURE) {
            return true;
        }

        boolean isDoFilter = true;
        try {
            grpcServiceChangedAwareStrategy.initRemotingChannel();
            event.setParameter(GrpcServiceChangedAwareStrategy.EVENT_CONTEXT_CHANNEL,
                grpcServiceChangedAwareStrategy.getRemotingChannel());
        } catch (NacosException e) {
            logger.error("init remoting channel cause an exception.", e);
            isDoFilter = false;
        }
        return isDoFilter;
    }

}
