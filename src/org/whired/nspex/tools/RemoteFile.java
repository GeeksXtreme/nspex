package org.whired.nspex.tools;

import java.awt.Image;

public class RemoteFile {
	/** The name of this file */
	private final String name;
	/** Whether or not this file has children */
	private final boolean hasChildren;
	/** The thumbnail for this file */
	private final Image thumbnail;
	/** The size of this file, in bytes */
	private final long size;

	public RemoteFile(final String name, final long size, final boolean hasChildren) {
		this(name, size, hasChildren, null);
	}

	/**
	 * Determines whether or not this file has children
	 * @return
	 */
	public boolean hasChildren() {
		return hasChildren;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Creates a remote file
	 * @param name the name of the file
	 * @param size the size of the file, in bytes
	 * @param hasChildren whether or not this file has children
	 * @param thumbnail the thumbnail for the file
	 */
	public RemoteFile(final String name, final long size, final boolean hasChildren, final Image thumbnail) {
		this.name = name;
		this.size = size;
		this.hasChildren = hasChildren;
		this.thumbnail = thumbnail;
	}

	/**
	 * Gets the thumbnail for this file
	 * @return the thumbnail, or null if no thumbnail could be rendered
	 */
	public Image getThumbnail() {
		return thumbnail;
	}

	/**
	 * Gets the name of this file
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the size of this file, in bytes
	 * @return
	 */
	public long getSize() {
		return size;
	}
}
