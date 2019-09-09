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
import com.alibaba.nacos.naming.push.PushService;
import com.alibaba.nacos.naming.push.grpc.GrpcPushAdaptor;
import com.alibaba.nacos.naming.push.grpc.reactive.NamingGrpcClientEventReactive;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author pbting
 * @date 2019-08-30 10:43 PM
 */
public abstract class AbstractGrpcClientEventListener implements IPipelineEventListener,
    SmartInitializingSingleton, ApplicationContextAware {

    protected GrpcPushAdaptor grpcEmitterService;
    protected ApplicationContext applicationContext;
    protected PushService pushService;

    public AbstractGrpcClientEventListener() {
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.pushService = this.applicationContext.getBean(PushService.class);
        this.grpcEmitterService = this.applicationContext.getBean(GrpcPushAdaptor.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Class<? extends IEventReactive> listenerReactivePartition() {
        return NamingGrpcClientEventReactive.class;
    }
}
