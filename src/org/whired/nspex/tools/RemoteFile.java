package org.whired.nspex.tools;

import java.awt.Image;

public class RemoteFile {
	private final String name;
	private final boolean hasChildren;
	private final Image thumbnail;
	private final long size = 0;

	public RemoteFile(final String name, final boolean hasChildren) {
		this(name, hasChildren, null);
	}

	public boolean hasChildren() {
		return hasChildren;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Creates a remote file with the specified name and thumbnail
	 * @param name the name of the file
	 * @param thumbnail the thumbnail for the file
	 */
	public RemoteFile(final String name, final boolean hasChildren, final Image thumbnail) {
		this.name = name;
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

	public long getSize() {
		return size;
	}
}
