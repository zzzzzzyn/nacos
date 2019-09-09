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
package com.alibaba.nacos.client.naming.core;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.client.naming.core.builder.ServiceAwareStrategyBuilder;

import java.util.Map;

/**
 * a strategy which is for aware changed service
 *
 * @author pbting
 * @date 2019-08-30 11:30 AM
 */
public interface IServiceAwareStrategy {

    /**
     * initializer a strategy which is for aware changed service.
     *
     * @param serviceChangedStrategyConfig some configuration for init service changed aware strategy
     */
    void initServiceAwareStrategy(ServiceAwareStrategyBuilder.ServiceAwareStrategyConfig serviceChangedStrategyConfig);

    /**
     * can get service info use this method.
     * will get from local memory first,if is empty ,then get from server directly.
     *
     * @param serviceName
     * @param clusters
     * @return
     */
    ServiceInfo getServiceInfo(String serviceName, String clusters);

    /**
     * get service info from server directly
     *
     * @param serviceName
     * @param clusters
     * @return
     * @throws NacosException
     */
    ServiceInfo getServiceInfoDirectlyFromServer(String serviceName, String clusters) throws NacosException;

    /**
     * process data stream response from server
     *
     * @param json
     * @return
     */
    ServiceInfo processDataStreamResponse(String json);

    /**
     * get service info mapping
     *
     * @return
     */
    Map<String, ServiceInfo> getServiceInfoMap();
}
