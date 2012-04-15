package org.whired.inspexi.blackbox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;

import org.whired.inspexi.tools.NetTask;
import org.whired.inspexi.tools.NetTaskQueue;
import org.whired.inspexi.tools.SessionListener;

/**
 * The authentication server
 * 
 * @author Whired
 */
public class AuthenticationServer {
	private final ServerSocket socket;
	private final SessionListener listener = new SessionListener() {

		@Override
		public void sessionEnded(String reason) {
			// Ummmm? to be honest there needs to be more to this!!!! the socket is not given!
		}
	};
	private final NetTaskQueue queue = new NetTaskQueue(listener);

	// !!! We want communication here to be as minimal as possible !!!
	// !!! We also want to accept and delegate as quickly as possible !!!

	/**
	 * Starts an authentication server on the specified port
	 * 
	 * @param port the port to listen for connections on
	 * @throws IOException
	 */
	public AuthenticationServer(int port) throws IOException {
		socket = new ServerSocket();

		while (true) {
			queue.add(new NetTask("auth_reactor", socket.accept()) {

				@Override
				public void run(DataInputStream dis, DataOutputStream dos) throws IOException {
					// TODO auth logic here
				}
			});

		}
	}

	public static void main(String[] args) throws IOException {
		new AuthenticationServer(43597);
	}
}
