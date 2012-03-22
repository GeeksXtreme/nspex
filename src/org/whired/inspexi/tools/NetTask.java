package org.whired.inspexi.tools;

import java.io.IOException;
import java.util.concurrent.Callable;

public abstract class NetTask implements Callable<Boolean> {

	public abstract void run() throws IOException;

	@Override
	public Boolean call() throws Exception {
		try {
			run();
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}
}
