package org.whired.inspexi.tools;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A queue of tasks to execute
 * @author Whired
 */
public class NetTaskQueue {
	/** The underlying queue */
	private final LinkedBlockingDeque<NetTask> pendingTasks = new LinkedBlockingDeque<NetTask>();
	/** The listener to notify when a task fails */
	private final SessionListener listener;
	/** The max number of threads in a thread pool */
	private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() * 10;
	/** Executes tasks */
	private static final ExecutorService exec = Executors.newFixedThreadPool(MAX_THREADS);

	/**
	 * Creates a new queue with the specified listener Automatically handles any connection exceptions and closes the appropriate socket if applicable
	 * @param listener the listener to notify if a task fails
	 */
	public NetTaskQueue(final SessionListener listener) {
		this.listener = listener;
		for (int i = 0; i < MAX_THREADS; i++) {
			exec.submit(task);
		}
		// start();
		System.out.println("Submitted " + MAX_THREADS + " tasks.");
	}

	/** Takes and runs tasks in the queue */
	private final Runnable task = new Runnable() {
		@Override
		public void run() {
			while (true) {
				NetTask t = null;
				try {
					t = pendingTasks.take();
					System.out.println("Executing task " + t.toString());
					t.run(new DataInputStream(t.socket.getInputStream()), new DataOutputStream(t.socket.getOutputStream()));
				}
				catch (final IOException e) {
					if (t != null) {
						t.onFail();
					}
					if (t.socket != null) {
						try {
							t.socket.close();
						}
						catch (final IOException e1) {
						} // Not a big deal, just trying to clean up
					}
					listener.sessionEnded(e.toString());
				}
				catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};

	/**
	 * Adds a task to this queue
	 * @param task the task to execute
	 */
	public synchronized void add(final NetTask task) {
		pendingTasks.offer(task);
	}

	public Runnable getTask() {
		return task;
	}
}
