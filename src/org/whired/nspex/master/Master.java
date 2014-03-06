package org.whired.nspex.master;

import java.awt.EventQueue;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.whired.nspex.tools.Slave;
import org.whired.nspex.tools.logging.Log;

/**
 * A master server
 * @author Whired
 */
public class Master {
	/** The view for this master */
	private MasterFrame frame;

	/** The session id sent from the auth server */
	private String sessionId;

	/** The ip for the auth server */
	private String authIp;

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

		private AuthenticationClient ac;

		@Override
		public void downloadSlaves() {
			// TODO Testing only!
			refresh(new RemoteSlave[] { new RemoteSlave("localhost") });
			try {
				ac = new AuthenticationClient(new AuthenticationListener() {

					@Override
					public void slavesReceived(RemoteSlave[] slaves) {
						Log.l.info(slaves.length + " slaves received.");
						refresh(slaves);
					}

					@Override
					public void remoteLogged(Level level, String message) {
						Log.l.log(level, message);
					}

					@Override
					public void disconnected() {
						Log.l.warning("Disconnected from auth server");
					}

					@Override
					public void sessionIDReceived(final String lsessionId) {
						sessionId = lsessionId;
						Log.l.info("Received sessionId: " + sessionId);
					}

					@Override
					public void sessionInvalidated() {
						sessionId = null;
						frame.loginWithCredentials(null, null, null); // TODO God, why
					}
				});

				if (sessionId == null) {
					frame.loginWithCredentials(null, null, null); // TODO God, why
				}
				else {
					// Attempt login with the sessionId
					ac.login(authIp, sessionId);
				}
			}
			catch (Throwable e) {
				Log.l.warning("Unable to download slaves: " + e);
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

		@Override
		public void loginWithCredentials(String ip, String user, String pass) {
			try {
				ac.login(authIp = ip, user, pass);
			}
			catch (Throwable e) {
				Log.l.warning("Unable to login: " + e);
			}
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
		Log.l.setLevel(Level.FINE);

		// Obtain slaves from overlord
		// (We don't really know how to obtain overlord's IP yet..)
		Master m = new Master();

		m.listener.downloadSlaves();
	}
}
