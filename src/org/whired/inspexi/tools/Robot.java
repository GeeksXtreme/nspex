package org.whired.inspexi.tools;

import java.awt.Rectangle;

public abstract class Robot {
	private final Rectangle bounds;
	private final double zoom;

	protected Robot(Rectangle bounds, double zoom) {
		if (bounds == null) {
			bounds = getScreenBounds();
		}
		this.bounds = bounds;
		this.zoom = zoom;
	}

	public abstract byte[] getBytePixels();

	public abstract Rectangle getScreenBounds();

	public int getZoom(int orig) {
		return (int) (orig * zoom);
	}

	public Rectangle getBounds() {
		return this.bounds;
	}
}
