package org.whired.nspex.tools;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;

/**
 * Maintains a RSA session
 * @author Whired
 */
public class RSASession {
	/** The public key that the remote is using */
	private final PublicKey remotePublicKey;
	/** A generic RSA cipher used for cryption */
	private final Cipher cipher = Cipher.getInstance("RSA");
	/** The keys to use for cryption */
	private final RSAKeySet keys;

	/**
	 * Creates a new RSA session with the specified local keys and remote key spec The remote key will be generated immediately, so this operation is expensive
	 * @param keys the local keys
	 * @param spec the remote key spec
	 * @throws GeneralSecurityException if a key cannot be generated
	 */
	public RSASession(final RSAKeySet keys, final ByteBuffer spec) throws GeneralSecurityException {
		this.keys = keys;

		// Get the modulus
		byte[] buf = new byte[spec.getInt()];
		spec.get(buf);
		final BigInteger mod = new BigInteger(buf);

		// Get the exponent
		buf = new byte[spec.getInt()];
		spec.get(buf);
		final BigInteger exp = new BigInteger(buf);

		// Actually generate the key
		remotePublicKey = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(mod, exp));
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
