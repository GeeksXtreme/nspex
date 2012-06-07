package org.whired.nspex.tools;

public class RemoteFile {
	private final String name;
	private final boolean hasChildren;

	public RemoteFile(final String name, final boolean hasChildren) {
		this.name = name;
		this.hasChildren = hasChildren;
	}

	public boolean hasChildren() {
		return hasChildren;
	}

	@Override
	public String toString() {
		return name;
	}
}
