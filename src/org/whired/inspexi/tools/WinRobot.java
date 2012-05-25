package org.whired.inspexi.tools;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;

/**
 * A native windows implementation of a Robot
 * @author Whired
 */
public class WinRobot extends Robot {

	private static final HWND desktop = User32.INSTANCE.GetDesktopWindow();
	private final HDC hdcWindow = User32.INSTANCE.GetDC(desktop);
	private final HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);
	private final RECT wBounds = new RECT();
	private final BufferedImage unscaled = new BufferedImage(getBounds().width, getBounds().height, BufferedImage.TYPE_INT_RGB);
	private final int[] unscaledPix = ((DataBufferInt) unscaled.getRaster().getDataBuffer()).getData();
	private final BufferedImage scaled = new BufferedImage(getZoom(getBounds().width), getZoom(getBounds().height), BufferedImage.TYPE_INT_RGB);
	private final Graphics2D graphics = scaled.createGraphics();
	private final HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, getBounds().width, getBounds().height);
	private final BITMAPINFO bmi = new BITMAPINFO();
	private final Memory buffer = new Memory(getBounds().width * getBounds().height * 4);

	public WinRobot(final Rectangle captureBounds, final Dimension produceBounds) {
		this(captureBounds, Robot.calculateZoom(captureBounds, produceBounds));
	}

	public WinRobot(final Dimension produceBounds) {
		this(Robot.getScreenBounds(), produceBounds);
	}

	/**
	 * Creates a new windows robot with the specified bounds and zoom
	 * @param bounds the bounds to capture pixels from
	 * @param zoom original bounds will be scaled by this value
	 */
	public WinRobot(final Rectangle bounds, final double zoom) {
		super(bounds, zoom);
		wBounds.right = getBounds().x + getBounds().width;
		wBounds.left = getBounds().x;
		wBounds.top = getBounds().y;
		wBounds.bottom = getBounds().y + getBounds().height;
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
		bmi.bmiHeader.biWidth = getBounds().width;
		bmi.bmiHeader.biHeight = -getBounds().height;
		bmi.bmiHeader.biPlanes = 1;
		bmi.bmiHeader.biBitCount = 32;
		bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
	}

	/**
	 * Creates a new windows robot with the default (screen size) bounds and specified zoom
	 * @param zoom original bounds will be scaled by this value
	 */
	public WinRobot(final double zoom) {
		this(null, zoom);
	}

	@Override
	public byte[] getBytePixels() {
		GDI32.INSTANCE.BitBlt(hdcMemDC, 0, 0, getBounds().width, getBounds().height, hdcWindow, 0, 0, GDI32.SRCCOPY);
		GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, getBounds().height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

		final int[] toCompress = buffer.getIntArray(0, getBounds().width * getBounds().height);
		System.arraycopy(toCompress, 0, unscaledPix, 0, toCompress.length);

		graphics.drawImage(unscaled, 0, 0, scaled.getWidth(), scaled.getHeight(), 0, 0, unscaled.getWidth(), unscaled.getHeight(), null);
		return JPEGImageWriter.getImageBytes(scaled);
	}

	public static void main(final String[] args) throws IOException {
		final WinRobot t = new WinRobot(new Dimension(600, 450));
		for (int i = 0; i < 5; i++) {
			final byte[] compressed = t.getBytePixels();
			System.out.println((compressed.length / 1024) + "kb");
			final BufferedImage image = ImageIO.read(new ByteArrayInputStream(compressed));
			final JFrame frame = new JFrame("Inspexi JNA");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setAlwaysOnTop(true);
			final JPanel panel = new JPanel() {
				@Override
				public void paint(final Graphics g) {
					final Graphics2D g2 = (Graphics2D) g;
					g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					g2.drawImage(image, 0, 0, this);
					g2.dispose();
					g.dispose();
				}
			};
			panel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
			frame.getContentPane().add(panel);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
		}
	}

	private interface GDI32 extends com.sun.jna.platform.win32.GDI32 {
		GDI32 INSTANCE = (GDI32) Native.loadLibrary(GDI32.class);

		boolean BitBlt(HDC hdcDest, int nXDest, int nYDest, int nWidth, int nHeight, HDC hdcSrc, int nXSrc, int nYSrc, int dwRop);

		int SRCCOPY = 0xCC0020;
	}

	private interface User32 extends com.sun.jna.platform.win32.User32 {
		User32 INSTANCE = (User32) Native.loadLibrary(User32.class);

		HWND GetDesktopWindow();
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			GDI32.INSTANCE.DeleteDC(hdcMemDC);
			User32.INSTANCE.ReleaseDC(desktop, hdcWindow);
			GDI32.INSTANCE.DeleteObject(hBitmap);
		}
		finally {
			super.finalize();
		}
	}
}
