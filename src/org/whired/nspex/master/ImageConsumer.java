package org.whired.nspex.master;

import java.awt.Image;

public interface ImageConsumer {
	void imageProduced(Image image);

	void imageResized(int width, int height);
}
