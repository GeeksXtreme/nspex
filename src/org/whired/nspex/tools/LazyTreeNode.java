package org.whired.nspex.tools;

import javax.swing.tree.DefaultMutableTreeNode;

public class LazyTreeNode extends DefaultMutableTreeNode {

	private boolean hasBeenExpanded;
	private final boolean willHaveChildren;

	public LazyTreeNode(final String label, final boolean willHaveChildren) {
		super(label);
		this.willHaveChildren = willHaveChildren;
		if (this.willHaveChildren) {
			add(new LazyTreeNode("Loading..", false));
		}
	}

	public boolean hasBeenExpanded() {
		return hasBeenExpanded;
	}

	public void expanding() {
		if (!hasBeenExpanded && willHaveChildren) {
			this.removeAllChildren();
		}
	}

	public void setExpanded() {
		hasBeenExpanded = true;
	}
}
