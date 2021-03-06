package org.whired.nspex.master;

import java.util.logging.Level;

/**
 * Listens for authentication events
 * @author Whired
 */
public interface AuthenticationListener {
	/**
	 * Invoked when the list of slaves are received
	 * @param slaves the slaves that were received
	 */
	void slavesReceived(final RemoteSlave[] slaves);

	/**
	 * Invoked when a remote message has been logged
	 * @param level the level of the message
	 * @param message the message
	 */
	void remoteLogged(final Level level, final String message);

	/**
	 * Invoked when the client has been disconnected
	 */
	void disconnected();

	/**
	 * Invoked when a session id is received from the auth server
	 * @param lsessionId the session id that was received
	 */
	void sessionIDReceived(String lsessionId);

	/**
	 * Invoked when a user must decide to allow an ISP change
	 * @param timeout the limitation on how often an ISP can change, in MS
	 */
	void promptISPChange(long timeout);
}
