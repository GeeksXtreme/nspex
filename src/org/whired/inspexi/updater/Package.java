package org.whired.inspexi.updater;

import java.io.File;

/**
 * Represents a JAR package that can be updated and downloaded if necessary
 * @author Whired
 */
public class Package {
	/** An OS-independent file separator */
	public final static String FS = System.getProperty("file.separator");

	/** The name of this package */
	private final String name;
	/** The path to the remote code base for this package */
	private final String remoteCodebase;
	/** The path to the local code base for this package */
	private final String localCodebase;
	/** The fully-qualified name for the main class for this package */
	private final String mainClass;

	/**
	 * Creates a new package
	 * @param name the name of the package
	 * @param remoteCodebase the path to the remote code base (excluding the name)
	 * @param localCodebase the path to the local code base (excluding the name) if the path does not exist, an attempt will be made to create it
	 * @param mainClass the fully-qualified name of the main class for this package
	 */
	public Package(final String name, final String remoteCodebase, final String localCodebase, final String mainClass) {
		this.name = name;
		this.remoteCodebase = remoteCodebase;
		this.localCodebase = localCodebase;
		final File f = new File(localCodebase);
		if (!f.exists()) {
			f.mkdirs();
		}
		this.mainClass = mainClass;
	}

	/**
	 * Gets the name for this package
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the path to the remote code base for this package
	 * @return the path
	 */
	public String getRemoteCodebase() {
		return remoteCodebase;
	}

	/**
	 * Gets the path the local code base for this package
	 * @return the path
	 */
	public final String getLocalCodebase() {
		return localCodebase;
	}

	/**
	 * Gets the main class of this package
	 * @return the fully-qualified name
	 */
	public String getMainClass() {
		return mainClass;
	}
}
