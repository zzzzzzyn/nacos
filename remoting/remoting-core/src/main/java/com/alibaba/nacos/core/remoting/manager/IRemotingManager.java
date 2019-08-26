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
package com.alibaba.nacos.core.remoting.manager;

import com.alibaba.nacos.core.remoting.Identify;
import com.alibaba.nacos.core.remoting.event.IEventPipelineReactive;

/**
 * a remoting manager ,it will use it to Initialize all remotely related behaviors。
 * It has two classic scene implementations. one is {@IServerRemotingManager} and
 * another is {@IClientRemotingManager}
 *
 * @author pbting
 * @date 2019-08-22 5:42 PM
 */
public interface IRemotingManager extends Identify {

    /**
     * get an event pipeline reactive by the given event reactive partion.
     *
     * @return an IEventPipelineReactive instance  If there is a specific by the input parameter or return null.
     */
    IEventPipelineReactive getAbstractEventPipelineReactive(Class<? extends IEventPipelineReactive> eventReactivePartition);

    /**
     * init  an event pipeline reactive.
     *
     * @param eventReactive
     * @param eventReactive
     */
    void initEventPipelineReactive(IEventPipelineReactive eventReactive);

    /**
     * @param key
     * @return
     */
    default String builderIdentify(String key) {
        return identify() + "@" + key;
    }
}
