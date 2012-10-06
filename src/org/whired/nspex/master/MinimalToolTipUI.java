package org.whired.nspex.master;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;

/**
 * A UI for a nice, minimal tooltip
 * @author Whired
 */
public class MinimalToolTipUI extends BasicToolTipUI {

	/** The single instance to get */
	private final static MinimalToolTipUI TOOL_TIP_UI = new MinimalToolTipUI();
	/** The font to draw */
	private final static Font FONT = new Font("SansSerif", Font.PLAIN, 9);
	/** The background color of the tooltip */
	private final static Color BG_COLOR = (Color) UIManager.getDefaults().get("nimbusSelectionBackground");

	/** Only used for metrics */
	private final static Graphics pg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
	private final static FontMetrics FONT_METRICS = pg.getFontMetrics(FONT);

	public static ComponentUI createUI(JComponent c) {
		return TOOL_TIP_UI;
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		g.setFont(FONT);
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(BG_COLOR);
		g.fillRoundRect(0, 0, g.getClipBounds().width, g.getClipBounds().height, 10, 10);
		g.setColor(Color.WHITE);
		g.drawString(((JToolTip) c).getTipText(), 4, g.getFontMetrics().getHeight());
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		final String str = ((JToolTip) c).getTipText();
		return new Dimension(FONT_METRICS.stringWidth(str) + 8, FONT_METRICS.getHeight() + 6);
	}
}
