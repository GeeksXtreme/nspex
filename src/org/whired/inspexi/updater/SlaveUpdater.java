package org.whired.inspexi.updater;


public class SlaveUpdater {
	public static void main(String[] args) {
		new Updater(new Package("ispx_slv.jar", "remote", "./", "org.whired.inspexi.net.Slave")).run();
	}
}
