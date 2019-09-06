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
import com.alibaba.nacos.core.remoting.event.filter.IEventReactiveFilter;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author pbting
 */
public class DefaultEventReactive implements IEventReactive, IEventReactiveHelm {

    private static final InternalLogger log =
        InternalLoggerFactory.getInstance(DefaultEventReactive.class);

    private final int ahead = 0;
    private final int back = 1;

    protected ConcurrentHashMap<String, Collection<IPipelineEventListener>> listenersSinkRegistry;
    protected ReentrantLock lock = new ReentrantLock();
    private Deque<IEventReactiveFilter> eventReactiveFilters = new LinkedList<>();

    public DefaultEventReactive() {
    }

    @Override
    public void registerEventReactiveFilter(Collection<IEventReactiveFilter> reactiveFilters) {
        if (reactiveFilters == null || reactiveFilters.isEmpty()) {
            return;
        }

        this.eventReactiveFilters.addAll(reactiveFilters);
        Collections.sort((List) eventReactiveFilters, new Comparator<IEventReactiveFilter>() {
            @Override
            public int compare(IEventReactiveFilter o1, IEventReactiveFilter o2) {
                return o1.order() - o2.order();
            }
        });
    }

    /**
     * @param event
     * @return
     */
    @Override
    public boolean aheadFilter(Event event) {
        Iterator<IEventReactiveFilter> filterIterator = eventReactiveFilters.iterator();
        return filterPerform(event, filterIterator, ahead);
    }

    /**
     * @param event
     * @return
     */
    @Override
    public boolean backFilter(Event event) {
        Iterator<IEventReactiveFilter> filterIterator = eventReactiveFilters.descendingIterator();
        return filterPerform(event, filterIterator, back);
    }

    private boolean filterPerform(Event event, Iterator<IEventReactiveFilter> filterIterator, int order) {
        boolean result = true;
        while (filterIterator.hasNext()) {
            IEventReactiveFilter filter = filterIterator.next();
            if (order == ahead) {
                if (!filter.aheadFilter(event)) {
                    result = false;
                }
            } else if (order == back) {
                if (!filter.backFilter(event)) {
                    result = false;
                }
            }
        }
        return result;
    }

    @Override
    public boolean containsEventSink(String eventSink) {
        if (eventSink == null || listenersSinkRegistry == null) {
            return false;
        }

        return listenersSinkRegistry.containsKey(eventSink);
    }

    @Override
    public void addLast(IPipelineEventListener objectListener) {
        addListener0(objectListener, back);
    }

    @Override
    public void addFirst(IPipelineEventListener objectListener) {
        addListener0(objectListener, ahead);
    }

    @Override
    public void addListener(IPipelineEventListener pipelineEventListener) {
        addListener0(pipelineEventListener, back);
    }

    private void addListener0(IPipelineEventListener pipelineEventListener, int order) {
        if (listenersSinkRegistry == null) {
            lock.lock();
            try {
                if (listenersSinkRegistry == null) {
                    listenersSinkRegistry = new ConcurrentHashMap<>();
                }
            } finally {
                lock.unlock();
            }
        }

        String[] eventSinks = pipelineEventListener.interestSinks();
        for (String eventSink : eventSinks) {
            if (listenersSinkRegistry.get(eventSink) == null) {
                ConcurrentLinkedDeque<IPipelineEventListener> tempInfo = new ConcurrentLinkedDeque<>();
                if (order == back) {
                    tempInfo.addLast(pipelineEventListener);
                } else {
                    tempInfo.addFirst(pipelineEventListener);
                }
                listenersSinkRegistry.put(eventSink, tempInfo);
            } else {
                listenersSinkRegistry.get(eventSink).add(pipelineEventListener);
            }
            debugEventMsg("register an event listenersSinkRegistry,the event type is" + eventSink);
        }
    }

    public void removeListener(IPipelineEventListener objectListener, String eventSink) {
        if (listenersSinkRegistry == null) {
            return;
        }

        lock.lock();
        try {
            Collection<IPipelineEventListener> tempInfo = listenersSinkRegistry.get(eventSink);
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

        debugEventMsg("移除一个事件,类型为" + eventSink);
    }

    public void removeListener(String eventSink) {
        Collection<IPipelineEventListener> listener = listenersSinkRegistry.remove(eventSink);
        if (listener != null) {
            listener.clear();
        }
        debugEventMsg("移除一个事件,类型为" + eventSink);
    }

    protected void reactive0(Event event) {
        if (event == null || listenersSinkRegistry == null) {
            return;
        }

        String eventSink = event.getSink();
        Deque tempList = (Deque<IPipelineEventListener>) listenersSinkRegistry.get(eventSink);

        if (tempList == null || tempList.isEmpty()) {
            log.warn("event sink {}, alias {} No event listenersSinkRegistry。", eventSink);
        } else {
            // 3、触发,
            listenerPerform(tempList, event);
        }
    }

    @Override
    public void reactive(Event event) {
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
        try {
            if (!aheadFilter(event)) {
                return;
            }
            perform(objectListeners, event);
        } finally {
            backFilter(event);
        }
    }

    private <T extends Event> void perform(Deque<IPipelineEventListener> objectListeners, T event) {
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
            if (listenersSinkRegistry != null) {
                listenersSinkRegistry = null;
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
