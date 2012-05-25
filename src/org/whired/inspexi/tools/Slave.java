package org.whired.inspexi.tools;

/** A slave */
public class Slave {
	/** The version */
	public static String VERSION = "0.0.8";
	/** Opcodes */
	public static final int INTENT_CHECK = 0, INTENT_CONNECT = 1, INTENT_CHECK_BULK = 2, INTENT_REBUILD = 8;
	public static final int OP_HANDSHAKE = 0, OP_TRANSFER_IMAGE = 1, OP_DO_COMMAND = 2, OP_GET_FILES = 3, OP_GET_FILE_THUMB = 4;
	/** Slave information */
	private String user, os, version;
	private String ip;
	private boolean online;

	public Slave() {
	}

	/**
	 * Creates a new slave with the specified ip
	 * @param ip the ip for this slave
	 */
	public Slave(String ip) {
		this.ip = ip;
	}

	/**
	 * Gets the current operating system for this slave
	 * @return the operating system and architecture
	 */
	public String getOS() {
		return os;
	}

	/**
	 * Sets the operating sytem for this slave
	 * @param os
	 */
	public void setOS(final String os) {
		this.os = os;
	}

	/**
	 * Gets the name of the user for this slave
	 * @return the name
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Sets the name of the user for this slave
	 * @param user the name to set
	 */
	public void setUser(final String user) {
		this.user = user;
	}

	/**
	 * Gets the version of this slave
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the version for this slave
	 * @param version the version to ddd
	 */
	public void setVersion(final String version) {
		this.version = version;
	}

	/**
	 * Gets the IP of this slave
	 */
	public String getIp() {
		return this.ip;
	}

	@Override
	public String toString() {
		return this.ip;
	}

	/**
	 * Determines whether or not this slave is online
	 * @return {@code true} if the slave is online, otherwise {@code false}
	 */
	public boolean isOnline() {
		return online;
	}

	/**
	 * Sets whether or not this slave is online
	 * @param online
	 */
	public void setOnline(boolean online) {
		this.online = online;
	}
}
