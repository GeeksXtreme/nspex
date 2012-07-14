package org.whired.nspex.tools;

public class DefaultSlave implements Slave {
	/** Slave information */
	private String user, os, version;
	private String host;
	private boolean online;

	public DefaultSlave() {
	}

	/**
	 * Creates a new slave with the specified ip
	 * @param host the ip for this slave
	 */
	public DefaultSlave(final String host) {
		this.host = host;
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
	 * Gets the host of this slave
	 */
	public String getHost() {
		return this.host;
	}

	@Override
	public String toString() {
		return this.host;
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
	public void setOnline(final boolean online) {
		this.online = online;
	}
}
