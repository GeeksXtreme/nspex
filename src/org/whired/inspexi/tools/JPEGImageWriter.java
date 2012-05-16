package org.whired.inspexi.tools;

import java.awt.image.RenderedImage;
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

	public static byte[] getImageBytes(RenderedImage i) throws IOException {
		writer.write(null, new IIOImage(i, null, null), iwparam);
		final byte[] toReturn = bos.toByteArray();
		bos.reset();
		return toReturn;
	}
}
