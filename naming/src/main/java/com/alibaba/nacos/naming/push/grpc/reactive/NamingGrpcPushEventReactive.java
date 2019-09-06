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
package com.alibaba.nacos.naming.push.grpc.reactive;

import com.alibaba.nacos.core.remoting.event.reactive.EventLoopReactive;
import com.alibaba.nacos.naming.push.grpc.filter.IClientPushFilter;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * @author pbting
 * @date 2019-09-06 2:12 PM
 */
@Component
public class NamingGrpcPushEventReactive extends EventLoopReactive
    implements SmartInitializingSingleton, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(NamingGrpcPushEventReactive.class);

    private ApplicationContext applicationContext;

    public NamingGrpcPushEventReactive() {
    }

    public NamingGrpcPushEventReactive(MultithreadEventExecutorGroup multithreadEventExecutorGroup) {
        super(multithreadEventExecutorGroup);
    }

    @Override
    public void afterSingletonsInstantiated() {

        try {
            Collection<IClientPushFilter> clientPushFilters =
                this.applicationContext.getBeansOfType(IClientPushFilter.class).values();
            this.registerEventReactiveFilter(clientPushFilters);
        } catch (BeansException e) {
            logger.error("get client push filter cause an exception.", e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
