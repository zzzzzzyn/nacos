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
package com.alibaba.nacos.naming.push.grpc.listener;

import com.alibaba.nacos.naming.misc.Loggers;
import com.alibaba.nacos.naming.push.AbstractPushClient;
import com.alibaba.nacos.naming.push.grpc.GrpcEmitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author pbting
 * @date 2019-09-06 3:34 PM
 */
@Component
public class GrpcRetryPushCheckEventListener extends AbstractGrpcEmitterEventListener {

    private static final Logger logger = LoggerFactory.getLogger(GrpcRetryPushCheckEventListener.class);

    @Override
    public boolean onEvent(GrpcEmitterService.EmitterRecyclableEvent event, int listenerIndex) {
        int pushTimes = event.getParameter(PUSH_TIMES, 0);
        pushTimes++;
        event.setParameter(PUSH_TIMES, pushTimes);
        Map<String, AbstractPushClient>
            pushFailure = event.getParameter(AbstractGrpcEmitterEventListener.PUSH_FAILURE);
        if (pushFailure != null && pushFailure.size() > 0) {
            event.setValue(pushFailure);
            event.setRecycleInterval((int) TimeUnit.MILLISECONDS.toSeconds(event.getParameter(PUSH_CACHE_MILLS)));
            int pushMaxRetryTimes = event.getParameter(PUSH_MAX_RETRY_TIMES, 1);
            if (pushTimes > pushMaxRetryTimes) {
                for (AbstractPushClient abstractPushClient : pushFailure.values()) {
                    Loggers.GRPC_PUSH.info("{} => push failure after max push retry time {}", abstractPushClient.getSubscribeMetadata().toString(), pushMaxRetryTimes);
                }
                event.setCancel(true);
            }
        } else {
            event.setCancel(true);
        }

        return true;
    }

    @Override
    public int pipelineOrder() {
        return 2;
    }
}
