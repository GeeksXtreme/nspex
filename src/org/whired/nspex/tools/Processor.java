package org.whired.nspex.tools;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.whired.nspex.net.Communicable;

/**
 * Uses a fixed thread pool to execute tasks asynchronously
 * @author Whired
 */
public class Processor {
	/** The maximum number of threads in the pool */
	public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() * 4;
	/** The maximum number of tasks per communicable */
	private static final int MAX_TASKS_PER_COMMUNICABLE = 5;
	/** The executor that runs tasks */
	private static final Executor executor = Executors.newFixedThreadPool(MAX_THREADS);
	/** The map that tracks the number of tasks per communicable */
	private static final ConcurrentHashMap<Communicable, Integer> submitters = new ConcurrentHashMap<Communicable, Integer>();

	/**
	 * Submits a task to the processor
	 * @param comm the communicable that is submitting the task
	 * @param task the task to submit
	 */
	public static void submit(final Communicable comm, final Runnable task) {
		Integer taskCount = submitters.get(comm);
		if (taskCount == null || taskCount < MAX_TASKS_PER_COMMUNICABLE) {
			// Not tracked or limit not reached, track and increment count
			submitters.put(comm, taskCount != null ? ++taskCount : 1);
			executor.execute(new Runnable() {
				@Override
				public void run() {
					// Ensure they are even still connected
					if (comm.isConnected()) {
						task.run();
						Integer taskCount = submitters.get(comm);
						if (taskCount != null) {
							if (taskCount == 1) {
								// This is their last task, stop tracking
								submitters.remove(comm);
							}
							else {
								// Not the last task, decrement count but keep tracking
								submitters.put(comm, --taskCount);
							}
						}
					}
					else {
						// They are no longer connected, so just remove them
						submitters.remove(comm);
					}
				}
			});
		}
	}
}
