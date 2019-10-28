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

import com.alibaba.nacos.client.naming.core.IServiceAwareStrategy;

import java.io.File;

import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;

/**
 * @author pbting
 * @date 2019-08-30 1:39 PM
 */
class BackupFileCheckOnStartupTask implements Runnable {

    private String failoverDir;
    private IServiceAwareStrategy serviceChangedAwareStrategy;

    public BackupFileCheckOnStartupTask(IServiceAwareStrategy serviceChangedAwareStrategy, String failoverDir) {
        this.serviceChangedAwareStrategy = serviceChangedAwareStrategy;
        this.failoverDir = failoverDir;
    }

    @Override
    public void run() {
        try {
            File cacheDir = new File(failoverDir);

            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                throw new IllegalStateException("failed to create cache dir: " + failoverDir);
            }

            File[] files = cacheDir.listFiles();
            if (files == null || files.length <= 0) {
                new DiskFileWriterTask(serviceChangedAwareStrategy, failoverDir).run();
            }
        } catch (Throwable e) {
            NAMING_LOGGER.error("[NA] failed to backup file on startup.", e);
        }
    }
}
