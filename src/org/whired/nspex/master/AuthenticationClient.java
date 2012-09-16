package org.whired.nspex.master;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.logging.Level;

import org.whired.nspex.blackbox.Opcodes;
import org.whired.nspex.net.BufferUtil;
import org.whired.nspex.net.ExpandableByteBuffer;
import org.whired.nspex.net.IoCommunicable;
import org.whired.nspex.tools.RSAKeySet;
import org.whired.nspex.tools.RSASession;
import org.whired.nspex.tools.logging.Log;

/**
 * A client that connects to the authentication server
 * @author Whired
 */
public abstract class AuthenticationClient {

	/** The RSA session to use when communicating securely */
	private RSASession rsaSess;
	/** The RSA keys to use for this client */
	private final RSAKeySet rsaKeys = new RSAKeySet();
	/** The communicable this client will use */
	private final IoCommunicable comm;

	public AuthenticationClient(final String user, final String password, final String ip) throws UnknownHostException, IOException, GeneralSecurityException {
		comm = new IoCommunicable(new Socket(ip, 43597)) {

			@Override
			public void handle(final int id) {
			}

			@Override
			public void handle(final int id, final ByteBuffer payload) {
				try {
					switch (id) {
						case Opcodes.RSA_KEY_REQUEST:
							// Generate key from remote's spec
							rsaSess = new RSASession(rsaKeys, payload);
							// Guess we can also send the other junk here
							final ExpandableByteBuffer buf = new ExpandableByteBuffer();
							buf.putJTF(user);
							buf.putJTF(password);
							send(Opcodes.LOGIN, rsaSess.encrpyt(buf.asByteBuffer()));
						break;
						case Opcodes.SLAVES_RECEIVED:
							for (int i = 0; i < payload.getInt(); i++) {
								Log.l.info("I own slave: " + BufferUtil.getJTF(payload));
							}
						break;
					}
				}
				catch (final Throwable e) {
					e.printStackTrace();
					disconnect();
				}
			}

			@Override
			protected void disconnected() {
				AuthenticationClient.this.disconnected();
				//				SwingUtilities.invokeLater(new Runnable() {
				//					@Override
				//					public void run() {
				//						JOptionPane.showMessageDialog(parent, "Disconnected from auth server", "nspex", JOptionPane.ERROR_MESSAGE);
				//					}
				//				});
			}

			@Override
			public void remoteLogged(Level level, final String message) {
				AuthenticationClient.this.remoteLogged(level, message);
				//				final int type = level.intValue() <= Level.INFO.intValue() ? JOptionPane.PLAIN_MESSAGE : JOptionPane.ERROR_MESSAGE;
				//				SwingUtilities.invokeLater(new Runnable() {
				//					@Override
				//					public void run() {
				//						// TODO like these actually belong here..
				//						JOptionPane.showMessageDialog(parent, message, "nspex", type);
				//					}
				//				});
			}

		};
		// Send local public key
		comm.send(Opcodes.RSA_KEY_REQUEST, rsaKeys.getPublicKeySpec());
	}

	/**
	 * Invoked when the list of slaves are received
	 * @param slaves the slaves that were received
	 */
	protected abstract void slavesReceived(final RemoteSlave[] slaves);

	/**
	 * Invoked when a remote message has been logged
	 * @param level the level of the message
	 * @param message the message
	 */
	protected abstract void remoteLogged(final Level level, final String message);

	/**
	 * Invoked when the client has been disconnected
	 */
	protected abstract void disconnected();
}
