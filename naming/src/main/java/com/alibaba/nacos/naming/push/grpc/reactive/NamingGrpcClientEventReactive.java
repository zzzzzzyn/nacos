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

import com.alibaba.nacos.core.remoting.event.reactive.IEventReactive;
import com.alibaba.nacos.core.remoting.grpc.reactive.GrpcClientEventReactive;
import com.alibaba.nacos.naming.push.grpc.filter.IClientProcessFilter;
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
 * @date 2019-09-05 11:19 AM
 */
@Component
public class NamingGrpcClientEventReactive extends GrpcClientEventReactive
    implements SmartInitializingSingleton, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(NamingGrpcPushEventReactive.class);

    private ApplicationContext applicationContext;

    @Override
    public void afterSingletonsInstantiated() {

        // Assembly event response filter
        try {
            Collection<IClientProcessFilter> filters =
                this.applicationContext.getBeansOfType(IClientProcessFilter.class).values();
            filters.removeIf(filter -> filter instanceof IEventReactive);
            this.registerEventReactiveFilter(filters);
        } catch (BeansException e) {
            logger.error("initialize client process filter cause an exception.", e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
