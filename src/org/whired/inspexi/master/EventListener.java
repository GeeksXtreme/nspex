package org.whired.inspexi.master;

public interface EventListener {
	void connect(String ip);

	void rebuild(String ip);

	void refresh();
}
