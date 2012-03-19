package org.whired.inspexi.master;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class MinimalScrollBar extends BasicScrollBarUI {

	final Color transparent = new Color(0, 0, 0, 0);
	final Color ghostBlue = new Color(99, 130, 191, 120);
	final Color ghostHighlight = new Color(119, 150, 211, 140);

	public MinimalScrollBar(final JScrollBar scrollbar) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				scrollbar.setOpaque(false);
				scrollbar.setPreferredSize(new Dimension(8, scrollbar.getHeight()));
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
	protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
		paint(g, c, r);
	}

	@Override
	protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
		paint(g, c, r);
	}

	private void paint(Graphics g, JComponent c, Rectangle r) {
		g.setColor(new Color(255, 255, 255));
		g.fillRect(0, 0, c.getWidth(), c.getHeight());
		if (this.isThumbRollover() || this.isDragging) {
			g.setColor(ghostHighlight);
		}
		else {
			g.setColor(c.getParent().getBackground());
		}
		((java.awt.Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.fillRoundRect(r.x + r.width - 8, r.y, 8, r.height, 5, 5);
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		super.paint(g, c);
	}

}
