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
package com.alibaba.nacos.client.naming.core.grpc;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingCommonEventSinks;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.api.naming.push.SubscribeMetadata;
import com.alibaba.nacos.client.naming.core.AbstractServiceAwareStrategy;
import com.alibaba.nacos.client.naming.core.builder.ServiceAwareStrategyBuilder;
import com.alibaba.nacos.client.naming.utils.NetUtils;
import com.alibaba.nacos.client.naming.utils.UtilAndComs;
import com.alibaba.nacos.client.utils.AppNameUtils;
import com.alibaba.nacos.client.utils.StringUtils;
import com.alibaba.nacos.core.remoting.channel.AbstractRemotingChannel;
import com.alibaba.nacos.core.remoting.event.RecyclableEvent;
import com.alibaba.nacos.core.remoting.event.filter.IEventReactiveFilter;
import com.alibaba.nacos.core.remoting.event.reactive.AsyncEventReactive;
import com.alibaba.nacos.core.remoting.event.reactive.EventLoopReactive;
import com.alibaba.nacos.core.remoting.grpc.manager.GrpcClientRemotingManager;
import com.alibaba.nacos.core.remoting.manager.IClientRemotingManager;
import com.alibaba.nacos.core.remoting.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * aware service changes based on gRPC
 *
 * @author pbting
 * @date 2019-09-03 9:41 AM
 */
public class GrpcServiceAwareStrategy extends AbstractServiceAwareStrategy {

    public static final String PUSH_PACKET_DOM_TYPE = "dom";
    public static final String PUSH_PACKET_DOM_SERVICE_KEY = "pushPacketDomServiceKey";
    public static final String PUSH_PACKET_DOM_DUMP = "dump";
    public static final String EVENT_CONTEXT_CHANNEL = "remotingChannel";
    private final ServiceInfo emptyServiceInfo = new ServiceInfo();

    private Map<String, AbstractRemotingChannel> remotingChannelMapping = new Hashtable();
    private volatile AbstractRemotingChannel remotingChannel;
    private ConcurrentMap<String, AtomicBoolean> serviceNameRequestStreamRegistry =
        new ConcurrentHashMap<String, AtomicBoolean>();

