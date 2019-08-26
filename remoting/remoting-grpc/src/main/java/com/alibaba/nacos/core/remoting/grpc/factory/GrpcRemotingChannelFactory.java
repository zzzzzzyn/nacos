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
package com.alibaba.nacos.core.remoting.grpc.factory;

import com.alibaba.nacos.core.remoting.channel.AbstractRemotingChannel;
import com.alibaba.nacos.core.remoting.channel.IRemotingChannelFactory;
import com.alibaba.nacos.core.remoting.grpc.GrpcRemotingChannel;
import com.alibaba.nacos.core.remoting.manager.IRemotingManager;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;

/**
 * @author pbting
 */
public class GrpcRemotingChannelFactory implements IRemotingChannelFactory {

    private IRemotingManager remotingManager;

    public GrpcRemotingChannelFactory(IRemotingManager remotingManager) {
        this.remotingManager = remotingManager;
    }

    @Override
    public AbstractRemotingChannel newRemotingChannel(String addressPort,
                                                      String clusterName) {
        // 初始化指定节点连接
        String[] addressPortArr = addressPort.split("[:]");
        NettyChannelBuilder nettyChannelBuilder = NettyChannelBuilder.forAddress(
            addressPortArr[0].trim(), Integer.valueOf(addressPortArr[1].trim()));

        ManagedChannel channel = nettyChannelBuilder.usePlaintext()
            .flowControlWindow(NettyChannelBuilder.DEFAULT_FLOW_CONTROL_WINDOW)
            .build();
        GrpcRemotingChannel remotingChannel = new GrpcRemotingChannel(channel, addressPort);
        remotingChannel.setIdentify(remotingManager.builderIdentify(addressPort + "@" + clusterName));

        return remotingChannel;
    }

}
