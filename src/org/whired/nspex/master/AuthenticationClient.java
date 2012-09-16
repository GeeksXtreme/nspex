package org.whired.nspex.blackbox;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.logging.Level;

import org.whired.nspex.net.ExpandableByteBuffer;
import org.whired.nspex.net.IoCommunicable;
import org.whired.nspex.tools.logging.Log;

/**
 * A client that connects to the authentication server
 * @author Whired
 */
public class AuthenticationClient {

	private RSASession rsaSess;
	private final RSAKeySet rsaKeys = new RSAKeySet();
	private final IoCommunicable comm;

	public AuthenticationClient() throws UnknownHostException, IOException, GeneralSecurityException {
		comm = new IoCommunicable(new Socket("localhost", 43597)) {

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
							buf.putJTF("Whired");
							buf.putJTF("mypassword");
							send(Opcodes.LOGIN, rsaSess.encrpyt(buf.asByteBuffer()));
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
				// TODO notify user
			}

		};
		// Send local public key
		comm.send(0, rsaKeys.getPublicKeySpec());
	}

	public static void main(final String[] args) throws UnknownHostException, IOException, GeneralSecurityException {
		Log.l.setLevel(Level.ALL);
		new AuthenticationClient();
	}
}
