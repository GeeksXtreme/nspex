package org.whired.inspexi.tools;

import java.io.IOException;

import org.whired.inspexi.master.ImageConsumer;

public interface SlaveModel extends ImageConsumer {
	void requestThumbnail(final String filePath) throws IOException;

	void requestChildFiles(final String parentPath) throws IOException;

	void setView(final SlaveView view);
}
