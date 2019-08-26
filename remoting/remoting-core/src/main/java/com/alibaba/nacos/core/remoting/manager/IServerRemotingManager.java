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

import com.alibaba.nacos.core.remoting.event.listener.StartupServerEventListener;

/**
 * a remoting manager implement for server side
 *
 * @author pbting
 * @date 2019-08-22 10:52 PM
 */
public interface IServerRemotingManager extends IRemotingManager {

    /**
     * set the server
     *
     * @param server
     * @param <T>
     */
    <T> void setServer(T server);

    /**
     * @param <T>
     * @return
     */
    <T> T getServer();

    /**
     * initialize a startup server event listener
     *
     * @return
     */
    StartupServerEventListener initStartupServerEventListener();

    /**
     * @param streamTopicIdentify
     * @param streamReplayProcessor
     * @param <V>
     */
    <V> void mappingStreamReplayProcessor(String streamTopicIdentify,
                                          V streamReplayProcessor);
}
