package org.whired.nspex.blackbox;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.whired.nspex.tools.logging.Log;

/**
 * Manages sessions
 * @author Whired
 */
public class SessionManager {
	// The sessions
	private final Map<String, Session> sessions = new HashMap<String, Session>();
	// The last time sessions were pruned
	private long lastPruneTime = System.currentTimeMillis();
	// The amount of time that should pass before sessions are pruned
	private final static long TIME_BETWEEN_PRUNES_MS = TimeUnit.MINUTES.toMillis(10);
	// The amount of time that is allowed to pass before a session is considered invalid
	private final static long LIFESPAN_MS = TimeUnit.MINUTES.toMillis(30);

	/**
	 * Gets a session id for the specified ip. If no session exists, one is created.
	 * @param ip the ip to get a session id for
	 * @return the session id that was created
	 */
	public String getSessionId(final String ip) {
		// Prune old sessions if necessary
		pruneSessions();

		Session session = sessions.get(ip);
		if (session == null) {
			session = new Session(DigestUtils.sha256Hex(ip + (int) (Math.random() * Integer.MAX_VALUE)), LIFESPAN_MS);
			sessions.put(ip, session);
		}
		return session.getSessionId();
	}

	/**
	 * Checks whether or not an ip's session is valid
	 * @param ip the ip to check
	 * @param sessionId the session ip to check
	 * @return {@code true} if the session id matches, otherwise {@code false}
	 */
	public boolean sessionValid(final String ip, final String sessionId) {
		final Session session = sessions.get(ip);
		if (session != null && session.getSessionId().equals(sessionId)) { // Check that the session exists and matches
			if (!session.hasExpired()) {
				session.renew(); // Session is still good, renew
				return true;
			}
			else {
				Log.l.fine("Removing expired session for " + ip);
				sessions.remove(ip); // Session has expired, remove
				return false;
			}
		}
		else {
			return false;
		}
	}

	/**
	 * Prunes all inactive sessions when the last prune time has exceeded {@link #TIME_BETWEEN_PRUNES_MS}
	 */
	public void pruneSessions() {
		if (System.currentTimeMillis() - lastPruneTime > TIME_BETWEEN_PRUNES_MS) {
			int before = sessions.size();
			Iterator<Session> it = sessions.values().iterator();
			while (it.hasNext()) {
				if (it.next().hasExpired()) {
					it.remove();
				}
			}
			lastPruneTime = System.currentTimeMillis();
			Log.l.info("Pruned expired sessions=" + (before - sessions.size()));
		}
	}
}
