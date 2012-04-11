package org.whired.inspexi.master;

/**
 * Listens for events fired by a view
 * 
 * @author Whired
 */
public interface ControllerEventListener {
	/**
	 * Requests a connect to the specified ips
	 * 
	 * @param ips the ips to connect to
	 */
	void connect(String[] ips);

	/**
	 * Requests a rebuild from the specified ips
	 * 
	 * @param ips the ips to request to rebuild
	 */
	void rebuild(String[] ips);

	/**
	 * Requests an information update from the specified ips
	 * 
	 * @param ips the ips to request information from
	 */
	void refresh(String[] ips);
}
