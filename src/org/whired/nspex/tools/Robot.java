package org.whired.nspex.tools;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.peer.RobotPeer;

import sun.awt.ComponentFactory;

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
	/** The peer used for doing less time-sensitive operations */
	private final RobotPeer peer;

	/**
	 * Creates a new robot with the specified capture bounds and zoom
	 * @param captureBounds the bounds to capture, or {@code null} to capture the whole screen
	 * @param zoom the amount to scale by
	 * @throws AWTException
	 * @throws HeadlessException
	 */
	protected Robot(Rectangle captureBounds, final double zoom) throws HeadlessException, AWTException {
		peer = ((ComponentFactory) Toolkit.getDefaultToolkit()).createRobot(null, GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice());
		if (captureBounds == null) {
			captureBounds = getScreenBounds();
		}
		this.captureBounds = captureBounds;
		this.zoom = zoom;
	}

	/**
	 * Gets the awt robot peer
	 * @return the peer
	 */
	RobotPeer getAWTPeer() {
		return peer;
	}

	/**
	 * Gets the pixels located in the rectangle specified by {@link #getCaptureBounds()}
	 * @return the pixels
	 */
	public abstract byte[] getPixels();

	/**
	 * Moves the mouse to the specified x and y locations
	 * @param x the x-coordinate to move to
	 * @param y the y-coordinate to move to
	 */
	public void mouseMove(final int x, final int y) {
		peer.mouseMove(x, y);
	}

	/**
	 * Presses the specified mouse buttons
	 * @param buttons the (OR'd) buttons to press
	 */
	public void mousePress(int x, int y, final int buttons) {
		peer.mouseMove(unscale(x), unscale(y));
		peer.mousePress(buttons);
	}

	/**
	 * Releases the specified mouse buttons
	 * @param buttons the (OR'd) buttons to release
	 */
	public void mouseRelease(int x, int y, final int buttons) {
		peer.mouseMove(unscale(x), unscale(y));
		peer.mouseRelease(buttons);
	}

	/**
	 * Moves the mouse wheel by the specified amount
	 * @param wheelAmt the amount to move
	 */
	public void mouseWheel(final int wheelAmt) {
		peer.mouseWheel(wheelAmt);
	}

	/**
	 * Presses the specified key
	 * @param keycode the key to press
	 */
	public void keyPress(final int keycode) {
		peer.keyPress(keycode);
	}

	/**
	 * Releases the specified key
	 * @param keycode the key to release
	 */
	public void keyRelease(final int keycode) {
		peer.keyRelease(keycode);
	}

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
	 * Unscales the given integer by {@link #zoom}
	 * @param orig the integer to unscale
	 * @return the unscaled integer
	 */
	public int unscale(final int orig) {
		return (int) (orig / zoom);
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

	@Override
	protected void finalize() throws Throwable {
		peer.dispose();
		super.finalize();
	}
}
