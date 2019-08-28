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

import com.alibaba.nacos.naming.misc.UtilsAndCommons;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.util.VersionUtil;

/**
 * @author nacos
 */
public class ClientInfo {

    public Version version;
    public ClientType type;

    public ClientInfo(String userAgent) {
        String versionStr = StringUtils.isEmpty(userAgent) ? StringUtils.EMPTY : userAgent;

        if (versionStr.startsWith(ClientTypeDescription.JAVA_CLIENT)) {
            type = ClientType.JAVA;

            versionStr = versionStr.substring(versionStr.indexOf(":v") + 2, versionStr.length());
            version = VersionUtil.parseVersion(versionStr);

            return;
        }

        if (versionStr.startsWith(ClientTypeDescription.DNSF_CLIENT)) {
            type = ClientType.DNS;

            versionStr = versionStr.substring(versionStr.indexOf(":v") + 2, versionStr.length());
            version = VersionUtil.parseVersion(versionStr);

            return;
        }

        if (versionStr.startsWith(ClientTypeDescription.C_CLIENT)) {
            type = ClientType.C;

            versionStr = versionStr.substring(versionStr.indexOf(":v") + 2, versionStr.length());
            version = VersionUtil.parseVersion(versionStr);

            return;
        }

        if (versionStr.startsWith(ClientTypeDescription.SDK_CLIENT)) {
            type = ClientType.JAVA_SDK;

            versionStr = versionStr.substring(versionStr.indexOf(":v") + 2, versionStr.length());
            version = VersionUtil.parseVersion(versionStr);

            return;
        }

        if (versionStr.startsWith(UtilsAndCommons.NACOS_SERVER_HEADER)) {
            type = ClientType.NACOS_SERVER;

            versionStr = versionStr.substring(versionStr.indexOf(":v") + 2, versionStr.length());
            version = VersionUtil.parseVersion(versionStr);

            return;
        }

        if (versionStr.startsWith(ClientTypeDescription.NGINX_CLIENT)) {
            type = ClientType.TENGINE;

            versionStr = versionStr.substring(versionStr.indexOf(":v") + 2, versionStr.length());
            version = VersionUtil.parseVersion(versionStr);

            return;
        }

        if (versionStr.startsWith(ClientTypeDescription.CPP_CLIENT)) {
            type = ClientType.C;

            versionStr = versionStr.substring(versionStr.indexOf(":v") + 2, versionStr.length());
            version = VersionUtil.parseVersion(versionStr);

            return;
        }

        if (versionStr.startsWith(ClientTypeDescription.GO_CLIENT)) {
            type = ClientType.GO;

            versionStr = versionStr.substring(versionStr.indexOf(":v") + 2, versionStr.length());
            version = VersionUtil.parseVersion(versionStr);

            return;
        }


        //we're not eager to implement other type yet
        this.type = ClientType.UNKNOWN;
        this.version = Version.unknownVersion();
    }

}
