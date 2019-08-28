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

import com.alibaba.nacos.naming.core.Service;
import com.alibaba.nacos.naming.misc.UtilsAndCommons;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * @author pbting
 * @date 2019-08-28 9:01 AM
 */
public abstract class AbstractEmitterSupport implements IEmitter, ApplicationListener<ServiceChangeEvent>, SmartInitializingSingleton {

    protected ApplicationContext applicationContext;

    public AbstractEmitterSupport(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        initEmitter();
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return applicationContext;
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

    public Map<String, Object> prepareHostsData(AbstractPushClientSupport client) throws Exception {
        Map<String, Object> cmd = new HashMap(2);
        cmd.put("type", "dom");
        cmd.put("data", client.getDataSource().getData(client));

        return cmd;
    }

    public String getPushCacheKey(String serviceName, String agent) {
        return serviceName + UtilsAndCommons.CACHE_KEY_SPLITER + agent;
    }
}
