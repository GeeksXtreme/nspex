package org.whired.nspex.tools.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {
	public static final Logger l = createLogger();

	private final static Logger createLogger() {
		final Logger logger = Logger.getLogger(Log.class.getName());
		logger.setLevel(Level.INFO);
		final ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(Level.ALL);
		ch.setFormatter(new LogFormatter());
		logger.addHandler(ch);
		logger.setUseParentHandlers(false);
		return logger;
	}
}
