package org.whired.inspexi.blackbox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;

public class RSASession {
	public PublicKey remotePublicKey;
	public final PrivateKey localPrivateKey;
	private final Cipher cipher = Cipher.getInstance("RSA");
	private final KeyFactory fact = KeyFactory.getInstance("RSA");
	private final DataInputStream dis;
	private final DataOutputStream dos;
	private final RSAPublicKeySpec localPublicKeySpec;

	public RSASession(final DataInputStream dis, final DataOutputStream dos) throws GeneralSecurityException, IOException {
		this.dis = dis;
		this.dos = dos;
		final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		final KeyPair kp = kpg.genKeyPair();
		localPublicKeySpec = fact.getKeySpec(kp.getPublic(), RSAPublicKeySpec.class);
		localPrivateKey = kp.getPrivate();
	}

	public void getRemoteKey() throws IOException, InvalidKeySpecException {
		// Read public from remote
		byte[] buf = new byte[dis.readInt()];
		dis.readFully(buf);
		final BigInteger mod = new BigInteger(buf);

		buf = new byte[dis.readInt()];
		dis.readFully(buf);
		final BigInteger exp = new BigInteger(buf);
		remotePublicKey = fact.generatePublic(new RSAPublicKeySpec(mod, exp));
	}

	public void sendLocalKey() throws IOException {

		// Send public to remote
		byte[] data = localPublicKeySpec.getModulus().toByteArray();
		dos.writeInt(data.length);
		dos.write(data);

		data = localPublicKeySpec.getPublicExponent().toByteArray();
		dos.writeInt(data.length);
		dos.write(data);
	}

	public final byte[] decrypt(final byte[] encrypted) throws GeneralSecurityException {
		cipher.init(Cipher.DECRYPT_MODE, localPrivateKey);
		final byte[] cipherData = cipher.doFinal(encrypted);
		return cipherData;
	}

	public final byte[] encrpyt(final byte[] plainText) throws GeneralSecurityException {
		cipher.init(Cipher.ENCRYPT_MODE, remotePublicKey);
		final byte[] cipherData = cipher.doFinal(plainText);
		return cipherData;
	}
}
