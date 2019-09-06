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

import io.netty.util.concurrent.EventExecutor;

import java.util.EventObject;
import java.util.HashMap;

/**
 * @author pbting
 * @date 2019-08-22 4:59 PM
 */
public class Event extends EventObject implements IEventContext {

    public static final Object EMPTY_VALUE = new Object();
    public static final String EMPTY_SINK = "";

    protected HashMap<String, Object> eventContext = null;

    protected boolean isInterrupt;

    private String alias;
    private Object value;
    /**
     * the sink of event from remoting request
     */
    private String sink;
    private EventExecutor eventExecutor;

    public Event(Object source, Object value, String sink) {
        super(source);
        this.value = value;
        this.sink = sink;
    }

    public Event(Object source, Object value) {
        this(source, value, EMPTY_SINK);
    }

    public Event(Object source) {
        this(source, EMPTY_VALUE, EMPTY_SINK);
    }

    public <T> T getValue() {
        return (T) value;
    }

    public <T> void setValue(T value) {
        this.value = value;
    }

    public void setSink(String sink) {
        this.sink = sink;
    }

    public String getSink() {
        return sink;
    }

    @Override
    public boolean isInterrupt() {
        return isInterrupt;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias == null ? this.getSource().getClass().getCanonicalName() : alias;
    }

    @Override
    public void setInterrupt(boolean isInterrupt) {
        this.isInterrupt = isInterrupt;
    }

    @Override
    public <T> void setParameter(String key, T value) {
        this.getEventContext().put(key, value);
    }

    @Override
    public <T> T getParameter(String key) {
        return (T) getEventContext().get(key);
    }

    @Override
    public <T> T getParameter(String key, T defaultValue) {
        return (T) getEventContext().getOrDefault(key, defaultValue);
    }

    @Override
    public <T> T removeParameter(String key) {

        return (T) this.getEventContext().remove(key);
    }

    private HashMap<String, Object> getEventContext() {
        HashMap<String, Object> tmpContext = eventContext;
        if (tmpContext == null) {
            synchronized (this) {
                tmpContext = eventContext;
                if (tmpContext == null) {
                    eventContext = new HashMap<>();
                    tmpContext = eventContext;
                }
            }
        }
        return tmpContext;
    }

    public EventExecutor getEventExecutor() {
        return eventExecutor;
    }

    public void setEventExecutor(EventExecutor eventExecutor) {
        this.eventExecutor = eventExecutor;
    }
}
