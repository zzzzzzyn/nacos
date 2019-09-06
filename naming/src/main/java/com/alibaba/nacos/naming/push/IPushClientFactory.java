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
package com.alibaba.nacos.naming.push;

import com.alibaba.nacos.api.naming.push.SubscribeMetadata;

/**
 * @author pbting
 * @date 2019-09-04 2:23 PM
 */
public interface IPushClientFactory {

    /**
     * construct a push client with the {@SubscribeMetadata} and {@DataSource}
     *
     * @param subscribeMetadata the metadata of subscribe.the information will transfer by client
     * @param dataSource        when push some service instance will use it to get the data source
     * @return
     */
    default AbstractPushClient newPushClient(SubscribeMetadata subscribeMetadata, DataSource dataSource) {
        // nothing to do
        return null;
    }


    /**
     * construct a push client with the {@SubscribeMetadata} 、 {@DataSource} and pusher
     *
     * @param subscribeMetadata the metadata of subscribe.the information will transfer by client
     * @param dataSource        when push some service instance will use it to get the data source
     * @param pusher            when push the data will use the pusher
     * @return
     */
    default <T> AbstractPushClient newPushClient(SubscribeMetadata subscribeMetadata, DataSource dataSource, T pusher) {
        // nothing to do
        return null;
    }
}
