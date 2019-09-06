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
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author pbting
 */
public class DefaultEventReactive implements IEventReactive, IEventReactiveHelm {

    private static final InternalLogger log =
        InternalLoggerFactory.getInstance(DefaultEventReactive.class);

    private final int ahead = 0;
    private final int back = 1;

    protected ConcurrentHashMap<String, LinkedList<IPipelineEventListener>> listenersSinkRegistry = new ConcurrentHashMap<>();
    protected HashMap<List, Boolean> sortableFlagMapping = new HashMap<>();
    protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Deque<IEventReactiveFilter> eventReactiveFilters = new LinkedList<>();

    public DefaultEventReactive() {
    }

    @Override
    public void registerEventReactiveFilter(Collection<? extends IEventReactiveFilter> reactiveFilters) {
        if (reactiveFilters == null || reactiveFilters.isEmpty()) {
            return;
        }

        synchronized (eventReactiveFilters) {
            this.eventReactiveFilters.addAll(reactiveFilters);
            Collections.sort((List) eventReactiveFilters, new Comparator<IEventReactiveFilter>() {
                @Override
                public int compare(IEventReactiveFilter o1, IEventReactiveFilter o2) {
                    return o1.order() - o2.order();
                }
            });
        }

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
        String[] eventSinks = pipelineEventListener.interestSinks();
        for (String eventSink : eventSinks) {
            if (listenersSinkRegistry.get(eventSink) == null) {
                LinkedList<IPipelineEventListener> tempInfo;
                ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
                try {
                    writeLock.lock();
                    tempInfo = listenersSinkRegistry.get(eventSink);
                    if (tempInfo == null) {
                        tempInfo = new LinkedList<>();
                        listenersSinkRegistry.put(eventSink, tempInfo);
                    }

                    if (order == back) {
                        tempInfo.addLast(pipelineEventListener);
                    } else {
                        tempInfo.addFirst(pipelineEventListener);
                    }
                } finally {
                    writeLock.unlock();
                }
            } else {
                LinkedList<IPipelineEventListener> tempInfo = listenersSinkRegistry.get(eventSink);
                ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
                try {
                    writeLock.lock();
                    tempInfo.add(pipelineEventListener);
                } finally {
                    writeLock.unlock();
                }
            }
            debugEventMsg("register an event listenersSinkRegistry,the event type is" + eventSink);
        }
    }

    public void removeListener(IPipelineEventListener objectListener, String eventSink) {
        if (listenersSinkRegistry == null) {
            return;
        }
        Collection<IPipelineEventListener> tempInfo = listenersSinkRegistry.get(eventSink);
        if (tempInfo == null) {
            return;
        }

        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            if (tempInfo.size() == 1) {
                tempInfo.clear();
                return;
            }
            tempInfo.remove(objectListener);
        } finally {
            writeLock.unlock();
        }
        debugEventMsg("移除一个事件,类型为" + eventSink);
    }

    public void removeListener(String eventSink) {
        Collection<IPipelineEventListener> listener = listenersSinkRegistry.remove(eventSink);
        if (listener != null) {
            ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
            try {
                writeLock.lock();
                listener.clear();
            } finally {
                writeLock.unlock();
            }
        }
        debugEventMsg("移除一个事件,类型为" + eventSink);
    }

    protected void reactive0(Event event) {
        if (event == null || listenersSinkRegistry == null) {
            return;
        }
        List tempList = getAndSortEventListeners(event);
        if (Objects.isNull(tempList)) {
            log.warn("event sink {}, alias {} No event listenersSinkRegistry。", event.getSink());
            return;
        }
        listenerPerform(tempList, event);
    }

    /**
     * @param event
     * @return
     */
    protected List<IPipelineEventListener> getAndSortEventListeners(Event event) {
        List tempList = getEventListeners(event);
        if (tempList == null) {
            return null;
        }
        sortEventListeners(tempList);
        return tempList;
    }

    private List getEventListeners(Event event) {
        String eventSink = event.getSink();
        if (eventSink == null || eventSink.equals(Event.EMPTY_SINK)) {
            eventSink = event.getClass().getName();
        }

        return listenersSinkRegistry.get(eventSink);
    }

    private void sortEventListeners(List<IPipelineEventListener> tempList) {
        Boolean isSortable = sortableFlagMapping.get(tempList);
        if (isSortable != null) {
            return;
        }
        synchronized (sortableFlagMapping) {
            isSortable = sortableFlagMapping.get(tempList);
            if (isSortable != null) {
                return;
            }
            Collections.sort(tempList, (o1, o2) -> (o1.pipelineOrder() - o2.pipelineOrder()));
            sortableFlagMapping.put(tempList, true);
        }
    }

    @Override

    public void reactive(Event event) {
        reactive0(event);
    }

    /**
     * 处理 单个的事件
     */
    protected <T extends Event> void listenerPerform(final List<IPipelineEventListener> objectListeners,
                                                     final T event) {
        EventExecutor eventExecutor = event.getEventExecutor();
        if (eventExecutor != null) {
            eventExecutor.execute(() -> listenerPerform0(objectListeners, event));
        } else {
            listenerPerform0(objectListeners, event);
        }
    }

    protected <T extends Event> void listenerPerform0(
        List<IPipelineEventListener> objectListeners, T event) {
        try {
            if (!aheadFilter(event)) {
                return;
            }
            drain(objectListeners, event);
        } finally {
            backFilter(event);
        }
    }

    private <T extends Event> void drain(List<IPipelineEventListener> objectListeners, T event) {
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
        listenersSinkRegistry.clear();
    }

    protected void debugEventMsg(String msg) {
        if (log.isDebugEnabled()) {
            log.debug(msg);
        }
    }

}
