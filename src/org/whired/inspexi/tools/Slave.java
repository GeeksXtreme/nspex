package org.whired.inspexi.tools;

public abstract class Slave {
	public static String REMOTE_VERSION = "0.0.3";
	public static final int INTENT_CHECK = 0, INTENT_REBUILD = 8, INTENT_CONNECT = 1;
	public static final int OP_TRANSFER_IMAGE = 0;
	private String host, os, version;

	public String getOS() {
		return os;
	}

	public void setOS(String os) {
		this.os = os;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
