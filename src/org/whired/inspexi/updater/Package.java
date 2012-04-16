package org.whired.inspexi.updater;

import java.io.File;

public class Package {
	public final static String FS = System.getProperty("file.separator");

	private final String name;
	private final String remoteCodebase;
	private final String localCodebase;
	private final String entryPoint;

	public Package(final String name, final String remoteCodebase, final String localCodebase, final String entryPoint) {
		this.name = name;
		this.remoteCodebase = remoteCodebase;
		this.localCodebase = localCodebase;
		this.entryPoint = entryPoint;
	}

	public String getName() {
		return name;
	}

	public String getRemoteCodebase() {
		return remoteCodebase;
	}

	public final String getLocalCodebase() {
		final File f = new File(localCodebase);
		if (!f.exists()) {
			f.mkdirs();
		}
		return localCodebase;
	}

	public String getEntryPoint() {
		return entryPoint;
	}
}
