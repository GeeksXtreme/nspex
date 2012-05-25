package org.whired.inspexi.master;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Scrollbar;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * A minimal skin for scrollbars
 * @author Whired
 */
public class MinimalScrollBar extends BasicScrollBarUI {

	final Color transparent = new Color(0, 0, 0, 0);
	final Color ghostBlue = new Color(99, 130, 191, 120);
	final Color ghostHighlight = new Color(119, 150, 211, 140);

	public MinimalScrollBar(final JScrollBar scrollbar) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				scrollbar.setOpaque(false);
				if (scrollbar.getOrientation() == Scrollbar.VERTICAL) {
					scrollbar.setPreferredSize(new Dimension(8, scrollbar.getHeight()));
				}
				else {
					scrollbar.setPreferredSize(new Dimension(scrollbar.getWidth(), 8));
				}
				if (decrButton != null && decrButton.getParent() != null) {
					decrButton.getParent().remove(decrButton);
				}
				if (incrButton != null && incrButton.getParent() != null) {
					incrButton.getParent().remove(incrButton);
				}
			}
		});
	}

	@Override
	protected void paintThumb(final Graphics g, final JComponent c, final Rectangle r) {
		paint(g, c, r);
	}

	@Override
	protected void paintTrack(final Graphics g, final JComponent c, final Rectangle r) {
		paint(g, c, r);
	}

	private void paint(final Graphics g, final JComponent c, final Rectangle r) {
		g.setColor(((JScrollPane) c.getParent()).getViewport().getView().getBackground());
		g.fillRect(0, 0, c.getWidth(), c.getHeight());
		if (this.isThumbRollover() || this.isDragging) {
			g.setColor(ghostHighlight);
		}
		else {
			g.setColor(ghostBlue);
		}
		((java.awt.Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.fillRoundRect(r.x, r.y, r.width, r.height, 5, 5);
	}

	@Override
	public void paint(final Graphics g, final JComponent c) {
		super.paint(g, c);
	}

}
