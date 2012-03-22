package org.whired.inspexi.tools;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.whired.inspexi.master.SessionListener;

public class NetTaskQueue {
	private final ExecutorService exe = Executors.newSingleThreadExecutor();
	private final SessionListener listener;
	private final ConcurrentLinkedQueue<Future<Boolean>> pendingTasks = new ConcurrentLinkedQueue<Future<Boolean>>();

	public NetTaskQueue(SessionListener listener) {
		this.listener = listener;
	}

	/**
	 * @param task
	 * @return {@code false} if a previous task failed to complete
	 */
	public boolean add(NetTask task) {
		Future<Boolean> pt = pendingTasks.peek();
		if (pt != null && pt.isDone()) {
			System.out.println("Task done");
			pendingTasks.poll();
			try {
				if (!pt.get()) {
					listener.sessionEnded("Task failed");
					return false;
				}
			}
			catch (Throwable t) {
				listener.sessionEnded(t.toString());
				return false;
			}
		}
		pendingTasks.add(exe.submit(task));
		System.out.println("Task submitted");
		return true;
	}
}
