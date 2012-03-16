package org.whired.inspexi.tools;

import java.awt.AWTException;
import java.awt.DisplayMode;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.peer.MouseInfoPeer;
import java.awt.peer.RobotPeer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.whired.inspexi.net.Slave;

import sun.awt.ComponentFactory;

public final class DirectRobot implements Robot {
	private int detail = LOW;
	public static final int LOWEST = 0, LOWER = 1, LOW = 2, GREYSCALE = 3;
	private final Rectangle bounds;

	public void setDetail(int detail) {
		this.detail = detail;
	}

	@Override
	public Rectangle getScreenBounds() {
		DisplayMode dm = device.getDisplayMode();
		return new Rectangle(0, 0, dm.getWidth(), dm.getHeight());
	}

	public DirectRobot() throws AWTException {
		this(null);
	}

	public DirectRobot(Rectangle bounds) throws AWTException {
		if (bounds == null) {
			bounds = getScreenBounds();
		}
		this.bounds = bounds;
		this.device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		peer = ((ComponentFactory) toolkit).createRobot(null, device);
		Class<?> peerClass = peer.getClass();
		System.out.println("Class name: " + peerClass.getName());
		Method method = null;
		int methodType = -1;
		Object methodParam = null;
		try {
			method = peerClass.getDeclaredMethod("getRGBPixels", new Class<?>[] { Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, int[].class });
			methodType = 0;
		}
		catch (Exception ex) {
		}
		if (methodType < 0) {
			try {
				method = peerClass.getDeclaredMethod("getScreenPixels", new Class<?>[] { Rectangle.class, int[].class });
				methodType = 1;
			}
			catch (Exception ex) {
			}
		}

		if (methodType < 0) {
			try {
				method = peerClass.getDeclaredMethod("getScreenPixels", new Class<?>[] { Integer.TYPE, Rectangle.class, int[].class });
				methodType = 2;
				GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
				int count = devices.length;
				for (int i = 0; i != count; ++i) {
					if (device.equals(devices[i])) {
						methodParam = Integer.valueOf(i);
						break;
					}
				}

			}
			catch (Exception ex) {
			}
		}

		if (methodType < 0) {
			try {
				method = peerClass.getDeclaredMethod("getRGBPixelsImpl", new Class<?>[] { Class.forName("sun.awt.X11GraphicsConfig"), Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, int[].class });
				methodType = 3;
				Field field = peerClass.getDeclaredField("xgc");
				try {
					field.setAccessible(true);
					methodParam = field.get(peer);
				}
				finally {
					field.setAccessible(false);
				}
			}
			catch (Exception ex) {
			}
		}

		if (methodType >= 0 && method != null && (methodType <= 1 || methodParam != null)) {
			getRGBPixelsMethod = method;
			getRGBPixelsMethodType = methodType;
			getRGBPixelsMethodParam = methodParam;
		}
		else {
			System.out.println("WARNING: Failed to acquire direct method for grabbing pixels, please post this on the main thread!");
			System.out.println();
			System.out.println(peer.getClass().getName());
			System.out.println();
			try {
				Method[] methods = peer.getClass().getDeclaredMethods();
				for (Method method1 : methods) {
					System.out.println(method1);
				}

			}
			catch (Exception ex) {
			}
			System.out.println();
		}
	}

	public static GraphicsDevice getMouseInfo(Point point) {
		if (!hasMouseInfoPeer) {
			hasMouseInfoPeer = true;
			try {
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Method method = toolkit.getClass().getDeclaredMethod("getMouseInfoPeer", new Class<?>[0]);
				try {
					method.setAccessible(true);
					mouseInfoPeer = (MouseInfoPeer) method.invoke(toolkit, new Object[0]);
				}
				finally {
					method.setAccessible(false);
				}
			}
			catch (Exception ex) {
			}
		}
		if (mouseInfoPeer != null) {
			int device = mouseInfoPeer.fillPointWithCoords(point != null ? point : new Point());
			GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
			return devices[device];
		}
		PointerInfo info = MouseInfo.getPointerInfo();
		if (point != null) {
			Point location = info.getLocation();
			point.x = location.x;
			point.y = location.y;
		}
		return info.getDevice();
	}

	public static int getNumberOfMouseButtons() {
		return MouseInfo.getNumberOfButtons();
	}

	public static GraphicsDevice getDefaultScreenDevice() {
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	}

	public static GraphicsDevice getScreenDevice() {
		return getMouseInfo(null);
	}

	public void mouseMove(int x, int y) {
		peer.mouseMove(x, y);
	}

	public void mousePress(int buttons) {
		peer.mousePress(buttons);
	}

	public void mouseRelease(int buttons) {
		peer.mouseRelease(buttons);
	}

	public void mouseWheel(int wheelAmt) {
		peer.mouseWheel(wheelAmt);
	}

	public void keyPress(int keycode) {
		peer.keyPress(keycode);
	}

