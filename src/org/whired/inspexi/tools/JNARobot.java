package org.whired.inspexi.tools;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
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

public class JNARobot extends Robot {

	private static final HWND desktop = User32.INSTANCE.GetDesktopWindow();
	private final HDC hdcWindow;
	private final HDC hdcMemDC;
	private final RECT wBounds = new RECT();
	private final BufferedImage unscaled;
	private final int[] unscaledPix;
	private final BufferedImage scaled;
	private final Graphics2D graphics;
	private final HBITMAP hBitmap;
	private final BITMAPINFO bmi = new BITMAPINFO();
	private final Memory buffer;
	private final JPEGImageWriteParam iwparam = new JPEGImageWriteParam(new Locale("en"));
	private ImageWriter writer;
	private final ByteArrayOutputStream bos = new ByteArrayOutputStream();

	public JNARobot(Rectangle bounds, double zoom) {
		super(bounds, zoom);
		hdcWindow = User32.INSTANCE.GetDC(desktop);
		hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);
		wBounds.right = getBounds().x + getBounds().width;
		wBounds.left = getBounds().x;
		wBounds.top = getBounds().y;
		wBounds.bottom = getBounds().y + getBounds().height;
		unscaled = new BufferedImage(getBounds().width, getBounds().height, BufferedImage.TYPE_INT_RGB);
		unscaledPix = ((DataBufferInt) unscaled.getRaster().getDataBuffer()).getData();
		scaled = new BufferedImage(getZoom(getBounds().width), getZoom(getBounds().height), BufferedImage.TYPE_INT_RGB);
		graphics = scaled.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, getBounds().width, getBounds().height);
		GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
		bmi.bmiHeader.biWidth = getBounds().width;
		bmi.bmiHeader.biHeight = -getBounds().height;
		bmi.bmiHeader.biPlanes = 1;
		bmi.bmiHeader.biBitCount = 32;
		bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
		buffer = new Memory(getBounds().width * getBounds().height * 4);
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
	}

	public JNARobot(double zoom) {
		this(null, zoom);
	}

	@Override
	public byte[] getBytePixels() {
		GDI32.INSTANCE.BitBlt(hdcMemDC, 0, 0, getBounds().width, getBounds().height, hdcWindow, 0, 0, GDI32.SRCCOPY);
		GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, getBounds().height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

		int[] toCompress = buffer.getIntArray(0, getBounds().width * getBounds().height);
		System.arraycopy(toCompress, 0, unscaledPix, 0, toCompress.length);

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
	}

	public static void main(String[] args) throws IOException {
		JNARobot t = new JNARobot(.7D);
		for (int i = 0; i < 5; i++) {
			byte[] compressed = t.getBytePixels();
			final BufferedImage image = ImageIO.read(new ByteArrayInputStream(compressed));
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
