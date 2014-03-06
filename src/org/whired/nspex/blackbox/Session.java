package org.whired.nspex.blackbox;


/**
 * A session
 * @author Whired
 */
public class Session {
	/** The id for this session */
	private final String sessionId;
	/** The time this session was created, in MS */
	private long createTime = System.currentTimeMillis();
	/** The life span of this session, in MS */
	private final long lifeSpan;

	/**
	 * Creates a new session with the specified session id
	 * @param sessionId the session id for the session to create
	 */
	public Session(final String sessionId, final long lifeSpan) {
		this.sessionId = sessionId;
		this.lifeSpan = lifeSpan;
	}

	/**
	 * Gets the session id for this session
	 * @return the session id
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * Checks whether or not this session has expired
	 */
	public boolean hasExpired() {
		return System.currentTimeMillis() - this.createTime > this.lifeSpan;
	}

	/**
	 * Renews this session's create time
	 */
	public void renew() {
		this.createTime = System.currentTimeMillis();
	}
}