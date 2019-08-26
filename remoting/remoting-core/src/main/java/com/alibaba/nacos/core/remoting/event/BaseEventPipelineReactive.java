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
package com.alibaba.nacos.core.remoting.event;

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
public class BaseEventPipelineReactive implements IEventPipelineReactive {

    private static final InternalLogger log =
        InternalLoggerFactory.getInstance(BaseEventPipelineReactive.class);

    protected ConcurrentHashMap<Integer, Collection<IPipelineEventListener>> listeners;
    protected ReentrantLock lock = new ReentrantLock();

    public BaseEventPipelineReactive() {
    }

    @Override
    public boolean containsEventType(Integer eventType) {
        if (eventType == null || listeners == null) {
            return false;
        }

        return listeners.containsKey(eventType);
    }

    public void addLast(IPipelineEventListener objectListener, Integer eventType) {
        addListener0(objectListener, eventType, 1);
    }

    @Override
    public void addFirst(IPipelineEventListener objectListener, Integer eventType) {
        addListener0(objectListener, eventType, 0);
    }

    public void addListener(IPipelineEventListener pipelineEventListener, Integer eventType) {
        addListener0(pipelineEventListener, eventType, 1);
    }

    private void addListener0(IPipelineEventListener objectListener, Integer eventType,
                              int order) {
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

        if (listeners.get(eventType) == null) {
            ConcurrentLinkedDeque<IPipelineEventListener> tempInfo = new ConcurrentLinkedDeque<>();
            if (order > 0) {
                tempInfo.addLast(objectListener);
            } else {
                tempInfo.addFirst(objectListener);
            }
            listeners.put(eventType, tempInfo);
        } else {
            listeners.get(eventType).add(objectListener);
        }

        debugEventMsg("注册一个事件,类型为" + eventType);
    }

    public void removeListener(IPipelineEventListener objectListener,
                               Integer eventType) {
        if (listeners == null)
            return;
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

    protected final void doPipelineReactive(Event event) {
        if (event == null || listeners == null) {
            return;
        }

        int eventType = event.getEventType();
        Deque tempList = (Deque<IPipelineEventListener>) listeners.get(eventType);

        if (tempList == null || tempList.isEmpty()) {
            log.warn("event type {}, alias {} No event listener。");
            return;
        }

        // 3、触发,
        listenerReactive(tempList, event);
    }

    public void pipelineReactive(Event event) {
        doPipelineReactive(event);
    }

    /**
     * 处理 单个的事件
     */
    public void listenerReactive(final Deque<IPipelineEventListener> objectListeners,
                                 final Event event) {
        if (event.getEventExecutor() != null) {
            event.getEventExecutor().execute(() -> doListenerReactive(objectListeners, event));
        } else {
            doListenerReactive(objectListeners, event);
        }
    }

    protected final void doListenerReactive(
        Deque<IPipelineEventListener> objectListeners, Event event) {
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