    private AsyncEventReactive asyncEventReactive =
        new EventLoopReactive(new DefaultEventExecutorGroup(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName(GrpcServiceAwareStrategy.class.getName());
                thread.setDaemon(false);
                return thread;
            }
        }));

    private final IClientRemotingManager clientRemotingManager = new GrpcClientRemotingManager();

    private GrpcPushReceiver grpcPushReceiver;

    @Override
    public ServiceInfo getServiceInfo(String serviceName, String clusters) {
        ServiceInfo result = null;
        try {
            result = super.getServiceInfo(serviceName, clusters);
            return result;
        } finally {
            SubscribeMetadata subscribeMetadata =
                new SubscribeMetadata(serverProxy.getNamespaceId(), serviceName,
                    clusters, UtilAndComs.VERSION,
                    NetUtils.localIP(), System.nanoTime(),
                    Constants.SERVICE_AWARE_STRATEGY_GRPC,
                    serverProxy.getNamespaceId(), AppNameUtils.getAppName());

            requestSubscribeStream(subscribeMetadata, false);
            long cacheMillis = (result != null ? result.getCacheMillis() : emptyServiceInfo.getCacheMillis());
            updateSubscribeDuration(subscribeMetadata, cacheMillis, ServiceInfo.getKey(serviceName, clusters));
        }
    }

    public void requestSubscribeStream(SubscribeMetadata subscribeMetadata, boolean isForce) {
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        AtomicBoolean resultBoolean = serviceNameRequestStreamRegistry.putIfAbsent(ServiceInfo.getKey(subscribeMetadata.getServiceName(), subscribeMetadata.getClusters()), atomicBoolean);
        if (resultBoolean == null) {
            resultBoolean = atomicBoolean;
        }
        // only send once
        if (!isForce && !resultBoolean.compareAndSet(false, true)) {
            subscribeMetadata.setSuccess(false);
            return;
        }

        subscribeMetadata.setPushType(Constants.SERVICE_AWARE_STRATEGY_GRPC);
        // send the subscribe remoting event
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setSink(NamingCommonEventSinks.SUBSCRIBE_SINK);
        builder.setPayload(ByteString.copyFrom(JSON.toJSONString(subscribeMetadata).getBytes(Charset.forName("utf-8"))));
        remotingChannel.requestStream(builder.build(), grpcPushReceiver);
    }

    public void updateSubscribeDuration(SubscribeMetadata subscribeMetadata, long cacheMillis, String pushKey) {
        if (!subscribeMetadata.isSuccess()) {
            return;
        }
        // subscribe duration for server
        RecyclableEvent recyclableEvent =
            new RecyclableEvent(this, subscribeMetadata,
                NamingCommonEventSinks.SUBSCRIBE_DURATION_SINK,
                (int) TimeUnit.MILLISECONDS.toSeconds(cacheMillis));
        recyclableEvent.setParameter(PUSH_PACKET_DOM_SERVICE_KEY, pushKey);
        recyclableEvent.setParameter(EVENT_CONTEXT_CHANNEL, remotingChannel);
        asyncEventReactive.reactive(recyclableEvent);
    }

    /**
     * 长连接推送的时候，发现客户端和服务的 checksum 不一样的时候，会主动拉一次。
     *
     * @param serviceName
     * @param clusters
     * @throws NacosException
     */
    @Override
    public void updateService(String serviceName, String clusters) throws NacosException {
        String result = serverProxy.queryList(serviceName, clusters, Constants.PORT_IDENTIFY_NNTS, false);
        if (StringUtils.isNotEmpty(result)) {
            processServiceAwareResult(result);
        }
    }

    @Override
    public void initServiceAwareStrategy(ServiceAwareStrategyBuilder.ServiceAwareStrategyConfig serviceChangedStrategyConfig) {
        // 1. init common state
        initCommonState(serviceChangedStrategyConfig);
        initFilters();
        initPipelineEventListener();
        try {
            // 2. 建立和 Server 之间的连接
            initRemotingChannel();
        } catch (NacosException e) {
            e.printStackTrace();
        }

        grpcPushReceiver = new GrpcPushReceiver(this);
    }

    protected void initRemotingChannel() throws NacosException {
        List<String> servers = serverProxy.getServers();
        int index = new Random(System.currentTimeMillis()).nextInt(servers.size());
        boolean isAllShutdown = true;
        for (int i = 0; i < servers.size(); i++) {
            String connectToServer = servers.get(index);
            String[] ipPort = connectToServer.split(UtilAndComs.SERVER_ADDR_IP_SPLITER);
            ipPort[1] = "28848";
            AbstractRemotingChannel remotingChannel = remotingChannelMapping.get(connectToServer);
            if (remotingChannel == null) {
                synchronized (remotingChannelMapping) {
                    if ((remotingChannel = remotingChannelMapping.get(connectToServer)) == null) {
                        remotingChannel =
                            clientRemotingManager.getRemotingChannelFactory()
                                .newRemotingChannel(ipPort[0] + UtilAndComs.SERVER_ADDR_IP_SPLITER + ipPort[1], "");
                        remotingChannelMapping.put(connectToServer, remotingChannel);
                    }
                }
            }
            ManagedChannel managedChannel = remotingChannel.getRawChannel();
            if (managedChannel.getState(true) != ConnectivityState.TRANSIENT_FAILURE) {
                this.remotingChannel = remotingChannel;
                isAllShutdown = false;
                break;
            }
            index = (index + 1) % servers.size();
        }

        if (isAllShutdown) {
            throw new NacosException(NacosException.SERVER_ERROR, "all of server[" + servers + "] initialize remoting channel fail.");
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        asyncEventReactive.destroy();
        for (AbstractRemotingChannel remotingChannel : remotingChannelMapping.values()) {
            remotingChannel.close();
        }
        this.remotingChannel.close();
        remotingChannelMapping.clear();
        serviceNameRequestStreamRegistry.clear();
    }

    public AbstractRemotingChannel getRemotingChannel() {
        return remotingChannel;
    }

    private void initFilters() {
        Collection<IEventReactiveFilter> filters = new LinkedList();
        filters.add(new RemotingActiveCheckFilter(this));
        asyncEventReactive.registerEventReactiveFilter(filters);
    }

    private void initPipelineEventListener() {
        asyncEventReactive.addLast(new SubscribeDurationEventListener(this));
    }
}
