package com.alibaba.nacos.test.naming;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.naming.utils.NamingUtils;
import com.alibaba.nacos.common.utils.ConcurrentHashSet;
import com.alibaba.nacos.common.utils.ThreadUtils;
import com.alibaba.nacos.naming.core.expire.Lessor;
import com.alibaba.nacos.naming.healthcheck.RsInfo;
import org.openjdk.jmh.annotations.Benchmark;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeWheelExpireContainerTest {

	static final ScheduledExecutorService EXECUTOR_SERVICE = Executors
			.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() << 2);
	private static final Map<String, Map<String, Instance>> STORAGE = new ConcurrentHashMap<>();
	private static final Set<Instance> ALREADY_UN_HEALTH = new ConcurrentHashSet<>();

	private static final String CLUSTER_NAME = "DEFAULT_CLUSTER";

	private static final Lessor<Instance> CONTAINER = new Lessor<>(16,
			Duration.ofSeconds(1), instances -> {
		System.out.println("current unHealth instance number = " + instances.size());
		instances.stream().filter(Objects::nonNull).forEach(instance -> {
			try {
				final String serviceName = instance.getServiceName();
				final String datumKey = instance.getDatumKey();
				STORAGE.get(serviceName).remove(datumKey);
			} catch (Throwable ex) {
				System.err.println(ex.toString());
			}
		});
	});

	private static final Map<String, List<Instance>> slow = new ConcurrentHashMap<>();
	private static final Map<String, List<Instance>> normal = new ConcurrentHashMap<>();

	public static void main(String[] args) throws Exception {

		// create 100 service
		initData();

		// Create 1000 instances for each service, A total of 10w instances
		registerInstance();

		//		Options opt = new OptionsBuilder()
		//				// 导入要测试的类
		//				.include(TimeWheelExpireContainerTest.class.getSimpleName())
		//				// 预热5轮
		//				.warmupIterations(5)
		//				// 度量10轮
		//				.measurementIterations(10)
		//				.mode(Mode.Throughput)
		//				.forks(3)
		//				.build();
		//		new Runner(opt).run();

		System.out.println("slow instance = " + slow.values().stream().map(List::size).reduce(
				Integer::sum));
		System.out.println("normal instance = " + normal.values().stream().map(List::size).reduce(
				Integer::sum));

		test_object_expire();
	}

	public static void initData() {
		AtomicInteger count = new AtomicInteger(0);
		for (; ; ) {
			final String serviceName = NamingUtils
					.getGroupedName(NamingBase.randomDomainName(),
							Constants.DEFAULT_GROUP);
			STORAGE.computeIfAbsent(serviceName, key -> {
				Map<String, Instance> map = new ConcurrentHashMap<>();
				count.incrementAndGet();
				return map;
			});
			if (count.get() == 100) {
				return;
			}
		}
	}

	private static void registerInstance() {
		CountDownLatch latch = new CountDownLatch(STORAGE.keySet().size());
		final long startTime = System.currentTimeMillis();
		STORAGE.forEach((serviceName, map) -> {
			EXECUTOR_SERVICE.execute(() -> {
				for (int i = 0; i < 1000L; i++) {
					final String ip = getRandomIp();
					final int port = 12345;
					final Instance instance = Instance.builder()
							.withServiceName(serviceName)
							.withIp(ip).withPort(port)
							.withClusterName(CLUSTER_NAME)
							.withEphemeral(true).build();
					if (i % 7 == 0) {
						slow.computeIfAbsent(serviceName, s -> new ArrayList<>());
						slow.get(serviceName).add(instance);
					}
					else {
						normal.computeIfAbsent(serviceName, s -> new ArrayList<>());
						normal.get(serviceName).add(instance);
					}
					map.put(instance.getDatumKey(), instance);
					CONTAINER.addItem(instance);
					latch.countDown();
				}
			});
		});

		ThreadUtils.latchAwait(latch);
		System.out.println("Instance creation time : " + (System.currentTimeMillis() - startTime) + " Ms");
	}

	@Benchmark
	public static void test_object_expire() {
		normal.forEach(
				(serviceName, instances) -> instances.forEach(instance -> sendBeat(serviceName, instance, EXECUTOR_SERVICE,
						true)));
		//		slow.forEach((serviceName, instance) -> sendBeat(serviceName, instance, slowExecutor, false));

		while (ALREADY_UN_HEALTH.size() != slow.size()) {
			ThreadUtils.sleep(2_000L);
		}
	}

	private static void sendBeat(String serviceName, Instance instance,
			ScheduledExecutorService executorService, boolean isNormal) {
		final RsInfo rsInfo = RsInfo.builder().serviceName(serviceName)
				.cluster(instance.getClusterName()).ip(instance.getIp())
				.port(instance.getPort()).build();
		onBeat(rsInfo);
		long delay = 5;
		if (!isNormal) {
			delay += ThreadLocalRandom.current().nextInt(5);
		}
		executorService.schedule(
				() -> sendBeat(serviceName, instance, executorService, isNormal), delay,
				TimeUnit.SECONDS);
	}

	private static void onBeat(RsInfo rsInfo) {
		final String serviceName = rsInfo.getServiceName();
		final String ip = rsInfo.getIp();
		final int port = rsInfo.getPort();
		final String clusterName = rsInfo.getCluster();
		final String key = Instance.buildDatumKey(ip, port, clusterName);
		Map<String, Instance> map = STORAGE.get(serviceName);
		if (map.containsKey(key)) {
			map.get(key).setLastBeat(System.currentTimeMillis());
		}
		else {
			Instance instance = Instance.builder().withIp(ip).withPort(port)
					.withClusterName(clusterName)
					.withLastBeat(System.currentTimeMillis()).build();
			CONTAINER.addItem(instance);
			map.put(key, instance);
			System.err.println("The instance does not exist");
		}
	}

	// copy from https://blog.csdn.net/zhengxiongwei/article/details/78486146

	public static String getRandomIp() {

		// ip范围
		int[][] range = { { 607649792, 608174079 }, // 36.56.0.0-36.63.255.255
				{ 1038614528, 1039007743 }, // 61.232.0.0-61.237.255.255
				{ 1783627776, 1784676351 }, // 106.80.0.0-106.95.255.255
				{ 2035023872, 2035154943 }, // 121.76.0.0-121.77.255.255
				{ 2078801920, 2079064063 }, // 123.232.0.0-123.235.255.255
				{ -1950089216, -1948778497 }, // 139.196.0.0-139.215.255.255
				{ -1425539072, -1425014785 }, // 171.8.0.0-171.15.255.255
				{ -1236271104, -1235419137 }, // 182.80.0.0-182.92.255.255
				{ -770113536, -768606209 }, // 210.25.0.0-210.47.255.255
				{ -569376768, -564133889 }, // 222.16.0.0-222.95.255.255
		};

		Random random = ThreadLocalRandom.current();
		int index = random.nextInt(10);
		return num2ip(
				range[index][0] + random.nextInt(range[index][1] - range[index][0]));
	}

	/*
	 * 将十进制转换成IP地址
	 */
	public static String num2ip(int ip) {
		int[] b = new int[4];
		String ipStr = "";
		b[0] = (ip >> 24) & 0xff;
		b[1] = (ip >> 16) & 0xff;
		b[2] = (ip >> 8) & 0xff;
		b[3] = ip & 0xff;
		ipStr = b[0] + "." + b[1] + "." + b[2] + "." + b[3];

		return ipStr;
	}

}