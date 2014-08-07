package net.indiespot.script.interp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class Scheduler {
	private static ExecFrame root(ExecFrame frame) {
		if(frame.callsite == null)
			throw new IllegalStateException();
		while (frame.callsite.callsite != null)
			frame = frame.callsite;
		return frame;
	}

	//

	public static void yield() {
		throw new UnsupportedOperationException();
	}

	static void signalYield(ExecFrame frame) {
		thread_local.get().executeLater(root(frame), 0);
	}

	//

	public static void sleep(int millis) {
		throw new UnsupportedOperationException();
	}

	static void signalSleep(ExecFrame frame, int millis) {
		thread_local.get().executeLater(root(frame), millis);
	}

	//

	public static void suspend() {
		throw new UnsupportedOperationException();
	}

	static void signalSuspend(ExecFrame frame) {
		// no-op
	}

	//

	public void start(ExecFrame frame) {
		this.executeLater(frame, 0);
	}

	static void signalResume(ExecFrame frame) {
		thread_local.get().executeLater(root(frame), 0);
	}

	static void signalTerminated(ExecFrame callsite) {
		if(callsite.terminationHandler != null) {
			callsite.terminationHandler.onTermination(callsite);
		}
	}

	public static void echo(Object msg) {
		throw new UnsupportedOperationException();
	}

	public static void echo(int msg) {
		throw new UnsupportedOperationException();
	}

	public static void echo(float msg) {
		throw new UnsupportedOperationException();
	}

	static void signalEcho(ExecFrame frame, Object msg) {
		System.out.println("echo: " + msg);
	}

	private static final ThreadLocal<Scheduler> thread_local = new ThreadLocal<Scheduler>();

	/**
	 * 
	 */

	private final TimeSortedQueue<Runnable> eventQueue;
	private final List<Runnable> execBatch;

	public Scheduler() {
		eventQueue = new TimeSortedQueue<>();
		execBatch = new ArrayList<>();
	}

	private static long time() {
		return System.currentTimeMillis();
	}

	public void tick() {
		long now = time();

		for(Runnable task; (task = eventQueue.poll(now)) != null;) {
			execBatch.add(task);
		}

		thread_local.set(this);
		{
			for(int i = 0, len = execBatch.size(); i < len; i++)
				execBatch.get(i).run();
			execBatch.clear();
		}
		thread_local.set(null);
	}

	public void executeLater(Runnable task, int delay) {
		this.executeAt(task, time() + delay);
	}

	public void executeAt(Runnable task, long time) {
		eventQueue.insert(time, task);
	}
}

class TimeSortedQueue<T> {
	private final PriorityQueue<Slot<T>> queue;

	public TimeSortedQueue() {
		queue = new PriorityQueue<Slot<T>>(11, new TimeSlotComparator<T>());
	}

	public int size() {
		return queue.size();
	}

	public void clear() {
		queue.clear();
	}

	public void insert(long time, T item) {
		queue.add(new Slot<T>(time, item));
	}

	public T poll(long now) {
		Slot<T> peeked = queue.peek();
		if(peeked == null || peeked.time > now)
			return null;
		return queue.poll().item;
	}

	// --

	private static class TimeSlotComparator<T> implements Comparator<Slot<T>> {
		@Override
		public int compare(Slot<T> o1, Slot<T> o2) {
			int cmp = Long.signum(o1.time - o2.time);
			return (cmp == 0) ? -1 : cmp;
		}
	}

	private static class Slot<Q> {
		private final long time;
		private final Q item;

		public Slot(long time, Q item) {
			this.time = time;
			this.item = item;
		}
	}
}
