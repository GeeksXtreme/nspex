package org.whired.nspex.updater;

public class SlaveUpdater {
	public static void main(final String[] args) {
		final String cb = System.getProperty("user.home") + Package.FS + "ispx_cache" + Package.FS;
		new Updater(new Package("ispx_slv.jar", "https://github.com/Whired/nspex/raw/master/dist/", cb, "org.whired.nspex.slave.LocalSlave")).run();
	}
}
