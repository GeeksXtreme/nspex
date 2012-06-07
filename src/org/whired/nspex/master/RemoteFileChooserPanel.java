package org.whired.nspex.master;

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

import org.whired.nspex.tools.LazyTreeNode;
import org.whired.nspex.tools.RemoteFile;
import org.whired.nspex.tools.logging.Log;

public abstract class RemoteFileChooserPanel extends JPanel implements TreeWillExpandListener, TreeSelectionListener {

	private final LazyTreeNode root = new LazyTreeNode("", true);
	private final JTree treeFiles = new JTree(root);
	private Image thumbnail;
	private final Color grayBorder = new Color(146, 151, 161);
	private final JPanel pnlPreview = new JPanel() {
		@Override
		protected void paintComponent(final Graphics g) {
			g.setColor(this.getBackground());
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			if (thumbnail != null) {
				final Graphics2D g2 = (Graphics2D) g;
				final int tWidth = thumbnail.getWidth(this);
				final int tHeight = thumbnail.getHeight(this);

				final int dx = this.getWidth() / 2 - tWidth / 2;
				final int dy = this.getHeight() / 2 - tHeight / 2;

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
		final FlowLayout fl_pnlPreview = (FlowLayout) pnlPreview.getLayout();
		fl_pnlPreview.setHgap(10);
		fl_pnlPreview.setVgap(80);
		add(pnlPreview, BorderLayout.SOUTH);
		treeFiles.setScrollsOnExpand(false);
		treeFiles.setShowsRootHandles(true);
		treeFiles.getSelectionModel().addTreeSelectionListener(this);

		final JScrollPane scrollPane = new JScrollPane();
		final LineBorder b = new LineBorder(grayBorder);
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

	public void addChildren(final char fs, final String parentPath, final RemoteFile[] children) {
		Log.l.fine("addChildren(), fs=" + fs + " parentPath=" + parentPath + " children.length=" + children.length);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				LazyTreeNode ltn = root;
				if (parentPath.length() > 0) {
					final String[] oldNodes = parentPath.split("\\" + fs);
					final String[] goodNodes;
					goodNodes = new String[oldNodes.length + 1];
					goodNodes[0] = parentPath.substring(0, parentPath.indexOf(fs) + 1);
					if (oldNodes.length > 0) {
						System.arraycopy(oldNodes, 0, goodNodes, 1, oldNodes.length);
					}
					Log.l.fine("goodNodes.length=" + goodNodes.length);

					Enumeration e;
					for (int i = 0; i < goodNodes.length; i++) {
						if (i > 0) {
							goodNodes[i] += fs; // Maybe not..
						}
						e = ltn.children();
						while (e.hasMoreElements()) {
							final Object pmatch = e.nextElement();
							if (pmatch.toString().equals(goodNodes[i])) {
								ltn = (LazyTreeNode) pmatch;
							}
						}
					}
				}
				ltn.expanding();
				for (final RemoteFile child : children) {
					boolean b = child.hasChildren();
					String s = child.toString();
					ltn.add(new LazyTreeNode(s + (b && !s.endsWith("" + fs) ? fs : ""), b));
				}
				((DefaultTreeModel) treeFiles.getModel()).nodeStructureChanged(ltn);
				ltn.setExpanded();
			}
		});

	}

	public void setThumbnail(final Image thumb) {
		this.thumbnail = thumb;
		pnlPreview.repaint();
	}

	@Override
	public void valueChanged(final TreeSelectionEvent e) {
		final LazyTreeNode node = (LazyTreeNode) treeFiles.getLastSelectedPathComponent();
		if (node != null && node.isLeaf()) {
			final String name = node.toString().toLowerCase();
			if (name.endsWith("jpeg") || name.endsWith("jpg") || name.endsWith("bmp") || name.endsWith("png") || name.endsWith("gif")) {
				String fname = pathToString(node.getPath());
				requestThumbnail(fname);
			}
		}
	}

	@Override
	public void treeWillExpand(final TreeExpansionEvent e) throws ExpandVetoException {
		// Load children from server!
		final LazyTreeNode parent = (LazyTreeNode) e.getPath().getLastPathComponent();

		if (!parent.hasBeenExpanded()) {
			// Hasn't been expanded, load children now
			requestChildren(pathToString(parent.getPath()));
		}
	}

	@Override
	public void treeWillCollapse(final TreeExpansionEvent arg0) throws ExpandVetoException {
		// We don't care
	}

	private String pathToString(final TreeNode[] path) {
		final StringBuilder sb = new StringBuilder();
		for (final TreeNode element : path) {
			sb.append(element.toString());
		}
		return sb.toString();
	}

	public Dimension getThumbSize() {
		invalidate();
		return pnlPreview.getSize();
	}
}
