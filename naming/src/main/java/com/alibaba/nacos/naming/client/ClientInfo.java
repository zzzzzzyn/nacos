/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
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

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.util.VersionUtil;

import java.util.stream.Stream;

/**
 * @author nacos
 */
public class ClientInfo {

    public Version version = Version.unknownVersion();
    public ClientType type = ClientType.UNKNOWN;

    public ClientInfo(String userAgent) {
        final String versionStr = StringUtils.isEmpty(userAgent) ? StringUtils.EMPTY : userAgent;

        Stream.of(ClientType.values()).filter(clientType ->
            versionStr.startsWith(clientType.clientName)
        ).forEach(clientType -> {
            type = clientType;
            version = VersionUtil.parseVersion(versionStr.substring(versionStr.indexOf(":v") + 2));
        });
    }
}
