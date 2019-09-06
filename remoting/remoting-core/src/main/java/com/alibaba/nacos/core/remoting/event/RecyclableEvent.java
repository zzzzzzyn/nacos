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
package com.alibaba.nacos.core.remoting.event;

/**
 * @author pbting
 * @date 2019-09-05 10:20 AM
 */
public class RecyclableEvent extends Event {

    private int recycleInterval;
    private boolean isCancel;

    public RecyclableEvent(Object source, Object value, String sink, int recycleInterval) {
        super(source, value, sink);
        this.recycleInterval = recycleInterval;
    }

    public RecyclableEvent(Object source, int recycleInterval, String sink) {
        super(source, EMPTY_VALUE, sink);
        this.recycleInterval = recycleInterval;
    }

    public int getRecycleInterval() {
        return recycleInterval;
    }

    public void setRecycleInterval(int recycleInterval) {
        this.recycleInterval = recycleInterval;
    }

    public boolean isCancel() {
        return isCancel;
    }

    public void setCancel(boolean cancel) {
        isCancel = cancel;
    }
}
