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
package com.alibaba.nacos.naming.push.grpc.filter.impl;

import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.naming.misc.SwitchDomain;
import com.alibaba.nacos.naming.push.AbstractEmitter;
import com.alibaba.nacos.naming.push.AbstractPushClient;
import com.alibaba.nacos.naming.push.grpc.filter.IClientPushFilter;
import com.alibaba.nacos.naming.push.grpc.listener.GrpcEmitterEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author pbting
 * @date 2019-09-06 2:22 PM
 */
@Component
public class ClientPushStateFilter implements IClientPushFilter {

    private static final Logger logger = LoggerFactory.getLogger(ClientPushStateFilter.class);

    @Override
    public boolean aheadFilter(Event event) {
        AbstractEmitter emitter = (AbstractEmitter) event.getSource();
        SwitchDomain switchDomain = emitter.getSwitchDomain();
        if (!switchDomain.isPushEnabled()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean backFilter(Event event) {

        Map<String, AbstractPushClient> pushFailure =
            event.getParameter(GrpcEmitterEventListener.PUSH_FAILURE);

        if (pushFailure.size() > 0) {
            for (Map.Entry<String, AbstractPushClient> entry : pushFailure.entrySet()) {
                logger.info(" [push failure] " + " => " + entry.getValue().getSubscribeMetadata().toString());
            }
        } else {
            Map<String, AbstractPushClient> pushClient = event.getValue();
            logger.info(" [ push success] ,push client size " + pushClient.size());
            for (Map.Entry<String, AbstractPushClient> entry : pushFailure.entrySet()) {
                logger.info(" [success client] " + " => " + entry.getValue().getSubscribeMetadata().toString());
            }
        }
        return true;
    }

    @Override
    public int order() {
        return 1;
    }
}
