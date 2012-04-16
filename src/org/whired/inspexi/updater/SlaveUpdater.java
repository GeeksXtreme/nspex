package org.whired.inspexi.updater;

public class SlaveUpdater {
	public static void main(final String[] args) {
		final String cb = System.getProperty("user.home") + Package.FS + "ispx_cache" + Package.FS;
		new Updater(new Package("ispx_slv.jar", "https://github.com/Whired/inspexi/raw/master/dist/", cb, "org.whired.inspexi.slave.LocalSlave")).run();
	}
}
