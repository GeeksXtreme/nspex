package org.whired.inspexi.master;

import org.whired.inspexi.tools.Slave;

/**
 * Listens for events fired by a view
 * @author Whired
 */
public interface ControllerEventListener {
	/**
	 * Requests a connect to the specified slaves
	 * @param slaves the slaves to connect to
	 */
	void connect(Slave[] slaves);

	/**
	 * Requests a rebuild from the specified slaves
	 * @param slaves the slaves to rebuild
	 */
	void rebuild(Slave[] slaves);

	/**
	 * Requests an information update from the specified slaves
	 * @param slaves the slaves to update
	 */
	void refresh(Slave[] slaves);
}
