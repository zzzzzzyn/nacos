/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.nacos.core.remoting.event;

/**
 * The event context that is shared when executed in multiple event listeners.
 * <p>
 * It can be used to store some intermediate variables and status values ​​for upstream and downstream processing.
 *
 * @author pbting
 */
public interface IEventContext {

    /**
     * Whether the event currently needs to be interrupted during the processing of the event pipeline.
     *
     * @return
     */
    boolean isInterrupt();

    /**
     * The event currently needs to be interrupted during the processing of the event pipeline.
     *
     * @param isBroken
     */
    void setInterrupt(boolean isBroken);

    /**
     * Pass some parameters in event context
     *
     * @param key
     * @param value
     * @param <T>
     */
    <T> void setParameter(String key, T value);

    /**
     * get the Parameter by the key
     *
     * @param key
     * @param <T>
     * @return
     */
    <T> T getParameter(String key);

    /**
     * get the Parameter by the key . can use the default value
     * When there is no associated parameter specified
     *
     * @param key
     * @param defaultValue
     * @param <T>
     * @return
     */
    <T> T getParameter(String key, T defaultValue);

    /**
     * This method can be used when you need to remove a parameter.
     *
     * @param key
     */
    <T> T removeParameter(String key);
}
