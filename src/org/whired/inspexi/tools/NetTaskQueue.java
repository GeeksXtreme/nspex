package org.whired.inspexi.tools;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A queue of tasks to execute
 * @author Whired
 */
public class NetTaskQueue {
	/** The underlying queue */
	private static final LinkedBlockingDeque<NetTask> pendingTasks = new LinkedBlockingDeque<NetTask>();
	/** The max number of threads in a thread pool */
	private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() * 2;
	/** Executes tasks */
	private static final ExecutorService exec = Executors.newFixedThreadPool(MAX_THREADS);

	private final static class TaskExecutor implements Runnable {
		LinkedBlockingDeque<NetTask> tasks = new LinkedBlockingDeque<NetTask>();
		Set<Socket> sockets = Collections.synchronizedSet(new HashSet<Socket>());

		@Override
		public void run() {
			while (true) {
				NetTask t = null;
				try {
					t = tasks.take();
					t.run(new DataInputStream(t.socket.getInputStream()), new DataOutputStream(t.socket.getOutputStream()));
				}
				catch (final Throwable e) {
					if (e instanceof SocketTimeoutException && !t.timeoutFatal) {
						tasks.offer(t);
					}
					else {
						e.printStackTrace();
						if (t != null) {
							t.onFail();
						}
						if (t.socket != null) {
							try {
								sockets.remove(t.socket);
								t.socket.close();
							}
							catch (final IOException e1) {
							}
						}
						if (t.listener != null) {
							t.listener.sessionEnded(e.toString(), e);
						}
					}
				}
			}
		}

		public boolean isControlling(Socket s) {
			return sockets.contains(s);
		}

		public void submit(NetTask task) {
			sockets.add(task.socket);
			tasks.offer(task);
		}

		public int getSubmitterCount() {
			return sockets.size();
		}
	}

	private static final HashSet<TaskExecutor> taskExecutors = new HashSet<TaskExecutor>();

	private static final Runnable taskAcceptor = new Runnable() {
		@Override
		public void run() {
			while (true) {
				try {
					System.out.println(Thread.currentThread().getName() + " waiting for task..");
					NetTask t = pendingTasks.take();
					System.out.println(Thread.currentThread().getName() + " accepting task: " + t);
					TaskExecutor lowLoad = null;
					boolean match = false;
					for (TaskExecutor te : taskExecutors) {
						// Delegate task to proper executor
						if (te.isControlling(t.socket)) {
							System.out.println("Found controller match");
							te.submit(t);
							match = true;
							break;
						}
						if (lowLoad == null || te.getSubmitterCount() < lowLoad.getSubmitterCount()) {
							lowLoad = te;
						}
					}
					if (!match) {
						System.out.println("No match, adding");
						lowLoad.submit(t);
					}
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	static {
		exec.submit(taskAcceptor);
		for (int i = 1; i < MAX_THREADS; i++) {
			TaskExecutor te = new TaskExecutor();
			exec.submit(te);
			taskExecutors.add(te);
		}
		System.out.println("Submitted " + MAX_THREADS + " task executors.");
	}

	/**
	 * Adds a task to this queue
	 * @param task the task to execute
	 */
	public static void add(final NetTask task) {
		System.out.println("Submitting " + task + " to task acceptor");
		pendingTasks.offer(task);
	}
}
