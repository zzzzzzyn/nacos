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

import com.alibaba.nacos.client.naming.cache.ConcurrentDiskUtil;
import com.alibaba.nacos.client.naming.cache.DiskCache;
import com.alibaba.nacos.client.naming.utils.UtilAndComs;
import com.alibaba.nacos.client.utils.StringUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;

/**
 * @author pbting
 * @date 2019-08-30 1:08 PM
 */
class SwitchRefresherTask implements Runnable {

    private FailoverReactor failoverReactor;
    private long lastModifiedMillis = 0L;

    public SwitchRefresherTask(FailoverReactor failoverReactor) {
        this.failoverReactor = failoverReactor;
    }

    @Override
    public void run() {
        // 1. check is runnable or not
        long lastModified = checkRunnable();
        if (lastModified < 0) {
            return;
        }
        lastModifiedMillis = lastModified;

        // 2. get the failover content
        String failoverContent = getFailoverContent();
        if (StringUtils.isEmpty(failoverContent)) {
            failoverReactor.putSwitchParam("failover-mode", "false");
            return;
        }

        // 3. process failover content
        processFailoverContent(failoverContent);
    }

    /**
     * if can runnable then return the last modified or -1
     *
     * @return
     */
    private long checkRunnable() {
        final File switchFile = new File(failoverReactor.getFailoverDir() + UtilAndComs.FAILOVER_SWITCH);
        if (!switchFile.exists()) {
            failoverReactor.putSwitchParam("failover-mode", "false");
            NAMING_LOGGER.debug("failover switch is not found, " + switchFile.getName());
            return -1;
        }

        long modified = switchFile.lastModified();
        if (lastModifiedMillis >= modified) {
            return -1;
        }

        return modified;
    }

    /**
     * get failover content
     *
     * @return
     */
    private String getFailoverContent() {
        String failoverContent = StringUtils.EMPTY;
        try {
            failoverContent = ConcurrentDiskUtil.getFileContent(failoverReactor.getFailoverDir() + UtilAndComs.FAILOVER_SWITCH,
                Charset.defaultCharset().toString());
        } catch (Throwable e) {
            NAMING_LOGGER.error("[NA] failed to read failover switch.", e);
        }

        return failoverContent;
    }

    /**
     * process the failover content
     *
     * @param failoverContent
     */
    private void processFailoverContent(String failoverContent) {
        List<String> lines = Arrays.asList(failoverContent.split(DiskCache.getLineSeparator()));
        for (String line : lines) {
            String line1 = line.trim();
            if ("1".equals(line1)) {
                failoverReactor.putSwitchParam("failover-mode", "true");
                NAMING_LOGGER.info("failover-mode is on");
                new FailoverFileReader(failoverReactor).run();
            } else if ("0".equals(line1)) {
                failoverReactor.putSwitchParam("failover-mode", "false");
                NAMING_LOGGER.info("failover-mode is off");
            }
        }
    }
}
