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
import com.alibaba.nacos.naming.push.grpc.filter.IClientProcessFilter;
import org.springframework.stereotype.Component;

/**
 * @author pbting
 * @date 2019-09-05 11:43 AM
 */
@Component
public class ClientProcessRtFilter implements IClientProcessFilter {

    private static final String START_PROCESS_TIME = "start.process.time";

    @Override
    public boolean backFilter(Event event) {
        long start = event.getParameter(START_PROCESS_TIME);
        System.err.println("[" + event.getSink() + "] cost time =>" + (System.currentTimeMillis() - start) + " Ms.");
        return true;
    }

    @Override
    public boolean aheadFilter(Event event) {
        event.setParameter(START_PROCESS_TIME, System.currentTimeMillis());
        return true;
    }
}
