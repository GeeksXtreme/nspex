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
public class AuthenticationClient implements AuthenticationListener {

	/** The RSA session to use when communicating securely */
	private RSASession rsaSess;
	/** The RSA keys to use for this client */
	private final RSAKeySet rsaKeys = RSAKeySet.generateKeys(1024);
	/** The communicable this client will use */
	private IoCommunicable comm;
	/** The listener for this client */
	private final AuthenticationListener listener;

	public AuthenticationClient(AuthenticationListener listener) throws GeneralSecurityException {
		this.listener = listener;
	}

	public void login(final String ip, final String sessionId) throws UnknownHostException, IOException {
		comm = new IoCommunicable(new Socket(ip, 43597)) {

			@Override
			public void handle(final int id) {
				switch (id) {
					case Opcodes.INVALIDATE_SESSION:
						sessionInvalidated();
					break;
				}
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
							buf.putJTF(sessionId);
							send(Opcodes.LOGIN_WITH_SESSION, rsaSess.encrypt(buf.asByteBuffer()));
						break;
						case Opcodes.SLAVES_RECEIVED:
							RemoteSlave[] slaves = new RemoteSlave[payload.getInt()];
							for (int i = 0; i < slaves.length; i++) {
								slaves[i] = new RemoteSlave(BufferUtil.getJTF(payload));
							}
							if (slaves.length > 0) {
								slavesReceived(slaves);
							}
						break;
						case Opcodes.LOGIN_WITH_SESSION:
							sessionIDReceived(BufferUtil.getJTF(rsaSess.decrypt(payload)));
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
		Log.l.info("Requesting public key from server..");
		comm.send(Opcodes.RSA_KEY_REQUEST, rsaKeys.getPublicKeySpec());
	}

	public void login(final String ip, final String user, final String password) throws IOException, GeneralSecurityException {
		comm = new IoCommunicable(new Socket(ip, 43597)) {

			@Override
			public void handle(final int id) {
				switch (id) {
					case Opcodes.INVALIDATE_SESSION:
						sessionInvalidated();
					break;
				}
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
							send(Opcodes.LOGIN, rsaSess.encrypt(buf.asByteBuffer()));
						break;
						case Opcodes.SLAVES_RECEIVED:
							RemoteSlave[] slaves = new RemoteSlave[payload.getInt()];
							for (int i = 0; i < slaves.length; i++) {
								slaves[i] = new RemoteSlave(BufferUtil.getJTF(payload));
							}
							if (slaves.length > 0) {
								slavesReceived(slaves);
							}
						break;
						case Opcodes.LOGIN_WITH_SESSION:
							sessionIDReceived(BufferUtil.getJTF(rsaSess.decrypt(payload)));
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

	@Override
	public void slavesReceived(RemoteSlave[] slaves) {
		listener.slavesReceived(slaves);
	}

	@Override
	public void remoteLogged(Level level, String message) {
		listener.remoteLogged(level, message);
	}

	@Override
	public void disconnected() {
		listener.disconnected();
	}

	@Override
	public void sessionIDReceived(String lsessionId) {
		listener.sessionIDReceived(lsessionId);
	}

	@Override
	public void sessionInvalidated() {
		listener.sessionInvalidated();
	}
}
