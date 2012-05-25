package org.whired.inspexi.tools;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

public abstract class Robot {
	private final Rectangle bounds;
	private final double zoom;

	protected Robot(Rectangle bounds, final double zoom) {
		if (bounds == null) {
			bounds = getScreenBounds();
		}
		this.bounds = bounds;
		this.zoom = zoom;
	}

	public abstract byte[] getBytePixels();

	private static final DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
	private static Rectangle screenBounds = new Rectangle(0, 0, dm.getWidth(), dm.getHeight());

	public static Rectangle getScreenBounds() {
		return screenBounds;
	}

	public int getZoom(final int orig) {
		return (int) (orig * zoom);
	}

	public static double calculateZoom(Rectangle source, Dimension dest) {
		if (source.width < dest.width && source.height < dest.width) {
			return 1;
		}
		if (dest.width > dest.height) {
			return 1f * dest.width / source.width;
		}
		else {
			return 1f * dest.height / source.height;
		}
	}

	public Rectangle getBounds() {
		return this.bounds;
	}
}
