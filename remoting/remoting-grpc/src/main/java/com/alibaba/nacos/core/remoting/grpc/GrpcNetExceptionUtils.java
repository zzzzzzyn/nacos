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
package com.alibaba.nacos.core.remoting.grpc;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;

/**
 * @author pbting
 */
public class GrpcNetExceptionUtils {

    public static boolean isNetUnavailable(Exception e, ManagedChannel managedChannel) {
        if (managedChannel.getState(true) == ConnectivityState.TRANSIENT_FAILURE) {
            return true;
        }

        if (e.getMessage() == null) {
            return false;
        }

        if (e.getMessage().startsWith("UNAVAILABLE")
            && e.getMessage().indexOf("io exception") != -1) {
            return true;
        }

        if (e.getMessage().indexOf("Connection refused") != -1) {
            return true;
        }

        if (e.getMessage().indexOf("Connection refused") != -1) {
            return true;
        }

        return false;
    }
}
