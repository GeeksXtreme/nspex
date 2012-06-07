package org.whired.nspex.tools;

import java.io.IOException;

import org.whired.nspex.master.ImageConsumer;

public interface SlaveModel extends ImageConsumer {
	void requestThumbnail(final String filePath) throws IOException;

	void requestChildFiles(final String parentPath) throws IOException;

	void setView(final SlaveView view);
}
