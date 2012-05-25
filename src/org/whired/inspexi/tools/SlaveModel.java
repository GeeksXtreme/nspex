package org.whired.inspexi.tools;

import org.whired.inspexi.master.ImageConsumer;

public interface SlaveModel extends ImageConsumer {
	void requestThumbnail(final String filePath);

	void requestChildFiles(final String parentPath);

	void setView(final SlaveView view);
}
