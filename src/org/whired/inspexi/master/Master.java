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
		public void connect(final RemoteSlave[] slaves) {
			for (int i = 0; i < slaves.length; i++) {
				RemoteSlave rsm = slaves[i];
				try {
					new RemoteSlaveFullView(frame, rsm);
					rsm.connect(Slave.INTENT_CONNECT);
				}
				catch (final IOException t) {
					Log.l.warning("Could not connect to " + rsm.getIp() + ".");
				}
			}
		}

		@Override
		public void rebuild(final RemoteSlave[] slaves) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < slaves.length; i++) {
						final RemoteSlave rsm = slaves[i];
						try {
							rsm.connect(Slave.INTENT_REBUILD);
						}
						catch (final Throwable t) {
						}
					}
				}
			}).start();
		}

		@Override
		public void refresh(final RemoteSlave[] slaves) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < slaves.length; i++) {
						final RemoteSlave rsm = slaves[i];
						try {
							rsm.setView(frame);
							if (slaves.length == 1) {
								rsm.connect(Slave.INTENT_CHECK);
							}
							else {
								rsm.connect(Slave.INTENT_CHECK_BULK);
							}
						}
						catch (final Throwable t) {
						}
					}
					Log.l.info("Queried " + slaves.length + " slave(s)");
				}
			}).start();
		}
	};

	/**
	 * Creates a new master that will work with the specified slaves
	 * @param slaves the slaves to work with initially
	 */
	public Master(final RemoteSlave[] slaves) {
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
				frame.refresh(slaves);
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

		//Log.l.setLevel(Level.ALL);
		// TODO save on exit:
		// String[] ips = new String[] { "localhost", "192.168.2.8" };
		// StringBuilder b = new StringBuilder();
		// for (String s : ips) {
		// b.append(s + ",");
		// }
		// b.deleteCharAt(b.length() - 1);
		// props.put("ips", b.toString());
		// props.store(new FileOutputStream("props.dat"), null);
		final String s = (String) props.get("ips");
		String[] ips = s != null ? s.split(",") : new String[0];
		RemoteSlave[] slaves = new RemoteSlave[ips.length];
		for (int i = 0; i < slaves.length; i++) {
			slaves[i] = new RemoteSlave(ips[i], 43596);
		}
		new Master(slaves);
	}
}
