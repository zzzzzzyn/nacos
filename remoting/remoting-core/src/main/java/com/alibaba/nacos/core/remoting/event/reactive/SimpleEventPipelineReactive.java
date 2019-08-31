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
package com.alibaba.nacos.core.remoting.event.reactive;

import com.alibaba.nacos.core.remoting.event.Event;
import com.alibaba.nacos.core.remoting.event.IPipelineEventListener;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author pbting
 */
public class SimpleEventPipelineReactive implements IEventPipelineReactive {

    private static final InternalLogger log =
        InternalLoggerFactory.getInstance(SimpleEventPipelineReactive.class);

    protected ConcurrentHashMap<Class<? extends Event>, Collection<IPipelineEventListener>> listeners;
    protected ReentrantLock lock = new ReentrantLock();

    public SimpleEventPipelineReactive() {
    }

    @Override
    public boolean containsEventType(Class<? extends Event> eventType) {
        if (eventType == null || listeners == null) {
            return false;
        }

        return listeners.containsKey(eventType);
    }

    @Override
    public void addLast(IPipelineEventListener objectListener) {
        addListener0(objectListener, 1);
    }

    @Override
    public void addFirst(IPipelineEventListener objectListener) {
        addListener0(objectListener, 0);
    }

    @Override
    public void addListener(IPipelineEventListener pipelineEventListener) {
        addListener0(pipelineEventListener, 1);
    }

    private void addListener0(IPipelineEventListener pipelineEventListener, int order) {
        if (listeners == null) {
            lock.lock();
            try {
                if (listeners == null) {
                    listeners = new ConcurrentHashMap<>();
                }
            } finally {
                lock.unlock();
            }
        }

        Class<? extends Event>[] eventTypes = pipelineEventListener.interestEventTypes();
        for (Class eventType : eventTypes) {
            if (listeners.get(eventType) == null) {
                ConcurrentLinkedDeque<IPipelineEventListener> tempInfo = new ConcurrentLinkedDeque<>();
                if (order > 0) {
                    tempInfo.addLast(pipelineEventListener);
                } else {
                    tempInfo.addFirst(pipelineEventListener);
                }
                listeners.put(eventType, tempInfo);
            } else {
                listeners.get(eventType).add(pipelineEventListener);
            }
            debugEventMsg("register an event listener,the event type is" + eventType);
        }
    }

    public void removeListener(IPipelineEventListener objectListener,
                               Integer eventType) {
        if (listeners == null) {
            return;
        }

        lock.lock();
        try {
            Collection<IPipelineEventListener> tempInfo = listeners.get(eventType);
            if (tempInfo == null) {
                return;
            }
            if (tempInfo.size() == 1) {
                tempInfo.clear();
                return;
            }
            tempInfo.remove(objectListener);
        } finally {
            lock.unlock();
        }

        debugEventMsg("移除一个事件,类型为" + eventType);
    }

    public void removeListener(Integer eventType) {
        Collection<IPipelineEventListener> listener = listeners.remove(eventType);
        if (listener != null) {
            listener.clear();
        }
        debugEventMsg("移除一个事件,类型为" + eventType);
    }

    protected <T extends Event> void reactive0(T event) {
        if (event == null || listeners == null) {
            return;
        }

        Class<? extends Event> eventType = event.getEventType();
        Deque tempList = (Deque<IPipelineEventListener>) listeners.get(eventType);
        if (tempList == null) {
            tempList = (Deque<IPipelineEventListener>) listeners.get(event.getClass());
        }

        if (tempList == null || tempList.isEmpty()) {
            boolean isEmpty = true;
            for (Class<? extends Event> eventTypeClazz : listeners.keySet()) {
                if (eventTypeClazz.isAssignableFrom(eventType) ||
                    eventTypeClazz.isAssignableFrom(event.getClass())) {
                    tempList = (Deque<IPipelineEventListener>) listeners.get(eventTypeClazz);
                    listenerPerform(tempList, event);
                    isEmpty = false;
                }
            }
            if (isEmpty) {
                log.warn("event type {}, alias {} No event listener。");
            }
        } else {
            // 3、触发,
            listenerPerform(tempList, event);
        }
    }

    @Override
    public <T extends Event> void reactive(T event) {
        reactive0(event);
    }

    /**
     * 处理 单个的事件
     */
    protected <T extends Event> void listenerPerform(final Deque<IPipelineEventListener> objectListeners,
                                                     final T event) {
        EventExecutor eventExecutor = event.getEventExecutor();
        if (eventExecutor != null) {
            eventExecutor.execute(() -> listenerPerform0(objectListeners, event));
        } else {
            listenerPerform0(objectListeners, event);
        }
    }

    protected <T extends Event> void listenerPerform0(
        Deque<IPipelineEventListener> objectListeners, T event) {
        int index = 1;
        for (IPipelineEventListener listener : objectListeners) {
            try {
                boolean isSuccessor = listener.onEvent(event, index);
                if (!isSuccessor || event.isInterrupt()) {
                    break;
                }
            } catch (Exception e) {
                log.error("handler the event cause an exception."
                    + event.getValue().toString(), e);
            }
            index++;
        }
    }

    public void clearListener() {
        lock.lock();
        try {
            if (listeners != null) {
                listeners = null;
            }
        } finally {
            lock.unlock();
        }
    }

    protected void debugEventMsg(String msg) {
        if (log.isDebugEnabled()) {
            log.debug(msg);
        }
    }

}
