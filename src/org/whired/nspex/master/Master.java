package org.whired.nspex.master;

import java.awt.EventQueue;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.whired.nspex.blackbox.ConnectDialog;
import org.whired.nspex.tools.Slave;
import org.whired.nspex.tools.logging.Log;

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
			for (final RemoteSlave rsm : slaves) {
				new RemoteSlaveFullView(frame, rsm);
				rsm.connect(Slave.INTENT_CONNECT);
			}
		}

		@Override
		public void rebuild(final RemoteSlave[] slaves) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (final RemoteSlave rsm : slaves) {
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
					if (slaves.length > 1) {
						setMaxProgress(slaves.length);
						for (int i = 0; i < slaves.length; i++) {
							slaves[i].setView(frame);
							slaves[i].connect(Slave.INTENT_CHECK_BULK);
							setProgress(i + 1);
						}
					}
					else if (slaves.length == 1) {
						slaves[0].setView(frame);
						slaves[0].connect(Slave.INTENT_CHECK);
					}

					Log.l.info("Queried " + slaves.length + " slave(s)");
				}
			}).start();
		}

		@Override
		public void downloadSlaves() {
			ConnectDialog cd = new ConnectDialog(frame);
			cd.setVisible(true);

			// Get slaves from cd user, pass, and ip
			if (!cd.isCancelled()) {
				Log.l.info("Logging in..");
				try {
					AuthenticationClient ac = new AuthenticationClient(cd.getUsername(), new String(cd.getPassword()), cd.getIp()) {
						@Override
						protected void slavesReceived(RemoteSlave[] slaves) {
							Log.l.info(slaves.length + " slaves received.");
							refresh(slaves);
						}

						@Override
						protected void remoteLogged(Level level, String message) {
							Log.l.log(level, message);
						}

						@Override
						protected void disconnected() {
							Log.l.warning("Disconnected from auth server");
						}

						@Override
						public void promptISPChange(final long timeout) {
							SwingUtilities.invokeLater(new Runnable() {

								@Override
								public void run() {
									final int res = JOptionPane.showConfirmDialog(frame, String.format("Your ISP differs from the one on file\nIt is allowed to change once every %.1f days.\n\nElect to change?", timeout / 1000d / 60d / 60d / 24d));
									confirmISPChange(res == JOptionPane.YES_OPTION);
								}
							});
						}
					};
				}
				catch (Throwable e) {
					Log.l.warning("Unable to download slaves: " + e);
				}
			}
			else {
				Log.l.info("Login cancelled.");
			}
		}

		@Override
		public void setProgress(int progress) {
			frame.setProgress(progress);
		}

		@Override
		public void setMaxProgress(int max) {
			frame.setMaxProgress(max);
		}
	};

	/**
	 * Creates a new master
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	public Master() throws InterruptedException, InvocationTargetException {
		EventQueue.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				frame = new MasterFrame(listener);
				frame.setVisible(true);
			}
		});
	}

	/**
	 * Creates a new master that will work with the specified slaves
	 * @param slaves the slaves to work with initially
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	public Master(final RemoteSlave[] slaves) throws InterruptedException, InvocationTargetException {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		}
		catch (final Throwable e) {
			Log.l.log(Level.WARNING, "Failed to set look and feel: ", e);
		}

		EventQueue.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				frame = new MasterFrame(listener);
				frame.refresh(slaves);
				frame.setVisible(true);
			}
		});
	}

	public void setSlaves(final RemoteSlave[] slaves) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				frame.refresh(slaves);
			}
		});
	}

	public static void main(final String[] args) throws InterruptedException, InvocationTargetException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, FileNotFoundException, IOException, GeneralSecurityException {
		Log.l.setLevel(Level.INFO);

		// Obtain slaves from overlord
		// (We don't really know how to obtain overlord's IP yet..)
		Master m = new Master();

		m.listener.downloadSlaves();
	}
}
