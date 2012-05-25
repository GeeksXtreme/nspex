package org.whired.inspexi.net;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Provides methods for communicating with a remote host
 * @author Whired
 */
public abstract class Communicable {
	private long lastReadTime = System.currentTimeMillis();
	private int readTimeout = 0;

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
	 * @param payload the NON-{@code flip()}PED payload of the packet
	 */
	public abstract void send(int id, ByteBuffer payload);

	/**
	 * Invoked when a packet without a payload has been fully received
	 * @param id the id of the packet that was received
	 */
	public abstract void handle(int id);

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
	public void setReadTimeout(int timeout) {
		readTimeout = timeout;
	}

	void setLastReadTime(long time) {
		lastReadTime = time;
	}

	/**
	 * Gets the time (in ms) of the last successful read
	 * @return the time, in ms
	 */
	long getLastReadTime() {
		return lastReadTime;
	}
}
