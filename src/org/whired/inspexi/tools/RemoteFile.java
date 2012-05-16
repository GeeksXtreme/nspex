package org.whired.inspexi.tools;

public class RemoteFile {
	private final String name;
	private final boolean hasChildren;

	public RemoteFile(String name, boolean hasChildren) {
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
