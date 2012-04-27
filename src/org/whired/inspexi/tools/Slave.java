package org.whired.inspexi.tools;

public abstract class Slave {
	public static String VERSION = "0.0.7";
	public static final int INTENT_CHECK = 0, INTENT_CONNECT = 1, INTENT_CHECK_BULK = 2, INTENT_REBUILD = 8;
	public static final int OP_TRANSFER_IMAGE = 0, OP_DO_COMMAND = 1;
	private String user, os, version;

	public String getOS() {
		return os;
	}

	public void setOS(final String os) {
		this.os = os;
	}

	public String getUser() {
		return user;
	}

	public void setUser(final String user) {
		this.user = user;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(final String version) {
		this.version = version;
	}
}
