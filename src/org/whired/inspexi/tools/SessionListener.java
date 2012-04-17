package org.whired.inspexi.tools;

public interface SessionListener {
	void sessionEnded(String reason, Throwable t);
}
