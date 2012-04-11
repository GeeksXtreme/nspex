package org.whired.inspexi.master;

/**
 * Listens for commands
 * 
 * @author Whired
 */
public interface CommandListener {
	/**
	 * Invoked when a command is to be executed
	 * 
	 * @param command the command to execute
	 */
	void doCommand(String command);
}
