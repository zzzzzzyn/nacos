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

import com.alibaba.nacos.client.naming.core.AbstractServiceChangedAwareStrategy;
import com.alibaba.nacos.client.naming.core.builder.ServiceChangedAwareStrategyBuilder;

/**
 * aware service changes based on gRPC
 *
 * @author pbting
 * @date 2019-09-03 9:41 AM
 */
public class GrpcServiceChangedAwareStrategy extends AbstractServiceChangedAwareStrategy {

    public GrpcServiceChangedAwareStrategy() {
    }

    @Override
    public void updateService(String serviceName, String clusters) {

    }

    @Override
    public void initServiceChangedAwareStrategy(ServiceChangedAwareStrategyBuilder.ServiceChangedStrategyConfig serviceChangedStrategyConfig) {

        // 1. init common state
        initCommonState(serviceChangedStrategyConfig);
    }
}
