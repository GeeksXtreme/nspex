package org.whired.inspexi.tools;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.util.Enumeration;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;

public abstract class RemoteFileChooserDialog extends JDialog implements TreeWillExpandListener, TreeSelectionListener {

	private final JPanel contentPanel = new JPanel();
	private final LazyTreeNode root = new LazyTreeNode("", true);
	private final JTree treeFiles = new JTree(root);
	private Image thumbnail;
	private final JPanel pnlPreview = new JPanel() {
		@Override
		protected void paintComponent(Graphics g) {
			if (thumbnail != null) {
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				double tWidth = thumbnail.getWidth(this);
				double tHeight = thumbnail.getHeight(this);
				int nWidth = (int) (this.getHeight() * (tWidth / tHeight));
				g2.drawImage(thumbnail, 0, 0, (nWidth), this.getHeight(), 0, 0, (int) tWidth, (int) tHeight, this);
			}
		};
	};

	/**
	 * Create the dialog
	 */
	public RemoteFileChooserDialog() {
		setSize(300, 500);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		FlowLayout fl_pnlPreview = (FlowLayout) pnlPreview.getLayout();
		fl_pnlPreview.setHgap(10);
		fl_pnlPreview.setVgap(80);
		contentPanel.add(pnlPreview, BorderLayout.NORTH);
		treeFiles.setShowsRootHandles(true);
		treeFiles.getSelectionModel().addTreeSelectionListener(this);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(treeFiles);
		contentPanel.add(scrollPane, BorderLayout.CENTER);
		treeFiles.addTreeWillExpandListener(this);
		treeFiles.collapseRow(0);
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		JButton okButton = new JButton("OK");
		okButton.setActionCommand("OK");
		buttonPane.add(okButton);
		getRootPane().setDefaultButton(okButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("Cancel");
		buttonPane.add(cancelButton);
	}

	protected abstract void requestChildren(String parentPath);

	protected abstract void requestThumbnail(String path);

	public void addChildren(final String parentPath, final RemoteFile[] children) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				String[] parentNodes = parentPath.split("\\|");
				LazyTreeNode ltn = root;
				Enumeration e;
				System.out.println("Parent path: " + parentPath);
				for (String parentNode : parentNodes) {
					if (parentNode.length() > 0) {
						e = ltn.children();
						while (e.hasMoreElements()) {
							Object pmatch = e.nextElement();
							if (pmatch.toString().equals(parentNode)) {
								ltn = (LazyTreeNode) pmatch;
							}
						}
					}
				}
				ltn.expanding();
				System.out.println("End match: " + ltn);
				for (RemoteFile child : children) {
					System.out.println(child + " " + child.hasChildren());
					ltn.add(new LazyTreeNode(child.toString(), child.hasChildren()));
				}
				((DefaultTreeModel) treeFiles.getModel()).nodeStructureChanged(ltn);
				ltn.setExpanded();
			}
		});

	}

	public void setThumbnail(Image thumb) {
		this.thumbnail = thumb;
		pnlPreview.repaint();
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		LazyTreeNode node = (LazyTreeNode) treeFiles.getLastSelectedPathComponent();
		if (node != null && node.isLeaf()) {
			String name = node.toString().toLowerCase();
			if (name.endsWith("jpeg") || name.endsWith("jpg") || name.endsWith("bmp") || name.endsWith("png") || name.endsWith("gif")) {
				String fname = pathToString(node.getPath());
				fname = fname.substring(0, fname.length() - 1);
				requestThumbnail(fname);
			}
		}
	}

	@Override
	public void treeWillExpand(TreeExpansionEvent e) throws ExpandVetoException {
		// Load children from server!
		LazyTreeNode parent = (LazyTreeNode) e.getPath().getLastPathComponent();

		if (!parent.hasBeenExpanded()) {
			// Hasn't been expanded, load children now
			System.out.println("Time to load");

			// Add loaded children
			requestChildren(pathToString(parent.getPath()));
		}
	}

	@Override
	public void treeWillCollapse(TreeExpansionEvent arg0) throws ExpandVetoException {
		// We don't care
	}

	private String pathToString(TreeNode[] path) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < path.length; i++) {
			sb.append(path[i].toString());
			if (path[i].toString().length() > 0) {
				sb.append("|");
			}
		}
		return sb.toString();
	}
}
