package org.whired.nspex.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;

import org.whired.nspex.tools.logging.Log;

/**
 * A JPEG compressor and stream writer
 * @author Whired
 */
public class JPEGImageWriter {
	/** The format to write */
	private static final String FORMAT_NAME = "JPEG";
	/** The options for image writing */
	private static final JPEGImageWriteParam iwparam = new JPEGImageWriteParam(null);
	/** The image writer */
	private static ImageWriter writer;
	/** The byte stream to write to */
	private static final ByteArrayOutputStream bos = new ByteArrayOutputStream();
	static {
		// Set options
		iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		iwparam.setCompressionQuality(.65F);

		// Get writer
		try {
			writer = ImageIO.getImageWritersByFormatName(FORMAT_NAME).next();
		}
		catch (NoSuchElementException e) {
			throw new ExceptionInInitializerError("No available writer for format: " + FORMAT_NAME);
		}
		try {
			// Write to byte array stream
			writer.setOutput(ImageIO.createImageOutputStream(bos));
		}
		catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Scales, compresses, and gets the bytes of the specified image
	 * @param image the image to scale and compress
	 * @param targetSize the size to scale to
	 * @return the bytes, or {@code null} if the bytes could not be compressed
	 */
	public synchronized static byte[] getImageBytes(final BufferedImage image, final Dimension targetSize) {
		if (writer.getOriginatingProvider().canEncodeImage(image)) {
			BufferedImage bufferedImage;
			if (targetSize != null) {

				// Draw to scale
				final double tWidth = image.getWidth();
				final double tHeight = image.getHeight();

				final double nPercentW = targetSize.getWidth() / tWidth;
				final double nPercentH = targetSize.getHeight() / tHeight;

				int scaledWidth;
				int scaledHeight;
				if (nPercentH >= nPercentW) {
					scaledWidth = (int) (nPercentW * tWidth);
					scaledHeight = (int) (nPercentW * tHeight);
				}
				else {
					scaledWidth = (int) (nPercentH * tWidth);
					scaledHeight = (int) (nPercentH * tHeight);
				}

				bufferedImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
			}
			else {
				// No target size, scale 1:1
				bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
			}
			final Graphics2D g2 = bufferedImage.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.drawImage(image, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), Color.BLACK, null);
			try {
				// Write to bos
				writer.write(null, new IIOImage(bufferedImage, null, null), iwparam);

				// GZIP
				byte[] raw = bos.toByteArray();
				ByteArrayOutputStream gzos = new ByteArrayOutputStream();
				GZIPOutputStream gzo = new GZIPOutputStream(gzos) {
					{
						def.setLevel(Deflater.BEST_COMPRESSION);
					}
				};
				gzo.write(raw);
				gzo.close();
				byte[] compressed = gzos.toByteArray();

				Log.l.finest("Image compression: before=" + raw.length + " after=" + compressed.length);

				return compressed;
			}
			catch (final IOException e) {
				e.printStackTrace();
				return null;
			}
			finally {
				if (g2 != null) {
					g2.dispose();
				}
				bos.reset();
			}
		}
		else {
			return null;
		}
	}

	/**
	 * Gets the compressed bytes for the specified image
	 * @param image the image to compress and get the bytes of
	 * @return the bytes, or {@code null} if the image could not be compressed
	 */
	public static byte[] getImageBytes(final BufferedImage image) {
		return getImageBytes(image, null);
	}
}
