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
package com.alibaba.nacos.core.remoting.grpc;

/**
 * 处理来自客户端请求的事件类型。事件类型区间为：[100,1000]。100 以下的事件类型为调试使用.
 * <p>
 * 处理来自集群节点间请求的事件类型。事件类型区间为：[1100,2000]
 *
 * @author pbting
 * @date 2019-08-22 11:04 PM
 */
public interface IGrpcEventReactiveType {

    /**
     * 处理本节点内的发布事件。事件类型区间为：[2100,3000]
     */
    enum ILocalizationEventReactiveType {

        STARTUP_EVENT(2100, "Server 端启动事件");

        private int eventType;
        private String sketch;

        ILocalizationEventReactiveType(int eventType, String sketch) {

            this.eventType = eventType;
            this.sketch = sketch;
        }

        public int getEventType() {
            return eventType;
        }

        public String getSketch() {
            return sketch;
        }

    }

}
