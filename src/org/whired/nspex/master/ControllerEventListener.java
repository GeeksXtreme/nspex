package org.whired.nspex.master;

/**
 * Listens for events fired by a view
 * @author Whired
 */
public interface ControllerEventListener {
	/**
	 * Requests a connect to the specified slaves
	 * @param slaves the slaves to connect to
	 */
	void connect(RemoteSlave[] slaves);

	/**
	 * Requests a rebuild from the specified slaves
	 * @param slaves the slaves to rebuild
	 */
	void rebuild(RemoteSlave[] slaves);

	/**
	 * Requests an information update from the specified slaves
	 * @param slaves the slaves to update
	 */
	void refresh(RemoteSlave[] slaves);

	/**
	 * Sets the current progress of some task
	 * @param progress the progress to set
	 */
	void setProgress(int progress);

	/**
	 * Sets the max progress of some task
	 * @param max the max progress to set
	 */
	void setMaxProgress(int max);
}
