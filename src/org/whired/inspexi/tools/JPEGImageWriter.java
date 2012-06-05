package org.whired.inspexi.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;

public class JPEGImageWriter {
	private static final JPEGImageWriteParam iwparam = new JPEGImageWriteParam(new Locale("en"));
	private static ImageWriter writer;
	private static final ByteArrayOutputStream bos = new ByteArrayOutputStream();
	static {
		iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		iwparam.setCompressionQuality(.8F);
		final Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpg");
		if (iter.hasNext()) {
			writer = iter.next();
		}
		try {
			writer.setOutput(ImageIO.createImageOutputStream(bos));
		}
		catch (final IOException e1) {
			e1.printStackTrace();
		}
	}

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
				bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
			}
			final Graphics2D g2 = bufferedImage.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.drawImage(image, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), Color.BLACK, null);
			try {
				writer.write(null, new IIOImage(bufferedImage, null, null), iwparam);
				return bos.toByteArray();
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

	public synchronized static byte[] getImageBytes(final BufferedImage image) {
		return getImageBytes(image, null);
	}
}
