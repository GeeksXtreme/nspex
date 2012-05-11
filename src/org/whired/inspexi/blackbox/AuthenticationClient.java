package org.whired.inspexi.blackbox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

public class AuthenticationClient {

	public static void main(final String[] args) throws UnknownHostException, IOException, GeneralSecurityException {
		// Set up RSA
		final RSASession rsa = new RSASession();

		// Streamlined connect
		final Socket s = new Socket("inspexi.hopto.org", 43597);
		final DataInputStream dis = new DataInputStream(s.getInputStream());
		final DataOutputStream dos = new DataOutputStream(s.getOutputStream());

		// Exchange the keys for RSA
		rsa.exchangeKeys(dis, dos);

		// Set up a secure output stream
		RSAOutputStream ros = new RSAOutputStream(dos, rsa);

		// Send our details
		ros.writeUTF("Whired");
		ros.writeUTF("mypassword");

		// Hang so server can catch up
		dos.close();
		dis.read();
	}
}
