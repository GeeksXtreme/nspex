package org.whired.inspexi.master;

import java.awt.EventQueue;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.whired.inspexi.logging.Log;
import org.whired.inspexi.tools.Slave;

public class Master {
	private MasterFrame frame;
	private final EventListener listener = new EventListener() {
		@Override
		public void connect(final String[] ips) {
			for (final String ip : ips) {
				try {
					RemoteSlave slave = new RemoteSlave(ip, 43596, Slave.INTENT_CONNECT, new SessionListener() {
						@Override
						public void sessionEnded(String reason) {
							Log.l.info("Session with " + ip + " ended: " + reason);
						}

					});
					slave.beginSession();
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}

		@Override
		public void rebuild(final String[] ips) {
			for (final String ip : ips) {
				try {
					new RemoteSlave(ip, 43596, Slave.INTENT_REBUILD, new SessionListener() {
						@Override
						public void sessionEnded(String reason) {
							Log.l.info("Session with " + ip + " ended: " + reason);
						}

					}).beginSession();
				}
				catch (Throwable t) {
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
							RemoteSlave r = new RemoteSlave(ip, 43596, Slave.INTENT_CHECK, new SessionListener() {
								@Override
								public void sessionEnded(String reason) {
								}

							});
							frame.updateSlaveList(ip, r.getHost(), r.getOS(), r.getVersion());
						}
						catch (Throwable t) {
							frame.setSlaveOffline(ip);
						}
					}

					Log.l.info("Queried " + ips.length + " slave(s)");
				}
			}).start();
		}
	};

	public Master(final String[] slaveIps) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				frame = new MasterFrame(listener);
				frame.addClients(slaveIps);
				frame.setVisible(true);
			}
		});
	}

	private static final String PROPS_FILE = "props.dat";

	public static Properties getProps() {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(PROPS_FILE));
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		return props;
	}

	public static void main(String[] args) throws InterruptedException, InvocationTargetException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, FileNotFoundException, IOException {
		Properties props = getProps();
		// String[] ips = new String[] { "localhost", "192.168.2.8" };
		// StringBuilder b = new StringBuilder();
		// for (String s : ips) {
		// b.append(s + ",");
		// }
		// b.deleteCharAt(b.length() - 1);
		// props.put("ips", b.toString());
		// props.store(new FileOutputStream("props.dat"), null);
		String s = (String) props.get("ips");
		new Master(s != null ? s.split(",") : new String[0]);
	}
}
