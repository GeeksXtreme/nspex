package org.whired.inspexi.blackbox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

public class AuthenticationClient {
	public static void main(final String[] args) throws UnknownHostException, IOException, GeneralSecurityException {

		// Straight up connection
		final Socket s = new Socket("localhost", 43597);
		final DataInputStream dis = new DataInputStream(s.getInputStream());
		final DataOutputStream dos = new DataOutputStream(s.getOutputStream());

		final RSASession rsa = new RSASession(dis, dos);
		rsa.getRemoteKey();
		rsa.sendLocalKey();

		// Encrypt with server public key
		final byte[] encrypted = rsa.encrpyt(new byte[] { (byte) 80 });
		System.out.println("Writing: " + encrypted[0]);
		dos.writeInt(encrypted.length);
		dos.write(encrypted);
	}
}
