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
	private PublicKey remotePublicKey;
	private final PrivateKey localPrivateKey;
	private final Cipher cipher = Cipher.getInstance("RSA");
	private final KeyFactory fact = KeyFactory.getInstance("RSA");
	private final RSAPublicKeySpec localPublicKeySpec;

	/**
	 * Creates a new RSA session. This operation is expensive and should not be done repetitively.
	 * @throws GeneralSecurityException when keys could not be generated for any reason
	 */
	public RSASession() throws GeneralSecurityException {
		long start = System.currentTimeMillis();
		final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		final KeyPair kp = kpg.genKeyPair();
		localPublicKeySpec = fact.getKeySpec(kp.getPublic(), RSAPublicKeySpec.class);
		localPrivateKey = kp.getPrivate();
		System.out.println("RSA keys generated (" + (System.currentTimeMillis() - start) + "ms)");
	}

	public void exchangeKeys(DataInputStream dis, DataOutputStream dos) throws IOException, InvalidKeySpecException {
		writeLocalKeySpec(dos);
		readRemoteKeySpec(dis);
	}

	/**
	 * Reads the remote public key specification from the specified input stream
	 * @param dis the input stream to read from
	 * @throws IOException if the key cannot be read
	 * @throws InvalidKeySpecException when a public key cannot be generated
	 */
	private void readRemoteKeySpec(DataInputStream dis) throws IOException, InvalidKeySpecException {
		byte[] buf = new byte[dis.readInt()];
		dis.readFully(buf);
		final BigInteger mod = new BigInteger(buf);

		buf = new byte[dis.readInt()];
		dis.readFully(buf);
		final BigInteger exp = new BigInteger(buf);
		remotePublicKey = fact.generatePublic(new RSAPublicKeySpec(mod, exp));
	}

	/**
	 * Writes the local public key specification to the specified output stream
	 * @param dos the output stream to write to
	 * @throws IOException when the key cannot be written
	 */
	private void writeLocalKeySpec(DataOutputStream dos) throws IOException {
		byte[] data = localPublicKeySpec.getModulus().toByteArray();
		dos.writeInt(data.length);
		dos.write(data);

		data = localPublicKeySpec.getPublicExponent().toByteArray();
		dos.writeInt(data.length);
		dos.write(data);
	}

	/**
	 * Decrypts the specified bytes
	 * @param encrypted the bytes to decrypt
	 * @return the decrypted bytes
	 * @throws GeneralSecurityException when the bytes can't be decrypted for any reason
	 */
	public final byte[] decrypt(final byte[] encrypted) throws GeneralSecurityException {
		cipher.init(Cipher.DECRYPT_MODE, localPrivateKey);
		final byte[] cipherData = cipher.doFinal(encrypted);
		return cipherData;
	}

	/**
	 * Encrypts the specified bytes
	 * @param plainText the bytes to encrypt
	 * @return the encrypted bytes
	 * @throws GeneralSecurityException when the bytes can't be encrypted for any reason
	 */
	public final byte[] encrpyt(final byte[] plainText) throws GeneralSecurityException {
		if (remotePublicKey == null) {
			// There's no way to guarantee that we have a key
			// Should be asked for in parameters but exceptions make it just as messy..
			throw new NullPointerException("Cannot encrpyt without remote key (getRemoteKey())");
		}
		cipher.init(Cipher.ENCRYPT_MODE, remotePublicKey);
		final byte[] cipherData = cipher.doFinal(plainText);
		return cipherData;
	}
}
