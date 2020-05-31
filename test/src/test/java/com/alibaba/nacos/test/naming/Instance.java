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

package com.alibaba.nacos.test.naming;

import com.alibaba.nacos.naming.core.expire.Overdue;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class Instance extends com.alibaba.nacos.naming.core.Instance implements Overdue {

	public static InstanceBuilder builder() {
		return new InstanceBuilder();
	}

	@Override
	public long remainingMs() {
		return getLastBeat() + getInstanceHeartBeatTimeOut() - System.currentTimeMillis();
	}

	public static final class InstanceBuilder {
		private String ip;
		private int port;
		private double weight = 1.0D;
		private boolean healthy = true;
		private boolean enabled = true;
		private boolean ephemeral = true;
		private volatile long lastBeat = System.currentTimeMillis();
		private String clusterName;
		private volatile boolean mockValid = false;
		private volatile boolean marked = false;
		private String serviceName;
		private String tenant;
		private String app;
		private Map<String, String> metadata = new HashMap<String, String>();

		private InstanceBuilder() {
		}

		public InstanceBuilder withIp(String ip) {
			this.ip = ip;
			return this;
		}

		public InstanceBuilder withPort(int port) {
			this.port = port;
			return this;
		}

		public InstanceBuilder withWeight(double weight) {
			this.weight = weight;
			return this;
		}

		public InstanceBuilder withHealthy(boolean healthy) {
			this.healthy = healthy;
			return this;
		}

		public InstanceBuilder withEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public InstanceBuilder withEphemeral(boolean ephemeral) {
			this.ephemeral = ephemeral;
			return this;
		}

		public InstanceBuilder withLastBeat(long lastBeat) {
			this.lastBeat = lastBeat;
			return this;
		}

		public InstanceBuilder withClusterName(String clusterName) {
			this.clusterName = clusterName;
			return this;
		}

		public InstanceBuilder withMockValid(boolean mockValid) {
			this.mockValid = mockValid;
			return this;
		}

		public InstanceBuilder withMarked(boolean marked) {
			this.marked = marked;
			return this;
		}

		public InstanceBuilder withServiceName(String serviceName) {
			this.serviceName = serviceName;
			return this;
		}

		public InstanceBuilder withTenant(String tenant) {
			this.tenant = tenant;
			return this;
		}

		public InstanceBuilder withApp(String app) {
			this.app = app;
			return this;
		}

		public InstanceBuilder withMetadata(Map<String, String> metadata) {
			this.metadata = metadata;
			return this;
		}

		public Instance build() {
			Instance instance = new Instance();
			instance.setIp(ip);
			instance.setPort(port);
			instance.setWeight(weight);
			instance.setHealthy(healthy);
			instance.setEnabled(enabled);
			instance.setEphemeral(ephemeral);
			instance.setLastBeat(lastBeat);
			instance.setClusterName(clusterName);
			instance.setMockValid(mockValid);
			instance.setMarked(marked);
			instance.setServiceName(serviceName);
			instance.setTenant(tenant);
			instance.setApp(app);
			instance.setMetadata(metadata);
			return instance;
		}
	}
}
