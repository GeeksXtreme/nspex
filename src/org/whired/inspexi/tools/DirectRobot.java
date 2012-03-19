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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;

import sun.awt.ComponentFactory;

public final class DirectRobot extends Robot {
	private int detail = LOW;
	public static final int LOWEST = 0, LOWER = 1, LOW = 2, GREYSCALE = 3;
	private final JPEGImageWriteParam iwparam = new JPEGImageWriteParam(new Locale("en"));
	private ImageWriter writer;
	private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
	private final BufferedImage unscaled;
	private final int[] unscaledPix;
	private final BufferedImage scaled;
	private final Graphics2D graphics;

	public void setDetail(int detail) {
		this.detail = detail;
	}

	@Override
	public Rectangle getScreenBounds() {
		DisplayMode dm = device.getDisplayMode();
		return new Rectangle(0, 0, dm.getWidth(), dm.getHeight());
	}

	public DirectRobot(Rectangle bounds, double zoom) throws AWTException {
		super(bounds, zoom);
		iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		iwparam.setCompressionQuality(.8F);
		Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpg");
		if (iter.hasNext()) {
			writer = iter.next();
		}
		try {
			writer.setOutput(ImageIO.createImageOutputStream(bos));
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
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
		unscaled = new BufferedImage(getBounds().width, getBounds().height, BufferedImage.TYPE_INT_RGB);
		unscaledPix = ((DataBufferInt) unscaled.getRaster().getDataBuffer()).getData();
		scaled = new BufferedImage(getZoom(getBounds().width), getZoom(getBounds().height), BufferedImage.TYPE_INT_RGB);
		graphics = scaled.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	}

	public DirectRobot(double zoom) throws AWTException {
		this(null, zoom);
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
		int[] pix = peer.getRGBPixels(getBounds());
		byte[] pixels = new byte[pix.length];
		switch (getDetail()) {
		case LOWEST:
			for (int i = 0; i < pix.length; i++/* i += 2 */) {
				pixels[i] = (byte) -110;
				if ((i - 1) % getBounds().width == 0 || i % getBounds().width == 0) {
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
			System.arraycopy(pix, 0, unscaledPix, 0, unscaledPix.length);
			graphics.drawImage(unscaled, 0, 0, scaled.getWidth(), scaled.getHeight(), 0, 0, unscaled.getWidth(), unscaled.getHeight(), null);

			try {
				writer.write(null, new IIOImage(scaled, null, null), iwparam);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			byte[] toReturn = bos.toByteArray();
			bos.reset();
			return toReturn;
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
	private static final GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	private Method getRGBPixelsMethod;
	private final RobotPeer peer;
	private static boolean hasMouseInfoPeer;
	private static MouseInfoPeer mouseInfoPeer;
}