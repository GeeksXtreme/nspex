package org.whired.inspexi.master;

public interface EventListener {
	void connect(String[] ips);

	void rebuild(String[] ips);

	void refresh(String[] ips);
}
