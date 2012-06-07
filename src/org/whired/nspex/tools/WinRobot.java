package org.whired.nspex.tools;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

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
	private final Dimension targetSize = new Dimension(getZoom(getBounds().width), getZoom(getBounds().height));
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
		// Copy native
		GDI32.INSTANCE.BitBlt(hdcMemDC, 0, 0, getBounds().width, getBounds().height, hdcWindow, 0, 0, GDI32.SRCCOPY);
		GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, getBounds().height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

		// Nonnative copy
		final int[] toCompress = buffer.getIntArray(0, getBounds().width * getBounds().height);
		System.arraycopy(toCompress, 0, unscaledPix, 0, toCompress.length);

		// Scale and compress
		return JPEGImageWriter.getImageBytes(unscaled, targetSize);
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
