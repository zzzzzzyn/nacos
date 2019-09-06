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

import com.alibaba.nacos.api.naming.push.SubscribeMetadata;
import com.alibaba.nacos.api.naming.utils.NamingUtils;
import com.alibaba.nacos.naming.misc.SwitchDomain;

/**
 * @author pbting
 * @date 2019-08-28 9:46 AM
 */
public abstract class AbstractPushClient<T> implements IPushClient {
    private SubscribeMetadata subscribeMetadata;
    protected long lastRefTime = System.currentTimeMillis();
    protected DataSource dataSource;
    protected T pusher;

    /**
     * the Necessary structural parameters
     *
     * @param subscribeMetadata
     * @param dataSource
     */
    public AbstractPushClient(SubscribeMetadata subscribeMetadata,
                              DataSource dataSource, T pusher) {
        this.subscribeMetadata = subscribeMetadata;
        this.dataSource = dataSource;
        this.pusher = pusher;
    }

    @Override
    public SubscribeMetadata getSubscribeMetadata() {
        return subscribeMetadata;
    }

    @Override
    public String getAddrStr() {
        return subscribeMetadata.getAddrStr();
    }

    @Override
    public String getIp() {
        return subscribeMetadata.getClientIp();
    }

    @Override
    public long getPort() {
        return subscribeMetadata.getPort();
    }

    public T getPusher() {
        return pusher;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean zombie(SwitchDomain switchDomain) {
        return System.currentTimeMillis() - lastRefTime > switchDomain.getPushCacheMillis(subscribeMetadata.getServiceName());
    }

    @Override
    public void refresh() {
        lastRefTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return NamingUtils.getPushClientKey(subscribeMetadata);
    }
}
