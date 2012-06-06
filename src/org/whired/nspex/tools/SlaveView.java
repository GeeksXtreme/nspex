package org.whired.nspex.tools;

import java.awt.Dimension;
import java.awt.Image;

import org.whired.nspex.master.ImageConsumer;

public interface SlaveView extends ImageConsumer {
	void setThumbnail(final Image thumb);

	void addChildFiles(final char fs, final String parentPath, RemoteFile[] childFiles);

	Dimension getThumbSize();

	void disconnected(Slave slave);

	void connected(Slave slave);
}
