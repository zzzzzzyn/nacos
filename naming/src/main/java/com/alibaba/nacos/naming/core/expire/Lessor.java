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

package com.alibaba.nacos.naming.core.expire;

import com.alibaba.nacos.common.executor.NameThreadFactory;
import com.alibaba.nacos.common.utils.ConvertUtils;
import com.alibaba.nacos.common.utils.Objects;
import io.grpc.netty.shaded.io.netty.util.internal.PlatformDependent;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;

/**
 * Refer to the data expiration algorithm implemented by Netty time wheel
 * {@link io.grpc.netty.shaded.io.netty.util.HashedWheelTimer}
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class Lessor<T extends Overdue> {

	private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();
	private static final AtomicIntegerFieldUpdater<Lessor> WORKER_STATE_UPDATER =
			AtomicIntegerFieldUpdater.newUpdater(Lessor.class, "workerState");

	private final Thread workerThread;
	private final Worker worker = new Worker();

	public static final int WORKER_STATE_INIT = 0;
	public static final int WORKER_STATE_STARTED = 1;
	public static final int WORKER_STATE_SHUTDOWN = 2;
	@SuppressWarnings({ "unused", "FieldMayBeFinal" })
	private volatile int workerState; // 0 - init, 1 - started, 2 - shut down

	private final long tickDuration;
	private final Slot[] wheel;
	private final int mask;
	private final CountDownLatch startTimeInitialized = new CountDownLatch(1);
	private final Queue<OverdueWrapper> timeouts = PlatformDependent.newMpscQueue();
	private final Consumer<Collection<T>> consumer;
	private final Set<T> alreadyExpire = new HashSet<>();

	private volatile long startTime;

	public Lessor(final int slotNum, final Duration duration, final Consumer<Collection<T>> consumer) {
		this.tickDuration = duration.toNanos();
		this.wheel = createWheel(ConvertUtils.normalizeToPower2(slotNum));
		this.mask = wheel.length - 1;
		ThreadFactory threadFactory = new NameThreadFactory(
				"nacos.core.item.expire.wheel");
		this.workerThread = threadFactory.newThread(worker);
		this.consumer = consumer;
	}

	private Slot[] createWheel(final int slotNum) {
		Slot[] wheel = new Lessor.Slot[slotNum];
		for (int i = 0; i < slotNum; i ++) {
			wheel[i] = new Slot();
		}
		return wheel;
	}

	/**
	 * Starts the background thread explicitly.  The background thread will
	 * start automatically on demand even if you did not call this method.
	 *
	 * @throws IllegalStateException if this timer has been
	 *                               {@linkplain #stop() stopped} already
	 */
	public void start() {
		switch (WORKER_STATE_UPDATER.get(this)) {
		case WORKER_STATE_INIT:
			if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
				workerThread.start();
			}
			break;
		case WORKER_STATE_STARTED:
			break;
		case WORKER_STATE_SHUTDOWN:
			throw new IllegalStateException("cannot be started once stopped");
		default:
			throw new Error("Invalid WorkerState");
		}

		// Wait until the startTime is initialized by the worker.
		while (startTime == 0) {
			try {
				startTimeInitialized.await();
			} catch (InterruptedException ignore) {
				// Ignore - it will be ready very soon.
			}
		}
	}

	public void stop() {
		if (Thread.currentThread() == workerThread) {
			throw new IllegalStateException(
					Lessor.class.getSimpleName() +
							".stop() cannot be called from " +
							Lessor.class.getSimpleName());
		}

		if (!WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
			// workerState can be 0 or 2 at this moment - let it always be 2.
			if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) != WORKER_STATE_SHUTDOWN) {
				INSTANCE_COUNTER.decrementAndGet();
			}
			return;
		}

		try {
			while (workerThread.isAlive()) {
				workerThread.interrupt();
				try {
					workerThread.join(100);
				} catch (InterruptedException ignored) {
					Thread.currentThread().interrupt();
				}
			}
		} finally {
			INSTANCE_COUNTER.decrementAndGet();
		}
	}

	public void addItem(T overdue) {

		start();

		final long delay = overdue.remainingNs();
		Objects.requireNonNull(overdue, "overdue");

		// Add the timeout to the timeout queue which will be processed on the next tick.
		// During processing all the queued HashedWheelTimeouts will be added to the correct HashedWheelBucket.
		long deadline = System.nanoTime() + delay - startTime;

		// Guard against overflow.
		if (delay > 0 && deadline < 0) {
			deadline = Long.MAX_VALUE;
		}
		OverdueWrapper wrapper = new OverdueWrapper(overdue, deadline);
		timeouts.add(wrapper);
	}

	private class Worker implements Runnable {

		private long tick;

		@Override
		public void run() {
			// Initialize the startTime.
			startTime = System.nanoTime();
			if (startTime == 0) {
				// We use 0 as an indicator for the uninitialized value here, so make sure it's not 0 when initialized.
				startTime = 1;
			}

			// Notify the other threads waiting for the initialization at start().
			startTimeInitialized.countDown();

			do {
				final long deadline = waitForNextTick();
				if (deadline > 0) {
					int idx = (int) (tick & mask);
					Slot bucket = wheel[idx];
					transferTimeoutsToBuckets();
					bucket.expireTimeouts();
					if (!alreadyExpire.isEmpty()) {
						consumer.accept(alreadyExpire);
					}
					alreadyExpire.clear();
					tick++;
				}
				System.out.println("spend time is [" + (System.nanoTime() - startTime - deadline) / 1000000 + "] ms");
			} while (WORKER_STATE_UPDATER.get(Lessor.this) == WORKER_STATE_STARTED);
		}

		private void transferTimeoutsToBuckets() {
			// transfer only max. 100000 overdue per tick to prevent a thread to stale the workerThread when it just
			// adds new timeouts in a loop.
			for (int i = 0; i < 100_000L; i++) {
				OverdueWrapper timeout = timeouts.poll();
				if (timeout == null) {
					// all processed
					break;
				}

				if (timeout.isCancelled) {
					continue;
				}

				long calculated = timeout.deadline / tickDuration;
				timeout.remainingRounds = (calculated - tick) / wheel.length;

				final long ticks = Math.max(calculated, tick); // Ensure we don't schedule for past.
				int stopIndex = (int) (ticks & mask);

				Slot bucket = wheel[stopIndex];
				bucket.addTimeout(timeout);
			}
		}

		/**
		 * calculate goal nanoTime from startTime and current tick number,
		 * then wait until that goal has been reached.
		 * @return Long.MIN_VALUE if received a shutdown request,
		 * current time otherwise (with Long.MIN_VALUE changed by +1)
		 */
		private long waitForNextTick() {
			long deadline = tickDuration * (tick + 1);

			for (; ; ) {
				final long currentTime = System.nanoTime() - startTime;
				long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;

				if (sleepTimeMs <= 0) {
					if (currentTime == Long.MIN_VALUE) {
						return -Long.MAX_VALUE;
					}
					else {
						return currentTime;
					}
				}

				// Check if we run on windows, as if thats the case we will need
				// to round the sleepTime as workaround for a bug that only affect
				// the JVM if it runs on windows.
				//
				// See https://github.com/netty/netty/issues/356
				if (PlatformDependent.isWindows()) {
					sleepTimeMs = sleepTimeMs / 10 * 10;
				}

				try {
					Thread.sleep(sleepTimeMs);
				}
				catch (InterruptedException ignored) {
					if (WORKER_STATE_UPDATER.get(Lessor.this) == WORKER_STATE_SHUTDOWN) {
						return Long.MIN_VALUE;
					}
				}
			}
		}

		public long getTick() {
			return tick;
		}
	}

	private class OverdueWrapper implements Overdue {

		long remainingRounds;
		private final long deadline;

		private OverdueWrapper prev;
		private OverdueWrapper next;
		private final T target;
		boolean isCancelled = false;

		/** The bucket to which the timeout was added */
		private Slot slot;

		public OverdueWrapper(T target, final long deadline) {
			this.target = target;
			this.deadline = deadline;
		}

		@Override
		public long remainingMs() {
			return target.remainingMs();
		}

		@Override
		public long remainingNs() {
			return target.remainingNs();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			OverdueWrapper that = (OverdueWrapper) o;
			return java.util.Objects.equals(target, that.target);
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(target);
		}

		public void setCancelled(boolean cancelled) {
			isCancelled = cancelled;
		}
	}

	private class Slot {
		// Used for the linked-list datastructure
		private OverdueWrapper head;
		private OverdueWrapper tail;

		/**
		 * Add {@link OverdueWrapper} to this bucket.
		 */
		public void addTimeout(OverdueWrapper timeout) {
			assert timeout.slot == null;
			timeout.slot = this;
			if (head == null) {
				head = tail = timeout;
			} else {
				tail.next = timeout;
				timeout.prev = tail;
				tail = timeout;
			}
		}

		/**
		 * Expire all {@link OverdueWrapper}s for the given {@code deadline}.
		 */
		public void expireTimeouts() {
			OverdueWrapper timeout = head;

			// process all timeouts
			while (timeout != null) {
				OverdueWrapper next = timeout.next;
				if (timeout.remainingRounds <= 0) {
					next = remove(timeout);
					if (timeout.remainingMs() <= 0) {
						alreadyExpire.add(timeout.target);
					} else {
						addItem(timeout.target);
					}
				} else {
					timeout.remainingRounds --;
				}
				timeout = next;
			}
		}

		public OverdueWrapper remove(OverdueWrapper timeout) {
			OverdueWrapper next = timeout.next;
			// remove timeout that was either processed or cancelled by updating the linked-list
			if (timeout.prev != null) {
				timeout.prev.next = next;
			}
			if (timeout.next != null) {
				timeout.next.prev = timeout.prev;
			}

			if (timeout == head) {
				// if timeout is also the tail we need to adjust the entry too
				if (timeout == tail) {
					tail = null;
					head = null;
				} else {
					head = next;
				}
			} else if (timeout == tail) {
				// if the timeout is the tail modify the tail to be the prev node.
				tail = timeout.prev;
			}
			// null out prev, next and bucket to allow for GC.
			timeout.prev = null;
			timeout.next = null;
			timeout.slot = null;
			return next;
		}
	}
}
