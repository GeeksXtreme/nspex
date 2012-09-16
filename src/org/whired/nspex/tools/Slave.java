package org.whired.nspex.tools;

/** A slave */
public interface Slave {
	/** The version */
	String VERSION = "0.1.0";
	/** The port */
	int PORT = 43596;
	/** Opcodes */
	int INTENT_CHECK = 0, INTENT_CONNECT = 1, INTENT_CHECK_BULK = 2, INTENT_REBUILD = 8;
	int OP_HANDSHAKE = 0, OP_TRANSFER_IMAGE = 1, OP_DO_COMMAND = 2, OP_GET_FILES = 3, OP_FILE_ACTION = 4, OP_REMOTE_SHELL = 6;
	/** File manipulation opcodes */
	int FOP_GET_INFO = 0, FOP_DOWNLOAD = 1, FOP_RENAME = 2, FOP_DELETE = 3;

	/**
	 * Gets the current operating system for this slave
	 * @return the operating system and architecture
	 */
	String getOS();

	/**
	 * Sets the operating sytem for this slave
	 * @param os
	 */
	void setOS(final String os);

	/**
	 * Gets the name of the user for this slave
	 * @return the name
	 */
	String getUser();

	/**
	 * Sets the name of the user for this slave
	 * @param user the name to set
	 */
	void setUser(final String user);

	/**
	 * Gets the version of this slave
	 * @return the version
	 */
	String getVersion();

	/**
	 * Sets the version for this slave
	 * @param version the version to set
	 */
	void setVersion(final String version);

	/**
	 * Gets the host of this slave
	 */
	String getHost();

	/**
	 * Determines whether or not this slave is online
	 * @return {@code true} if the slave is online, otherwise {@code false}
	 */
	boolean isOnline();

	/**
	 * Sets whether or not this slave is online
	 * @param online
	 */
	void setOnline(final boolean online);
}
