package org.whired.inspexi.master;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;

import org.whired.inspexi.tools.LazyTreeNode;
import org.whired.inspexi.tools.RemoteFile;

public abstract class RemoteFileChooserPanel extends JPanel implements TreeWillExpandListener, TreeSelectionListener {

	private final LazyTreeNode root = new LazyTreeNode("", true);
	private final JTree treeFiles = new JTree(root);
	private Image thumbnail;
	private final Color grayBorder = new Color(146, 151, 161);

	private final JPanel pnlPreview = new JPanel() {
		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(this.getBackground());
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			if (thumbnail != null) {
				final Graphics2D g2 = (Graphics2D) g;
				//g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				int tWidth = thumbnail.getWidth(this);
				int tHeight = thumbnail.getHeight(this);

				int dx = this.getWidth() / 2 - tWidth / 2;
				int dy = this.getHeight() / 2 - tHeight / 2;

				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				g2.drawImage(thumbnail, dx, dy, dx + tWidth, dy + tHeight, 0, 0, tWidth, tHeight, this);
			}
			g.setColor(grayBorder);
			g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);
			g.dispose();
		};
	};

	/**
	 * Create the dialog
	 */
	public RemoteFileChooserPanel() {
		setBorder(new EmptyBorder(2, 2, 2, 2));
		setLayout(new BorderLayout(0, 0));
		FlowLayout fl_pnlPreview = (FlowLayout) pnlPreview.getLayout();
		fl_pnlPreview.setHgap(10);
		fl_pnlPreview.setVgap(80);
		add(pnlPreview, BorderLayout.SOUTH);
		treeFiles.setScrollsOnExpand(false);
		treeFiles.setShowsRootHandles(true);
		treeFiles.getSelectionModel().addTreeSelectionListener(this);

		JScrollPane scrollPane = new JScrollPane();
		LineBorder b = new LineBorder(grayBorder);
		scrollPane.setBorder(b);
		scrollPane.getVerticalScrollBar().setUI(new MinimalScrollBar(scrollPane.getVerticalScrollBar()));
		scrollPane.getHorizontalScrollBar().setUI(new MinimalScrollBar(scrollPane.getHorizontalScrollBar()));
		scrollPane.setViewportView(treeFiles);
		((BorderLayout) getLayout()).setVgap(1);
		add(scrollPane, BorderLayout.CENTER);
		treeFiles.addTreeWillExpandListener(this);
		treeFiles.collapseRow(0);
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
				for (RemoteFile child : children) {
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

	public Dimension getThumbSize() {
		invalidate();
		return pnlPreview.getSize();
	}
}