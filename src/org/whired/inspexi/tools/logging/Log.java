package org.whired.inspexi.tools.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {
	public static final Logger l = LoggerFactory.create();

	private static final class LoggerFactory {
		private final static Logger create() {
			final Logger logger = Logger.getLogger(Log.class.getName());
			logger.setLevel(Level.INFO);
			final ConsoleHandler ch = new ConsoleHandler();
			ch.setLevel(Level.INFO);
			ch.setFormatter(new LogFormatter());
			logger.addHandler(ch);
			logger.setUseParentHandlers(false);
			return logger;
		}
	}
}
