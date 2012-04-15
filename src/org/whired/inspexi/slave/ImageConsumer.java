package org.whired.inspexi.slave;

public interface ImageConsumer {
	void imageProduced(ImageConsumer target, byte[] image);
}
