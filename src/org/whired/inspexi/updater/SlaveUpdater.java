package org.whired.inspexi.updater;

public class SlaveUpdater {
	public static void main(String[] args) {
		new Updater(new Package("ispx_slv.jar", "https://github.com/Whired/inspexi/raw/master/dist/", "", "org.whired.inspexi.slave.LocalSlave")).run();
	}
}
