package org.whired.inspexi.tools;

import java.io.IOException;

/**
 * A network task
 * 
 * @author Whired
 */
public abstract class NetTask {

	/**
	 * The name of this task
	 */
	private final String name;

	/**
	 * Creates a new task with the specified name
	 * 
	 * @param name the name of the task
	 */
	public NetTask(String name) {
		this.name = name;
	}

	/**
	 * Runs this task
	 * 
	 * @throws IOException when a network error occurs
	 */
	public abstract void run() throws IOException;

	@Override
	public String toString() {
		return this.getClass().getName() + " " + name;
	}
}
