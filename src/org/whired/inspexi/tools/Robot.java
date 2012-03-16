package org.whired.inspexi.tools;

import java.awt.Rectangle;

public interface Robot {
	byte[] getBytePixels();

	Rectangle getScreenBounds();
}
