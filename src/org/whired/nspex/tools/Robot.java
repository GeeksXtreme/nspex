package org.whired.nspex.tools;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/**
 * An abstract robot used to survey and control a machine
 * @author Whired
 */
public abstract class Robot {
	/** The resolution of the default screen */
	private static final DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
	/** The bounds of the default screen */
	private static Rectangle screenBounds = new Rectangle(0, 0, dm.getWidth(), dm.getHeight());
	/** The capture bounds for this robot */
	private final Rectangle captureBounds;
	/** The zoom of the image this robot will produce */
	private final double zoom;

	/**
	 * Creates a new robot with the specified capture bounds and zoom
	 * @param captureBounds the bounds to capture, or {@code null} to capture the whole screen
	 * @param zoom the amount to scale by
	 */
	protected Robot(Rectangle captureBounds, final double zoom) {
		if (captureBounds == null) {
			captureBounds = getScreenBounds();
		}
		this.captureBounds = captureBounds;
		this.zoom = zoom;
	}

	/**
	 * Gets the pixels located in the rectangle specified by {@link #getCaptureBounds()}
	 * @return the pixels
	 */
	public abstract byte[] getPixels();

	/**
	 * Gets the captureBounds of the entire screen
	 * @return the captureBounds
	 */
	public static Rectangle getScreenBounds() {
		return screenBounds;
	}

	/**
	 * Scales the given integer by {@link #zoom}
	 * @param orig the integer to scale
	 * @return the scaled integer
	 */
	public int scale(final int orig) {
		return (int) (orig * zoom);
	}

	/**
	 * Calculates a zoom factor to achieve the target size given an original size
	 * @param originalSize the original size to scale
	 * @param targetSize he target size
	 * @return the amount of zoom that is needed to achieve the target size
	 */
	public static double calculateZoom(final Rectangle originalSize, final Dimension targetSize) {
		// Target size is bigger, abort
		if (originalSize.width < targetSize.width && originalSize.height < targetSize.width) {
			return 1;
		}
		// Zoom using width
		else if (targetSize.width > targetSize.height) {
			return 1f * targetSize.width / originalSize.width;
		}
		// Zoom using height
		else {
			return 1f * targetSize.height / originalSize.height;
		}
	}

	/**
	 * Gets the capture captureBounds for this robot
	 * @return the captureBounds
	 */
	public Rectangle getCaptureBounds() {
		return this.captureBounds;
	}
}
