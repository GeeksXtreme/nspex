package org.whired.inspexi.tools;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Immediately accepts incoming connections and defers the session to a designated thread pool executor
 * 
 * @author Whired
 */
public abstract class ReactServer {
	/** The port to listen on */
	private final int port;
	/** The queue that will accept net tasks */
	private final NetTaskQueue queue;

	/**
	 * Creates a new react server on the specified port with the specified queue
	 * 
	 * @param port the port to listen for incoming connections on
	 * @param queue the queue that will accept net tasks
	 */
	public ReactServer(int port, NetTaskQueue queue) {
		this.port = port;
		this.queue = queue;
	}

	/**
	 * Binds and starts the server Blocks the current thread.
	 * 
	 * @throws IOException
	 */
	public void bind() throws IOException {
		ServerSocket ssock = new ServerSocket(port);
		while (true) {
			queue.add(getOnConnectTask(ssock.accept()));
		}
	}

	/**
	 * Gets the task to execute when a client is connecting
	 * 
	 * @param sock the socket
	 * @return the net task to run
	 */
	public abstract NetTask getOnConnectTask(Socket sock);
}
