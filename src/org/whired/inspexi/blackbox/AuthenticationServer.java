package org.whired.inspexi.blackbox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.whired.inspexi.tools.NetTask;
import org.whired.inspexi.tools.NetTaskQueue;
import org.whired.inspexi.tools.ReactServer;
import org.whired.inspexi.tools.SessionListener;

/**
 * The authentication server
 * @author Whired
 */
public class AuthenticationServer {
	private final SessionListener listener = new SessionListener() {
		@Override
		public void sessionEnded(final String reason) {
			// Turns out, we really don't care
			// It's best to just assume every connection is from a hacker and drop them without grace
		}
	};
	private final NetTaskQueue queue = new NetTaskQueue(listener);

	/**
	 * Starts an authentication server on the specified port
	 * @param port the port to listen for connections on
	 * @throws IOException
	 */
	public AuthenticationServer(final int port) throws IOException {
		final ReactServer server = new ReactServer(port, queue) {
			@Override
			public NetTask getOnConnectTask(final Socket sock) {
				return new NetTask("auth_connect", sock) {
					@Override
					public void run(final DataInputStream dis, final DataOutputStream dos) throws IOException {
						// First things first, these guys need to do their business QUICK
						sock.setSoTimeout(2000);
						// We need to send a crypt key here

						// Read hwid, name, and password

						// Compare ips/hwid/salt -- limit of 3 (_hashed_) per user!

						// Database work

						// Pick a slave

						// Tell slave to accept the calculated hash - master cannot replicate this

						// Write slave ip
					}
				};
			}
		};
		System.out.println("Starting authentication server");
		server.bind();
	}

	public static void main(final String[] args) throws IOException {
		new AuthenticationServer(43597);
	}
}
