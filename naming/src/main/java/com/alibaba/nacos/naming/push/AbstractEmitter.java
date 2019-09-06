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
package com.alibaba.nacos.naming.push;

import com.alibaba.nacos.api.naming.push.PushPacket;
import com.alibaba.nacos.naming.core.Service;
import com.alibaba.nacos.naming.misc.UtilsAndCommons;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.zip.GZIPOutputStream;

/**
 * @author pbting
 * @date 2019-08-28 9:01 AM
 */
public abstract class AbstractEmitter implements IEmitter,
    ApplicationListener<ServiceChangeEvent>, SmartInitializingSingleton {

    private Map<String, Future> futureMap = new ConcurrentHashMap<>();
    protected volatile ConcurrentHashMap<String, Long> sendTimeMap = new ConcurrentHashMap<>();

    protected ApplicationContext applicationContext;
    protected PushService pushService;

    public AbstractEmitter(ApplicationContext applicationContext, PushService pushService) {
        this.applicationContext = applicationContext;
        this.pushService = pushService;
    }

    @Override
    public void afterSingletonsInstantiated() {
        initEmitter();
    }

    @Override
    public void onApplicationEvent(ServiceChangeEvent event) {
        Service service = event.getService();
        emitter(service);
    }

    public byte[] compressIfNecessary(byte[] dataBytes) throws IOException {
        // enable compression when data is larger than 1KB
        int maxDataSizeUncompress = 1024;
        if (dataBytes.length < maxDataSizeUncompress) {
            return dataBytes;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(dataBytes);
        gzip.close();
        return out.toByteArray();
    }

    /**
     * this method will return not empty never
     *
     * @param client
     * @return
     */
    public PushPacket prepareHostsData(AbstractPushClient client) {
        PushPacket ackPacket = new PushPacket();
        ackPacket.setType("dom");
        ackPacket.setData(client.getDataSource().getData(client));

        return ackPacket;
    }

    public String getPushCacheKey(String serviceName, String agent) {
        return serviceName + UtilsAndCommons.CACHE_KEY_SPLITER + agent;
    }

    public void removeFutureMap(String key) {
        futureMap.remove(key);
    }

    public void putFutureMap(String key, Future future) {

        futureMap.put(key, future);
    }

    public boolean isContainsFutureMap(String key) {

        return futureMap.containsKey(key);
    }

    public long getAndRemoveSendTime(String key, long defaultValue) {
        Long value = sendTimeMap.remove(key);
        return value != null ? value : defaultValue;
    }
}
