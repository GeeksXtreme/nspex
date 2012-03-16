package org.whired.inspexi.tools;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.util.Hashtable;

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

public class JNARobot implements Robot {

	private static final double ZOOM = .70f;
	private final Rectangle bounds;
	private final HWND desktop = User32.INSTANCE.GetDesktopWindow();
	private final HDC hdcWindow;
	private final HDC hdcMemDC;
	private final RECT wBounds = new RECT();
	private final BufferedImage unscaled;
	private final int[] unscaledPix;
	private final BufferedImage scaled;
	private final int[] scaledPix;
	private final Graphics2D graphics;
	private final HBITMAP hBitmap;
	private final BITMAPINFO bmi = new BITMAPINFO();
	private final Memory buffer;

	public JNARobot(Rectangle bounds) {
		if (bounds == null) {
			bounds = getScreenBounds();
		}
		this.bounds = bounds;
		hdcWindow = User32.INSTANCE.GetDC(desktop);
		hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);
		wBounds.right = bounds.x + bounds.width;
		wBounds.left = bounds.x;
		wBounds.top = bounds.y;
		wBounds.bottom = bounds.y + bounds.height;
		unscaled = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
		unscaledPix = ((DataBufferInt) unscaled.getRaster().getDataBuffer()).getData();
		scaled = new BufferedImage(getZoom(bounds.width), getZoom(bounds.height), BufferedImage.TYPE_INT_RGB);
		scaledPix = ((DataBufferInt) scaled.getRaster().getDataBuffer()).getData();
		graphics = scaled.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, bounds.width, bounds.height);
		GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
		bmi.bmiHeader.biWidth = bounds.width;
		bmi.bmiHeader.biHeight = -bounds.height;
		bmi.bmiHeader.biPlanes = 1;
		bmi.bmiHeader.biBitCount = 32;
		bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
		buffer = new Memory(bounds.width * bounds.height * 4);
	}

	public JNARobot() {
		this(null);
	}

	@Override
	public byte[] getBytePixels() {
		long start = System.currentTimeMillis();
		GDI32.INSTANCE.BitBlt(hdcMemDC, 0, 0, bounds.width, bounds.height, hdcWindow, 0, 0, GDI32.SRCCOPY);

		GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, bounds.height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

		int[] toCompress = buffer.getIntArray(0, bounds.width * bounds.height);
		System.arraycopy(toCompress, 0, unscaledPix, 0, toCompress.length);

		graphics.drawImage(unscaled, 0, 0, scaled.getWidth(), scaled.getHeight(), 0, 0, unscaled.getWidth(), unscaled.getHeight(), null);

		byte[] pixels = new byte[scaledPix.length];
		for (int i1 = 0; i1 < scaledPix.length; i1++) {
			int c = scaledPix[i1] & 0xFFFFFF;
			int r = (c >> 16 & 0xFF) / 36;
			int g = (c >> 8 & 0xFF) / 36;
			int b = (c & 0xFF) / 85;
			pixels[i1] = (byte) ((r << 5) + (g << 2) + b);
		}
		System.out.println(System.currentTimeMillis() - start);
		return pixels;
	}

	public static void main(String[] args) {
		JNARobot t = new JNARobot();
		for (int i = 0; i < 5; i++) {
			byte[] clientPix = t.getBytePixels();
			ColorModel cm = new DirectColorModel(8, 0xE0, 0x1C, 0x3);
			DataBufferByte dataBuffer = new DataBufferByte(clientPix, clientPix.length);
			final BufferedImage image = new BufferedImage(cm, Raster.createWritableRaster(cm.createCompatibleSampleModel(getZoom(t.bounds.width), getZoom(t.bounds.height)), dataBuffer, null), false, new Hashtable<Object, Object>());
			JFrame frame = new JFrame("Inspexi JNA");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setAlwaysOnTop(true);
			JPanel panel = new JPanel() {
				@Override
				public void paint(Graphics g) {
					Graphics2D g2 = (Graphics2D) g;
					g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
					g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
					g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
					g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
					g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
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

	@Override
	public Rectangle getScreenBounds() {
		RECT wBounds = new RECT();
		User32.INSTANCE.GetWindowRect(desktop, wBounds);
		return new Rectangle(wBounds.left, wBounds.top, wBounds.right - wBounds.left, wBounds.bottom - wBounds.top);
	}

	public static int getZoom(int orig) {
		return (int) (orig * ZOOM);
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
