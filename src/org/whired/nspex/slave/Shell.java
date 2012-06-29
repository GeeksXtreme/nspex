package org.whired.nspex.slave;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.logging.Level;

import org.whired.nspex.tools.logging.Log;

/**
 * A shell (console) with support for input and output
 * @author Whired
 */
public abstract class Shell {

	private final BufferedReader reader;
	private final BufferedWriter writer;

	/**
	 * Creates a new shell
	 * @param program the name (and possibly path) of the shell to execute (IE 'cmd','/bin/bash')
	 */
	Shell(String program) throws IOException {
		final ProcessBuilder pb = new ProcessBuilder(program);
		pb.redirectErrorStream(true);
		final Process p = pb.start();
		reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
		readerThread.start();
	}

	private final Thread readerThread = new Thread(new Runnable() {
		@Override
		public void run() {
			executeCommand("echo connected");
			String line;
			try {
				while ((line = reader.readLine()) != null) {
					// Pretty easy, just send whatever output there is
					outputReceived(line + "\r\n");
				}
			}
			catch (IOException e) {
				Log.l.log(Level.WARNING, "Error reading shell:", e);
			}
			finally {
				close();
			}
		}
	});

	/**
	 * Invoked when the shell has been closed
	 */
	protected abstract void closed();

	/**
	 * Closes the shell
	 */
	void close() {
		try {
			Log.l.finest("Closing shell");
			reader.close();
			writer.close();
		}
		catch (IOException e) {
			Log.l.log(Level.WARNING, "Error closing shell:", e);
		}
		finally {
			closed();
		}
	}

	/**
	 * Attempts to execute the specified command
	 * @param command the command to execute
	 */
	public void executeCommand(String command) {
		try {
			writer.write(command);
			writer.newLine();
			writer.flush();
		}
		catch (IOException e) {
			Log.l.log(Level.WARNING, "Error writing:", e);
			close();
		}
	}

	/**
	 * Invoked when output from the shell is received
	 * @param output the output that was received
	 */
	protected abstract void outputReceived(String output);
}
