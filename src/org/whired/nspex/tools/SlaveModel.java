package org.whired.nspex.tools;

import java.io.IOException;

import org.whired.nspex.master.ImageConsumer;

/**
 * An abstract model for a slave
 * @author Whired
 */
public interface SlaveModel extends ImageConsumer {
	/**
	 * Invoked when an action is requested for the specified file path
	 * @param action the action to perform
	 * @param filePath the path of the file on the slave machine
	 * @throws IOException
	 */
	void requestFileAction(final int action, final String filePath) throws IOException;

	/**
	 * Requests a list of children files of the specified directory
	 * @param parentPath the path of the parent directory on the slave machine
	 * @throws IOException
	 */
	void requestChildFiles(final String parentPath) throws IOException;

	/**
	 * Invoked when a view is set for this model
	 * @param view the view to set
	 */
	void setView(final SlaveView view);
}
