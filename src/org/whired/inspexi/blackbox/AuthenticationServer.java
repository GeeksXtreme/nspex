package org.whired.inspexi.blackbox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import org.whired.inspexi.tools.NetTask;
import org.whired.inspexi.tools.NetTaskQueue;
import org.whired.inspexi.tools.ReactServer;
import org.whired.inspexi.tools.SessionListener;

/**
 * The authentication server
 * @author Whired
 */
public class AuthenticationServer {
	private final SessionListener listener = new SessionListener() {
		@Override
		public void sessionEnded(final String reason, Throwable t) {
			t.printStackTrace();
			// Turns out, we really don't care
			// It's best to just assume every connection is from a hacker and drop them without grace
		}
	};
	private final NetTaskQueue queue = new NetTaskQueue(listener);

	/**
	 * Starts an authentication server on the specified port
	 * @param port the port to listen for connections on
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	public AuthenticationServer(final int port) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		// Set up encryption
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.genKeyPair();
		KeyFactory fact = KeyFactory.getInstance("RSA");
		final PrivateKey serverPrivateKey = kp.getPrivate();
		final RSAPublicKeySpec serverPublicKey = fact.getKeySpec(kp.getPublic(), RSAPublicKeySpec.class);
		final ReactServer server = new ReactServer(new ServerSocket(port), queue) {
			@Override
			public NetTask getOnConnectTask(final Socket sock) {
				return new NetTask("auth_connect", sock) {
					@Override
					public void run(final DataInputStream dis, final DataOutputStream dos) throws IOException, GeneralSecurityException {
						// First things first, these guys need to do their business QUICK
						sock.setSoTimeout(2000);

						RSASession rsa = new RSASession(dis, dos);
						rsa.sendLocalKey();
						rsa.getRemoteKey();
						byte[] payload = new byte[dis.readInt()];
						System.out.println("plen: " + payload.length);
						dis.readFully(payload);
						// Decrypt with server private key
						System.out.println("before: " + payload[0]);
						payload = rsa.decrypt(payload);
						System.out.println("after: " + payload[0]);

						// Read hwid, name, and password

						// Compare ips/hwid/salt -- limit of 3 (_hashed_) per user!

						// Database work

						// Pick a slave

						// Tell slave to accept the calculated hash - master cannot replicate this

						// Write slave ip
					}
				};
			}
		};
		System.out.println("Starting authentication server");
		server.startAccepting();
	}

	public static void main(final String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		new AuthenticationServer(43597);
	}
}
