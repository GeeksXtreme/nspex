package org.whired.inspexi.tools;

import java.awt.Dimension;
import java.awt.Image;

import org.whired.inspexi.master.ImageConsumer;

public interface SlaveView extends ImageConsumer {
	void setThumbnail(final Image thumb);

	void addChildFiles(final String parentPath, RemoteFile[] childFiles);

	Dimension getThumbSize();

	void disconnected(Slave slave);

	void connected(Slave slave);
}
