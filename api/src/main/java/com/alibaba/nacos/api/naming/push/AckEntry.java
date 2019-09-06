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

import java.io.Serializable;
import java.net.DatagramPacket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pbting
 * @date 2019-08-27 5:47 PM
 */
public class AckEntry implements Serializable {

    private String key;
    private DatagramPacket origin;
    private AtomicInteger retryTimes = new AtomicInteger(0);
    private PushPacket data;

    public AckEntry(String key, DatagramPacket packet) {
        this.key = key;
        this.origin = packet;
    }

    public void increaseRetryTime() {
        retryTimes.incrementAndGet();
    }

    public int getRetryTimes() {
        return retryTimes.get();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public DatagramPacket getOrigin() {
        return origin;
    }

    public void setOrigin(DatagramPacket origin) {
        this.origin = origin;
    }

    public void setRetryTimes(AtomicInteger retryTimes) {
        this.retryTimes = retryTimes;
    }

    public PushPacket getData() {
        return data;
    }

    public void setData(PushPacket data) {
        this.data = data;
    }
}
