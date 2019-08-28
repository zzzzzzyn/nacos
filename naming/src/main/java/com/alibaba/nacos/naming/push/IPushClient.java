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

import com.alibaba.nacos.naming.misc.SwitchDomain;

/**
 * @author pbting
 * @date 2019-08-28 9:45 AM
 */
public interface IPushClient {

    /**
     * get data source will use it to get some push data.
     *
     * @return
     */
    DataSource getDataSource();

    /**
     * Determine if the current push client is already in a zombie state
     *
     * @param switchDomain
     * @return
     */
    boolean zombie(SwitchDomain switchDomain);

    /**
     * refresh the push client of last query list
     */
    void refresh();

    /**
     * get address with ip and port
     *
     * @return
     */
    String getAddrStr();

    /**
     * get the ip with out port
     *
     * @return
     */
    String getIp();

    /**
     * get the port with out ip
     *
     * @return
     */
    int getPort();
}
