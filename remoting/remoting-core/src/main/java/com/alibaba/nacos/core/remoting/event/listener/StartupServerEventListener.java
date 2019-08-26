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
package com.alibaba.nacos.core.remoting.event.listener;

import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import com.alibaba.nacos.core.remoting.event.LocalizationEvent;

/**
 * an event listener to startup server
 *
 * @author pbting
 * @date 2019-08-22 5:44 PM
 */
public abstract class StartupServerEventListener implements IPipelineEventListener<LocalizationEvent> {

    /**
     * Processing entry in response to an event。
     *
     * @param event         Localization Event
     * @param listenerIndex The eventListener index of the current response event
     * @return
     */
    @Override
    public boolean onEvent(LocalizationEvent event, int listenerIndex) {
        return onStartup(event);
    }

    /**
     * startup a server
     *
     * @param event
     * @return
     */
    public abstract boolean onStartup(LocalizationEvent event);
}
