package org.whired.inspexi.master;

import java.awt.EventQueue;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.whired.inspexi.tools.Slave;
import org.whired.inspexi.tools.logging.Log;

/**
 * A master server
 * @author Whired
 */
public class Master {
	/** The view for this master */
	private MasterFrame frame;

	/** Listens for events fired by the view */
	private final ControllerEventListener listener = new ControllerEventListener() {
		@Override
		public void connect(final String[] ips) {
			for (final String ip : ips) {
				try {
					final RemoteSlaveModel slave = new RemoteSlaveModel(ip, 43596);
					new RemoteSlaveFullView(slave);
					slave.connect(Slave.INTENT_CONNECT);
				}
				catch (final Throwable t) {
					t.printStackTrace();
					Log.l.warning("Could not connect to " + ip + ".");
				}
			}
		}

		@Override
		public void rebuild(final String[] ips) {
			for (final String ip : ips) {
				try {
					new RemoteSlaveModel(ip, 43596).connect(Slave.INTENT_REBUILD);
				}
				catch (final Throwable t) {
					t.printStackTrace();
				}
			}
		}

		@Override
		public void refresh(final String[] ips) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (final String ip : ips) {
						try {
							final RemoteSlaveModel r = new RemoteSlaveModel(ip, 43596);
							if (ips.length == 1) {
								r.setImageConsumer(frame);
								r.connect(Slave.INTENT_CHECK);
							}
							else {
								r.connect(Slave.INTENT_CHECK_BULK);
							}
							frame.updateSlaveList(ip, r.getHost(), r.getOS(), r.getVersion());
						}
						catch (final Throwable t) {
							frame.setSlaveOffline(ip);
						}
					}

					Log.l.info("Queried " + ips.length + " slave(s)");
				}
			}).start();
		}
	};

	/**
	 * Creates a new master that will work with the specified given ips
	 * @param slaveIps the ips to work with initially
	 */
	public Master(final String[] slaveIps) {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		}
		catch (final Throwable e) {
			Log.l.log(Level.WARNING, "Failed to set look and feel: ", e);
		}
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				frame = new MasterFrame(listener);
				frame.addSlaves(slaveIps);
				frame.setVisible(true);
			}
		});
	}

	/** The location of the properties file that has settings for this slave */
	private static final String PROPS_FILE = "props.dat";

	/**
	 * Loads properties from the path specified in {@link #PROPS_FILE}, or a new {@code Properties} if none could be loaded
	 * @return the properties
	 */
	public static Properties getProps() {
		final Properties props = new Properties();
		try {
			props.load(new FileInputStream(PROPS_FILE));
		}
		catch (final Throwable t) {
			t.printStackTrace();
		}
		return props;
	}

	public static void main(final String[] args) throws InterruptedException, InvocationTargetException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, FileNotFoundException, IOException {
		final Properties props = getProps();
		// String[] ips = new String[] { "localhost", "192.168.2.8" };
		// StringBuilder b = new StringBuilder();
		// for (String s : ips) {
		// b.append(s + ",");
		// }
		// b.deleteCharAt(b.length() - 1);
		// props.put("ips", b.toString());
		// props.store(new FileOutputStream("props.dat"), null);
		final String s = (String) props.get("ips");
		new Master(s != null ? s.split(",") : new String[0]);
	}
}
