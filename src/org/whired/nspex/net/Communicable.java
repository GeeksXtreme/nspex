package org.whired.nspex.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import org.whired.nspex.tools.logging.Log;

/**
 * Provides methods for communicating with a remote host
 * @author Whired
 */
public abstract class Communicable {
	private long lastReadTime = System.currentTimeMillis();
	private int readTimeout;
	boolean connected = true;
	final String hostName;

	public Communicable(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * Packages and sends a packet with the specified id
	 * @param id the id of the packet
	 */
	public abstract void send(int id);

	/**
	 * Receives data
	 * @throws IOException
	 */
	protected abstract int read() throws IOException;

	/**
	 * Packages and sends a packet with the specified id and payload
	 * @param id the id of the packet
	 * @param payload the payload of the packet
	 */
	public abstract void send(int id, ByteBuffer payload);

	/**
	 * Invoked when a packet without a payload has been fully received
	 * @param id the id of the packet that was received
	 */
	public abstract void handle(int id);

	/** The opcode dedicated to remote logging */
	static final int REMOTE_LOG = 5;

	/**
	 * Logs a message to the remote
	 * @param level the level of the message to log
	 * @param message the message to log
	 */
	public void remoteLog(final Level level, final String message) {
		ExpandableByteBuffer buffer = new ExpandableByteBuffer();
		buffer.putInt(level.intValue()).putJTF(message);
		send(REMOTE_LOG, buffer.asByteBuffer());
	}

	/**
	 * Invoked when a message from the remote has been logged
	 * @param level the level of the message that was logged
	 * @param message the message that was logged
	 */
	public void remoteLogged(final Level level, final String message) {
		Log.l.log(level, message);
	}

	/**
	 * Invoked when a packet has been fully received
	 * @param id the id of the packet that was received
	 * @param payload the payload of the packet that was received
	 */
	public abstract void handle(int id, ByteBuffer payload);

	/**
	 * Requests that this communicable is disconnected
	 */
	public abstract void disconnect();

	/**
	 * Invoked when this communicable has been disconnected
	 */
	protected abstract void disconnected();

	public boolean isConnected() {
		return connected;
	}

	/**
	 * Gets the amount of time (ms) that can pass between now and the next read and any consecutive reads a non-positive value is interpreted as an infinite timeout
	 * @return the timeout, in milliseconds
	 */
	int getReadTimeout() {
		return readTimeout;
	}

	/**
	 * Sets the amount of time (ms) that can pass between now and the next read and any consecutive reads a non-positive value is interpreted as an infinite timeout
	 * @param timeout the timeout, in milliseconds, to set
	 */
	public void setReadTimeout(final int timeout) {
		readTimeout = timeout;
	}

	void setLastReadTime(final long time) {
		lastReadTime = time;
	}

	/**
	 * Gets the time (in ms) of the last successful read
	 * @return the time, in ms
	 */
	long getLastReadTime() {
		return lastReadTime;
	}

	@Override
	public String toString() {
		return hostName;
	}
}
