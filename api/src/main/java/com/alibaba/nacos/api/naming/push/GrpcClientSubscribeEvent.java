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
package com.alibaba.nacos.api.naming.push;

import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.grpc.event.GrpcClientRequestStreamEvent;

/**
 * @author pbting
 * @date 2019-08-31 4:32 PM
 */
public class GrpcClientSubscribeEvent extends GrpcClientRequestStreamEvent {

    public GrpcClientSubscribeEvent(Object source, Object value, Class<? extends Event> eventType) {
        super(source, value, eventType);
    }
}