	public void keyRelease(int keycode) {
		peer.keyRelease(keycode);
	}

	public int getRGBPixel(int x, int y) {
		return peer.getRGBPixel(x, y);
	}

	public int[] getRGBPixels(Rectangle bounds) {
		return peer.getRGBPixels(bounds);
	}

	@Override
	public byte[] getBytePixels() {
		int[] pix = peer.getRGBPixels(bounds);
		byte[] pixels = new byte[pix.length];
		switch (getDetail()) {
		case LOWEST:
			for (int i = 0; i < pix.length; i++/* i += 2 */) {
				pixels[i] = (byte) -110;
				if ((i - 1) % bounds.width == 0 || i % bounds.width == 0) {
					i++;
				}
				if (i > pix.length - 1) {
					i = pix.length - 1;
				}
				i++;
				int c = pix[i];
				int r = (c >> 16 & 0xFF) / 36;
				int g = (c >> 8 & 0xFF) / 36;
				int b = (c & 0xFF) / 85;
				pixels[i] = (byte) ((r << 5) + (g << 2) + b);
			}
			return pixels;
		case LOWER:
			for (int i = 0; i < pix.length; i++) {
				int c = pix[i];
				int r = (c >> 16 & 0xFF) / 36;
				int g = (c >> 8 & 0xFF) / 36;
				int b = (c & 0xFF) / 85;
				byte col = (byte) ((r << 5) + (g << 2) + b);
				if (pix.length - i > 2) {
					pixels[i] = col;
					pixels[++i] = col;
				}

			}
		break;
		case LOW:
			BufferedImage unscaled = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
			int[] tpix = ((DataBufferInt) unscaled.getRaster().getDataBuffer()).getData();
			System.arraycopy(pix, 0, tpix, 0, tpix.length);

			BufferedImage scaled = new BufferedImage(Slave.getZoom(bounds.width), Slave.getZoom(bounds.height), BufferedImage.TYPE_INT_RGB);
			Graphics2D gr = scaled.createGraphics();

			gr.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			gr.drawImage(unscaled, 0, 0, scaled.getWidth(), scaled.getHeight(), 0, 0, unscaled.getWidth(), unscaled.getHeight(), null);
			gr.dispose();

			int[] npix = ((DataBufferInt) scaled.getRaster().getDataBuffer()).getData();
			pixels = new byte[npix.length];
			for (int i = 0; i < npix.length; i++) {
				int c = npix[i];
				int r = (c >> 16 & 0xFF) / 36;
				int g = (c >> 8 & 0xFF) / 36;
				int b = (c & 0xFF) / 85;
				pixels[i] = (byte) ((r << 5) + (g << 2) + b);
			}
			return pixels;
		case GREYSCALE:
			for (int i = 0; i < pix.length; i++) {
				pixels[i] = (byte) pix[i];
			}
		break;
		}
		return new byte[0];
	}

	public boolean getRGBPixels(int x, int y, int width, int height, int[] pixels) {
		if (getRGBPixelsMethod != null) {
			try {
				if (getRGBPixelsMethodType == 0) {
					getRGBPixelsMethod.invoke(peer, new Object[] { Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(width), Integer.valueOf(height), pixels });
				}
				else if (getRGBPixelsMethodType == 1) {
					getRGBPixelsMethod.invoke(peer, new Object[] { new Rectangle(x, y, width, height), pixels });
				}
				else if (getRGBPixelsMethodType == 2) {
					getRGBPixelsMethod.invoke(peer, new Object[] { getRGBPixelsMethodParam, new Rectangle(x, y, width, height), pixels });
				}
				else {
					getRGBPixelsMethod.invoke(peer, new Object[] { getRGBPixelsMethodParam, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(width), Integer.valueOf(height), pixels });
				}

				return true;
			}
			catch (Exception ex) {
			}
		}

		int[] tmp = getRGBPixels(new Rectangle(x, y, width, height));
		System.arraycopy(tmp, 0, pixels, 0, width * height);
		return false;
	}

	public void dispose() {
		getRGBPixelsMethodParam = null;
		Method method = getRGBPixelsMethod;
		if (method != null) {
			getRGBPixelsMethod = null;
			try {
				method.setAccessible(false);
			}
			catch (Exception ex) {
			}
		}
		// Using reflection now because of some peers not having ANY support at all (1.5)
		try {
			peer.getClass().getDeclaredMethod("dispose", new Class<?>[0]).invoke(peer, new Class<?>[0]);
		}
		catch (Exception ex) {
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

	public int getDetail() {
		return detail;
	}

	private Object getRGBPixelsMethodParam;
	private int getRGBPixelsMethodType;
	public final GraphicsDevice device;
	private Method getRGBPixelsMethod;
	private final RobotPeer peer;
	private static boolean hasMouseInfoPeer;
	private static MouseInfoPeer mouseInfoPeer;
}