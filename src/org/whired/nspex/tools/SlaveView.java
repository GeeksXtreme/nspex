package org.whired.nspex.tools;

import java.awt.Dimension;

import org.whired.nspex.master.ImageConsumer;

/**
 * A view for a slave
 * @author Whired
 */
public interface SlaveView extends ImageConsumer {
	/**
	 * Sets the selected file for a remote slave
	 * @param file the file to set
	 */
	void setFile(final RemoteFile file);

	/**
	 * Used for displaying a remote slave's filesystem
	 * @param fs the file separator char
	 * @param parentPath the parent folder for these files
	 * @param childFiles the files to add
	 */
	void addChildFiles(final char fs, final String parentPath, RemoteFile[] childFiles);

	/**
	 * Gets the preferred thumb size that the remote slave will scale to
	 * @return the preferred thumb size
	 */
	Dimension getThumbSize();

	/**
	 * Invoked when a slave is disconnected
	 * @param slave the slave that has disconnected
	 */
	void disconnected(Slave slave);

	/**
	 * Invoked when a slave has connected
	 * @param slave the slave that has connected
	 */
	void connected(Slave slave);

	/**
	 * Displays output to some console
	 * @param output the output to display
	 */
	void displayOutput(String output);
}
