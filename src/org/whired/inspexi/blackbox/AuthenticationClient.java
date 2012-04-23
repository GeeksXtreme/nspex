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
		System.out.println("exch");
		rsa.exchangeKeys(dis, dos);
		// Encrypt with server public key
		final byte[] encrypted = rsa.encrpyt(new String("ok").getBytes("utf8"));
		System.out.println("Writing: " + encrypted[0]);
		dos.writeInt(encrypted.length);
		dos.write(encrypted);

		// Hang so server can catch up
		dis.read();
	}
}
