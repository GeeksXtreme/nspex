package org.whired.inspexi.tools;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;

/**
 * A network task
 * @author Whired
 */
public abstract class NetTask {

	/** The socket that this net task will use */
	protected final Socket socket;

	/** The session listener to notify when something goes wrong */
	protected final SessionListener listener;

	protected boolean timeoutFatal;

	/**
	 * Creates a new task with the specified name
	 * @param name the name of the task
	 * @param socket the socket that this net task will use
	 */
	public NetTask(final SessionListener listener, final Socket socket) {
		this.listener = listener;
		this.socket = socket;
	}

	/**
	 * Runs this task
	 * @throws IOException when a network error occurs
	 * @throws GeneralSecurityException when an cryption error occurs
	 */
	public abstract void run(DataInputStream dis, DataOutputStream dos) throws IOException, GeneralSecurityException;

	/** This will be ran if {@link #run(DataInputStream, DataOutputStream)} throws an exception */
	public void onFail() {
		// Overriding is optional
	}
}
