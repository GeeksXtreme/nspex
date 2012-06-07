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
	/** The map that counts the number of tasks per communicable */
	private static final ConcurrentHashMap<Communicable, Integer> submitters = new ConcurrentHashMap<Communicable, Integer>();

	/**
	 * Submits a task to the processor
	 * @param comm the communicable that is submitting the task
	 * @param task the task to submit
	 */
	public static void submit(final Communicable comm, final Runnable task) {
		Integer i = submitters.get(comm);
		if (i == null || i < MAX_TASKS_PER_COMMUNICABLE) {
			submitters.put(comm, i != null ? ++i : 1);
			executor.execute(new Runnable() {
				@Override
				public void run() {
					// Don't waste time on expired communicables
					if (comm.isConnected()) {
						task.run();
						Integer i = submitters.get(comm);
						if (i != null) {
							if (i == 1) {
								submitters.remove(comm);
							}
							else {
								submitters.put(comm, --i);
							}
						}
					}
					else {
						submitters.remove(comm);
					}
				}
			});
		}
	}
}
