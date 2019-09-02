package com.alibaba.nacos.api.naming.push;

import com.alibaba.nacos.core.remoting.event.Event;

/**
 *
 */
public enum EventTypeMapping {

    SUBSCRIBE_EVENT(1, GrpcClientSubscribeEvent.class);

    private int eventTypeLabel;
    private Class<? extends Event> eventType;

    EventTypeMapping(int eventTypeLabel, Class<? extends Event> eventType) {
        this.eventTypeLabel = eventTypeLabel;
        this.eventType = eventType;
    }

    public int getEventTypeLabel() {
        return eventTypeLabel;
    }

    public Class<? extends Event> getEventType() {
        return eventType;
    }
}
