package org.whired.nspex.master;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Performs repetitive graphics tasks
 * @author Whired
 */
public class GraphicsUtil {
	/**
	 * Draws the specified string with a drop shadow
	 * @param g the graphics to draw to
	 * @param s the string to draw
	 * @param px the x coordinate
	 * @param py the y coordinate
	 * @param bg the background color
	 */
	public static void drawStringDropShadow(Graphics g, String s, int px, int py, Color bg) {
		// Save the initial color
		Color oldColor = g.getColor();
		g.setColor(bg);
		for (int x = -1; x < 2; x++) {
			for (int y = -1; y < 2; y++) {
				g.drawString(s, px + x, py + y);
			}
		}
		// Restore the initial color
		g.setColor(oldColor);
		g.drawString(s, px, py);
	}
}
