package org.whired.nspex.blackbox;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;

/**
 * Maintains a RSA-encrypted session
 * @author Whired
 */
public class RSASession {
	private final PublicKey remotePublicKey;
	private final Cipher cipher = Cipher.getInstance("RSA");
	private final KeyFactory fact = KeyFactory.getInstance("RSA");
	private final RSAKeySet keys;

	/**
	 * Creates a new RSA session with the specified local keys and remote key spec The remote key will be generated immediately, so this operation is expensive
	 * @param keys the local keys
	 * @param spec the remote key spec
	 * @throws GeneralSecurityException if a key cannot be generated
	 */
	public RSASession(final RSAKeySet keys, final ByteBuffer spec) throws GeneralSecurityException {
		this.keys = keys;

		byte[] buf = new byte[spec.getInt()];
		spec.get(buf);
		final BigInteger mod = new BigInteger(buf);
		buf = new byte[spec.getInt()];
		spec.get(buf);
		final BigInteger exp = new BigInteger(buf);

		remotePublicKey = fact.generatePublic(new RSAPublicKeySpec(mod, exp));
	}

	/**
	 * Decrypts the specified bytes
	 * @param encrypted the bytes to decrypt
	 * @return the decrypted bytes
	 * @throws GeneralSecurityException when the bytes can't be decrypted for any reason
	 */
	public final ByteBuffer decrypt(final ByteBuffer encrypted) throws GeneralSecurityException {
		cipher.init(Cipher.DECRYPT_MODE, keys.getPrivateKey());
		if (encrypted.position() > 0) {
			encrypted.flip();
		}
		final byte[] encBytes = new byte[encrypted.capacity()];
		encrypted.get(encBytes);
		final byte[] cipherData = cipher.doFinal(encBytes);
		return ByteBuffer.wrap(cipherData).asReadOnlyBuffer();
	}

	/**
	 * Encrypts the specified bytes
	 * @param plainText the bytes to encrypt
	 * @return the encrypted bytes
	 * @throws GeneralSecurityException when the bytes can't be encrypted for any reason
	 */
	public final ByteBuffer encrpyt(final ByteBuffer plainText) throws GeneralSecurityException {
		// TODO this sucks for obvious reasons
		if (remotePublicKey == null) {
			// There's no way to guarantee that we have a key
			// Should be asked for in parameters but exceptions make it just as messy..
			throw new NullPointerException("Cannot encrpyt without remote key (generateRemotePublicKey())");
		}
		cipher.init(Cipher.ENCRYPT_MODE, remotePublicKey);
		if (plainText.position() > 0) {
			plainText.flip();
		}
		final byte[] ptBytes = new byte[plainText.capacity()];
		plainText.get(ptBytes);
		final byte[] cipherData = cipher.doFinal(ptBytes);
		return ByteBuffer.wrap(cipherData).asReadOnlyBuffer();
	}
}