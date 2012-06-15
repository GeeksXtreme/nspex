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
 * A native windows implementation of a robot
 * @author Whired
 */
public class WinRobot extends Robot {
	private static final HWND desktop = User32.INSTANCE.GetDesktopWindow();
	private final HDC hdcWindow = User32.INSTANCE.GetDC(desktop);
	private final HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);
	private final RECT wBounds = new RECT();
	private final BufferedImage unscaled = new BufferedImage(getCaptureBounds().width, getCaptureBounds().height, BufferedImage.TYPE_INT_RGB);
	private final int[] unscaledPix = ((DataBufferInt) unscaled.getRaster().getDataBuffer()).getData();
	private final Dimension targetSize = new Dimension(scale(getCaptureBounds().width), scale(getCaptureBounds().height));
	private final HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, getCaptureBounds().width, getCaptureBounds().height);
	private final BITMAPINFO bmi = new BITMAPINFO();
	private final Memory buffer = new Memory(getCaptureBounds().width * getCaptureBounds().height * 4);

	/**
	 * Creates a new windows robot with the specified capture bound and target bounds
	 * @param captureBounds the bounds to capture
	 * @param targetBounds the bounds to scale to
	 */
	public WinRobot(final Rectangle captureBounds, final Dimension targetBounds) {
		this(captureBounds, Robot.calculateZoom(captureBounds, targetBounds));
	}

	/**
	 * Creates a new windows robot with a fullscreen capture bounds and the specified target bounds
	 * @param targetBounds the bounds to scale to
	 */
	public WinRobot(final Dimension targetBounds) {
		this(Robot.getScreenBounds(), targetBounds);
	}

	/**
	 * Creates a new windows robot with the specified bounds bounds and zoom
	 * @param captureBounds the bounds to capture pixels from
	 * @param zoom the zoom to scale to (0.0-1)
	 */
	public WinRobot(final Rectangle captureBounds, final double zoom) {
		super(captureBounds, zoom);
		wBounds.right = getCaptureBounds().x + getCaptureBounds().width;
		wBounds.left = getCaptureBounds().x;
		wBounds.top = getCaptureBounds().y;
		wBounds.bottom = getCaptureBounds().y + getCaptureBounds().height;
		GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
		bmi.bmiHeader.biWidth = getCaptureBounds().width;
		bmi.bmiHeader.biHeight = -getCaptureBounds().height;
		bmi.bmiHeader.biPlanes = 1;
		bmi.bmiHeader.biBitCount = 32;
		bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
	}

	/**
	 * Creates a new windows robot with fullscreen capture bounds and specified zoom
	 * @param zoom the zoom to scale to (0.0-1)
	 */
	public WinRobot(final double zoom) {
		this(null, zoom);
	}

	/**
	 * Creates a new windows robot with fullscreen capture bounds and 100% zoom
	 */
	public WinRobot() {
		this(1D);
	}

	@Override
	public byte[] getPixels() {
		// Copy native
		GDI32.INSTANCE.BitBlt(hdcMemDC, 0, 0, getCaptureBounds().width, getCaptureBounds().height, hdcWindow, 0, 0, GDI32.SRCCOPY);
		GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, getCaptureBounds().height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

		// Copy non-native
		final int[] toCompress = buffer.getIntArray(0, getCaptureBounds().width * getCaptureBounds().height);
		System.arraycopy(toCompress, 0, unscaledPix, 0, toCompress.length);

		// Compress
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
		// Release native resources
		try {
			GDI32.INSTANCE.DeleteDC(hdcMemDC);
		}
		catch (Throwable t1) {
			t1.printStackTrace();
		}
		try {
			User32.INSTANCE.ReleaseDC(desktop, hdcWindow);
		}
		catch (Throwable t2) {
			t2.printStackTrace();
		}
		try {
			GDI32.INSTANCE.DeleteObject(hBitmap);
		}
		catch (Throwable t3) {
			t3.printStackTrace();
		}
		super.finalize();
	}
}
