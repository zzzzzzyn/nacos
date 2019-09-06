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
package com.alibaba.nacos.naming.push.events;

import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.event.RecyclableEvent;

/**
 * @author pbting
 * @date 2019-08-31 12:50 AM
 */
public final class LocalizationEvents {

    /**
     * zombie push client check recycle event
     */
    public static class ZombiePushClientCheckEvent extends RecyclableEvent {

        public ZombiePushClientCheckEvent(Object source, int recycleInterval) {
            super(source, recycleInterval, ZombiePushClientCheckEvent.class.getName());
        }
    }

    /**
     * the re-transmitter event
     */
    public static class ReTransmitterEvent extends Event {
        public ReTransmitterEvent(Object source, Object value) {
            super(source, value, ReTransmitterEvent.class.getName());
        }
    }
}
