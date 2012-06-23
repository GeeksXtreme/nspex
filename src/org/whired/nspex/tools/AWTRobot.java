package org.whired.nspex.tools;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.peer.MouseInfoPeer;
import java.awt.peer.RobotPeer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import sun.awt.ComponentFactory;

/**
 * A lower level AWT robot
 * @author Whired
 */
public final class AWTRobot extends Robot {
	/** The unscaled image */
	private final BufferedImage unscaled;
	/** The unscaled pixels backing {@link #unscaled} */
	private final int[] unscaledPix;
	/** The target size to scale to */
	private final Dimension targetSize;
	/** The default screen device */
	private static final GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	private Method getRGBPixelsMethod;
	private final RobotPeer peer;
	private static boolean hasMouseInfoPeer;
	private static MouseInfoPeer mouseInfoPeer;

	/**
	 * Creates a new awt robot with the specified capture bounds and desired zoom level
	 * @param bounds the bounds to capture
	 * @param zoom the zoom to scale to, between .1 and 1.0 inclusive
	 * @throws AWTException if the robot can't be created
	 */
	public AWTRobot(final Rectangle bounds, final double zoom) throws AWTException {
		super(bounds, zoom);
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		peer = ((ComponentFactory) toolkit).createRobot(null, device);
		final Class<?> peerClass = peer.getClass();
		Method method = null;
		int methodType = -1;
		Object methodParam = null;
		try {
			method = peerClass.getDeclaredMethod("getRGBPixels", new Class<?>[] { Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, int[].class });
			methodType = 0;
		}
		catch (final Exception ex) {
		}
		if (methodType < 0) {
			try {
				method = peerClass.getDeclaredMethod("getScreenPixels", new Class<?>[] { Rectangle.class, int[].class });
				methodType = 1;
			}
			catch (final Exception ex) {
			}
		}

		if (methodType < 0) {
			try {
				method = peerClass.getDeclaredMethod("getScreenPixels", new Class<?>[] { Integer.TYPE, Rectangle.class, int[].class });
				methodType = 2;
				final GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
				final int count = devices.length;
				for (int i = 0; i != count; ++i) {
					if (device.equals(devices[i])) {
						methodParam = Integer.valueOf(i);
						break;
					}
				}

			}
			catch (final Exception ex) {
			}
		}

		if (methodType < 0) {
			try {
				method = peerClass.getDeclaredMethod("getRGBPixelsImpl", new Class<?>[] { Class.forName("sun.awt.X11GraphicsConfig"), Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, int[].class });
				methodType = 3;
				final Field field = peerClass.getDeclaredField("xgc");
				try {
					field.setAccessible(true);
					methodParam = field.get(peer);
				}
				finally {
					field.setAccessible(false);
				}
			}
			catch (final Exception ex) {
			}
		}

		if (methodType >= 0 && method != null && (methodType <= 1 || methodParam != null)) {
			getRGBPixelsMethod = method;
		}
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

	public static GraphicsDevice getMouseInfo(final Point point) {
		if (!hasMouseInfoPeer) {
			hasMouseInfoPeer = true;
			try {
				final Toolkit toolkit = Toolkit.getDefaultToolkit();
				final Method method = toolkit.getClass().getDeclaredMethod("getMouseInfoPeer", new Class<?>[0]);
				try {
					method.setAccessible(true);
					mouseInfoPeer = (MouseInfoPeer) method.invoke(toolkit, new Object[0]);
				}
				finally {
					method.setAccessible(false);
				}
			}
			catch (final Exception ex) {
			}
		}
		if (mouseInfoPeer != null) {
			final int device = mouseInfoPeer.fillPointWithCoords(point != null ? point : new Point());
			final GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
			return devices[device];
		}
		final PointerInfo info = MouseInfo.getPointerInfo();
		if (point != null) {
			final Point location = info.getLocation();
			point.x = location.x;
			point.y = location.y;
		}
		return info.getDevice();
	}

	/**
	 * Gets the number of mouse buttons, as specified by {@link MouseInfo#getNumberOfButtons()}
	 */
	public static int getNumberOfMouseButtons() {
		return MouseInfo.getNumberOfButtons();
	}

	/**
	 * Gets the default screen device, as specified by {@link GraphicsEnvironment#getDefaultScreenDevice()}
	 */
	public static GraphicsDevice getDefaultScreenDevice() {
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	}

	public static GraphicsDevice getScreenDevice() {
		return getMouseInfo(null);
	}

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
	public void mousePress(final int buttons) {
		peer.mousePress(buttons);
	}

	/**
	 * Releases the specified mouse buttons
	 * @param buttons the (OR'd) buttons to release
	 */
	public void mouseRelease(final int buttons) {
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
	 * Gets the RGB value of the pixel at the specified x and y coordinates
	 * @param x the x-coordinate of the pixel
	 * @param y the y-coordinate of the pixel
	 * @return the RGB value
	 */
	public int getRGBPixel(final int x, final int y) {
		return peer.getRGBPixel(x, y);
	}

	/**
	 * Gets the RGB values of the pixels within the specified rectangle
	 * @param bounds the bounds of the pixels whose values to get
	 * @return the values
	 */
	public int[] getRGBPixels(final Rectangle bounds) {
		return peer.getRGBPixels(bounds);
	}

	@Override
	public byte[] getPixels() {
		final int[] pix = peer.getRGBPixels(getCaptureBounds());
		System.arraycopy(pix, 0, unscaledPix, 0, unscaledPix.length);
		return JPEGImageWriter.getImageBytes(unscaled, targetSize);
	}

	/**
	 * Disposes of resources
	 */
	private final void dispose() {
		final Method method = getRGBPixelsMethod;
		if (method != null) {
			getRGBPixelsMethod = null;
			try {
				method.setAccessible(false);
			}
			catch (final Exception ex) {
			}
		}
		// Using reflection now because of some peers not having ANY support at all (1.5)
		try {
			peer.getClass().getDeclaredMethod("dispose", new Class<?>[0]).invoke(peer, new Class<?>[0]);
		}
		catch (final Exception ex) {
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			dispose();
		}
		finally {
			super.finalize();
		}
	}
}