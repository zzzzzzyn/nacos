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
package com.alibaba.nacos.naming.client;

/**
 * @author pbting
 * @date 2019-08-28 2:07 PM
 */
public enum ClientType {
    /**
     * Go client type
     */
    GO(ClientTypeDescription.GO_CLIENT),
    /**
     * Java client type
     */
    JAVA(ClientTypeDescription.JAVA_CLIENT),
    /**
     * C client type
     */
    C(ClientTypeDescription.C_CLIENT),
    /**
     * php client type
     */
    PHP(ClientTypeDescription.PHP_CLIENT),
    /**
     * dns-f client type
     */
    DNS(ClientTypeDescription.DNSF_CLIENT),
    /**
     * nginx client type
     */
    TENGINE(ClientTypeDescription.NGINX_CLIENT),
    /**
     * sdk client type
     */
    JAVA_SDK(ClientTypeDescription.SDK_CLIENT),
    /**
     * Server notify each other
     */
    NACOS_SERVER(ClientTypeDescription.NACOS_SERVER_HEADER),
    /**
     * Unknown client type
     */
    UNKNOWN(ClientTypeDescription.UNKONW_CLIENT);

    String clientName;

    ClientType(String clientName) {

        this.clientName = clientName;
    }
}
