package org.whired.inspexi.tools;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;


/**
 * A queue of tasks to execute
 * 
 * @author Whired
 */
public class NetTaskQueue {
	/**
	 * The underlying queue
	 */
	private final LinkedBlockingDeque<NetTask> pendingTasks = new LinkedBlockingDeque<NetTask>();
	/**
	 * Whether or not this queue is accepting tasks
	 */
	private boolean acceptingTasks = true;
	/**
	 * The listener to notify when a task fails
	 */
	private final SessionListener listener;

	/**
	 * Creates a new queue with the specified listener
	 * 
	 * @param listener the listener to notify if a task fails
	 */
	public NetTaskQueue(SessionListener listener) {
		this.listener = listener;
		start();
	}

	/**
	 * Gets whether or not this queue is accepting tasks
	 * 
	 * @return {@code true} if it is, otherwise {@code false}
	 */
	public synchronized boolean isAcceptingTasks() {
		return acceptingTasks;
	}

	/**
	 * Notifies this queue that it should accept new tasks
	 */
	public synchronized void acceptTasks() {
		this.acceptingTasks = true;
	}

	/**
	 * Prepares the queue and starts executing tasks on a new, self-contained thread
	 */
	private void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						pendingTasks.take().run();
					}
					catch (IOException e) {
						synchronized (NetTaskQueue.this) {
							acceptingTasks = false;
						}
						listener.sessionEnded(e.toString());
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	/**
	 * Adds a task to this queue
	 * 
	 * @param task the task to execute
	 * @return {@code true} if the task was added, otherwise {@code false}
	 */
	public synchronized boolean add(NetTask task) {
		if (acceptingTasks) {
			pendingTasks.offer(task);
			return true;
		}
		else {
			return false;
		}
	}
}
