package org.whired.nspex.tools;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * An AWT implementation of a {@link Robot}
 * @author Whired
 */
public final class AWTRobot extends Robot {
	/** The unscaled image */
	private final BufferedImage unscaled;
	/** The unscaled pixels backing {@link #unscaled} */
	private final int[] unscaledPix;
	/** The target size to scale to */
	private final Dimension targetSize;

	/**
	 * Creates a new awt robot with the specified capture bounds and desired zoom level
	 * @param bounds the bounds to capture
	 * @param zoom the zoom to scale to, between .1 and 1.0 inclusive
	 * @throws AWTException if the robot can't be created
	 */
	public AWTRobot(final Rectangle bounds, final double zoom) throws AWTException {
		super(bounds, zoom);
		unscaled = new BufferedImage(getCaptureBounds().width, getCaptureBounds().height, BufferedImage.TYPE_INT_RGB);
		unscaledPix = ((DataBufferInt) unscaled.getRaster().getDataBuffer()).getData();
		targetSize = new Dimension(scale(getCaptureBounds().width), scale(getCaptureBounds().height));
	}

	/**
	 * Creates a new awt robot with default screen bounds and the specified zoom to scale to
	 * @param zoom the zoom to scale to, between .1 and 1.0 inclusive
	 * @throws AWTException if the robot can't be created
	 */
	public AWTRobot(final double zoom) throws AWTException {
		this(null, zoom);
	}

	/**
	 * Creates a new awt robot with the default screen bounds and the specified target size
	 * @param targetSize the target size to scale to
	 * @throws AWTException if the robot can't be created
	 */
	public AWTRobot(final Dimension targetSize) throws AWTException {
		this(Robot.calculateZoom(Robot.getScreenBounds(), targetSize));
	}

	@Override
	public byte[] getPixels() {
		final int[] pix = getAWTPeer().getRGBPixels(getCaptureBounds());
		System.arraycopy(pix, 0, unscaledPix, 0, unscaledPix.length);
		return JPEGImageWriter.getImageBytes(unscaled, targetSize);
	}
}