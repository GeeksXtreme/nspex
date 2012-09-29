package org.whired.nspex.blackbox;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
	private final static long TIME_BETWEEN_PRUNES_MS = 1000 * 60 * 10; // 10 minutes
	// The amount of time that is allowed to pass before a session is considered invalid
	private final static long CONSIDERED_INVALID_MS = 1000 * 60 * 30; // 30 minutes

	/**
	 * Checks whether or not the specified ip has a session
	 * @param ip the ip to check
	 * @return {@code true} if a session exists, otherwise {@code false}
	 */
	public boolean hasSession(final String ip) {
		return sessions.containsKey(ip);
	}

	/**
	 * Checks whether or not an ip's session is valid
	 * @param ip the ip to check
	 * @param sessionId the session ip to check
	 * @return {@code true} if the session id matches, otherwise {@code false}
	 */
	public boolean sessionValid(final String ip, final String sessionId) {
		final Session session = sessions.get(ip);
		return session != null && session.getSessionId().equals(sessionId);
	}

	/**
	 * Creates a session for the specified ip
	 * @param ip the ip to create a session for
	 * @return the session id that was created
	 */
	public String createSession(final String ip) {
		// Prune if necessary
		pruneSessions();
		final Session session = new Session(DigestUtils.sha256Hex(this.toString() + System.currentTimeMillis() + "$5a4lL7t"));
		sessions.put(ip, session);
		return session.getSessionId();
	}

	/**
	 * Prunes all inactive sessions when the last prune time has exceeded {@link #TIME_BETWEEN_PRUNES_MS}
	 */
	public void pruneSessions() {
		if (System.currentTimeMillis() - lastPruneTime > TIME_BETWEEN_PRUNES_MS) {
			int before = sessions.size();
			Iterator<Session> it = sessions.values().iterator();
			Session session;
			while (it.hasNext()) {
				session = it.next();
				if (isInvalid(session)) {
					it.remove();
				}
			}
			lastPruneTime = System.currentTimeMillis();
			Log.l.info("Pruned invalid sessions=" + (before - sessions.size()));
		}
	}

	/**
	 * Checks whether or not a session is invalid
	 * @param session the session to check
	 */
	public boolean isInvalid(final Session session) {
		return System.currentTimeMillis() - session.getCreateTime() > CONSIDERED_INVALID_MS;
	}

	/**
	 * A session
	 * @author Whired
	 */
	private final class Session {
		/** The id for this session */
		private final String sessionId;
		/** The time this session was created, in ms */
		private final long createTime = System.currentTimeMillis();

		/**
		 * Creates a new session with the specified session id
		 * @param sessionId the session id for the session to create
		 */
		public Session(final String sessionId) {
			this.sessionId = sessionId;
		}

		/**
		 * Gets the session id for this session
		 * @return the session id
		 */
		public String getSessionId() {
			return this.sessionId;
		}

		/**
		 * Gets the create time of this session
		 * @return the last activity of this session, in MS
		 */
		public final long getCreateTime() {
			return this.createTime;
		}
	}
}
