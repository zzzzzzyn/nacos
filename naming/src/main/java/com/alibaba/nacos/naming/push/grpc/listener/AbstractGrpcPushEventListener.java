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
package com.alibaba.nacos.naming.push.grpc.listener;

import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import com.alibaba.nacos.core.remoting.event.reactive.IEventReactive;
import com.alibaba.nacos.naming.push.grpc.GrpcPushAdaptor;
import com.alibaba.nacos.naming.push.grpc.reactive.NamingGrpcPushEventReactive;

/**
 * @author pbting
 * @date 2019-09-06 3:35 PM
 */
public abstract class AbstractGrpcPushEventListener implements IPipelineEventListener<GrpcPushAdaptor.PushRecyclableEvent> {

    public static final String PUSH_FAILURE = "pushFailure";
    public static final String PUSH_TIMES = "pushRetryTimes";
    public static final String PUSH_MAX_RETRY_TIMES = "pushMaxRetryTimes";
    public static final String PUSH_CACHE_MILLS = "pushCacheMills";

    @Override
    public String[] interestSinks() {
        return new String[]{GrpcPushAdaptor.PushRecyclableEvent.class.getName()};
    }

    @Override
    public Class<? extends IEventReactive> listenerReactivePartition() {
        return NamingGrpcPushEventReactive.class;
    }
}
