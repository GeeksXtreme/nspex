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
	private final RSAKeySet rsaKeys = new RSAKeySet(1024);
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
							RemoteSlave[] slaves = new RemoteSlave[payload.getInt()];
							Log.l.info("Slaves.length: " + slaves.length);
							for (int i = 0; i < slaves.length; i++) {
								slaves[i] = new RemoteSlave(BufferUtil.getJTF(payload));
								Log.l.info("Adding slave: " + slaves[i].toString());
							}
							if (slaves.length > 0) {
								slavesReceived(slaves);
							}
						break;
						case Opcodes.CONFIRM_ISP_CHANGE:
							promptISPChange(payload.getLong());
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
			}

			@Override
			public void remoteLogged(Level level, final String message) {
				AuthenticationClient.this.remoteLogged(level, message);
			}

		};

		// Send local public key specification
		comm.send(Opcodes.RSA_KEY_REQUEST, rsaKeys.getPublicKeySpec());
	}

	/**
	 * Invoked when a user must decide to allow an ISP change
	 * @param timeout the limitation on how often an ISP can change, in MS
	 */
	public abstract void promptISPChange(long timeout);

	/**
	 * Confirms a chnage of ISP
	 * @param allow whether or not to allow the change
	 */
	public void confirmISPChange(boolean allow) {
		comm.send(Opcodes.CONFIRM_ISP_CHANGE, ByteBuffer.allocate(1).put((byte) (allow ? 1 : 0)));
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
