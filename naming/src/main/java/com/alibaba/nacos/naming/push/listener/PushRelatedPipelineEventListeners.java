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
package com.alibaba.nacos.naming.push.listener;

import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import com.alibaba.nacos.core.remoting.event.RecyclableEvent;
import com.alibaba.nacos.naming.push.IEventType;
import com.alibaba.nacos.naming.push.AbstractReTransmitter;
import com.alibaba.nacos.naming.push.PushService;

/**
 * some pipeline event listeners for push related
 *
 * @author pbting
 * @date 2019-08-28 4:26 PM
 */
public final class PushRelatedPipelineEventListeners {

    /**
     * an event listener for remove client if zombie
     */
    public static class RemoveClientIfZombieEventListener implements IPipelineEventListener<RecyclableEvent> {

        private PushService pushService;

        public RemoveClientIfZombieEventListener(PushService pushService) {
            this.pushService = pushService;
        }

        @Override
        public boolean onEvent(RecyclableEvent event, int listenerIndex) {
            pushService.removeClientIfZombie();
            return true;
        }

        @Override
        public int[] interestEventTypes() {
            return new int[]{IEventType.REMOVE_CLIENT_IF_ZOMBIE};
        }
    }

    /**
     * an event listener for push time check and maybe re-transmitter
     */
    public static class ReTransmitterEventListener implements IPipelineEventListener<RecyclableEvent> {

        @Override
        public boolean onEvent(RecyclableEvent event, int listenerIndex) {
            AbstractReTransmitter reTransmitter = event.getValue();
            reTransmitter.run();
            event.cancel();
            return true;
        }

        @Override
        public int[] interestEventTypes() {
            return new int[]{IEventType.RE_TRANSMITTER};
        }
    }
}
