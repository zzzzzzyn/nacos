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
package com.alibaba.nacos.client.naming.backups;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.client.naming.cache.ConcurrentDiskUtil;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.alibaba.nacos.client.naming.utils.UtilAndComs;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;

/**
 * @author pbting
 * @date 2019-08-30 1:08 PM
 */
class FailoverFileReader implements Runnable {

    private FailoverReactor failoverReactor;

    public FailoverFileReader(FailoverReactor failoverReactor) {
        this.failoverReactor = failoverReactor;
    }

    @Override
    public void run() {
        Map<String, ServiceInfo> domMap = new HashMap<String, ServiceInfo>(16);
        String failoverDir = failoverReactor.getFailoverDir();
        BufferedReader reader = null;
        try {

            File cacheDir = new File(failoverDir);
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                throw new IllegalStateException("failed to create cache dir: " + failoverDir);
            }

            File[] files = cacheDir.listFiles();
            if (files == null) {
                return;
            }

            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }

                if (file.getName().equals(UtilAndComs.FAILOVER_SWITCH)) {
                    continue;
                }

                ServiceInfo dom = new ServiceInfo(file.getName());

                try {
                    String dataString = ConcurrentDiskUtil.getFileContent(file,
                        Charset.defaultCharset().toString());
                    reader = new BufferedReader(new StringReader(dataString));

                    String json;
                    if ((json = reader.readLine()) != null) {
                        try {
                            dom = JSON.parseObject(json, ServiceInfo.class);
                        } catch (Exception e) {
                            NAMING_LOGGER.error("[NA] error while parsing cached dom : " + json, e);
                        }
                    }

                } catch (Exception e) {
                    NAMING_LOGGER.error("[NA] failed to read cache for dom: " + file.getName(), e);
                } finally {
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                }
                if (!CollectionUtils.isEmpty(dom.getHosts())) {
                    domMap.put(dom.getKey(), dom);
                }
            }
        } catch (Exception e) {
            NAMING_LOGGER.error("[NA] failed to read cache file", e);
        }

        if (domMap.size() > 0) {
            failoverReactor.overrideServiceMap(domMap);
        }
    }
}
