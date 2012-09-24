package org.whired.nspex.tools;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.RSAPublicKeySpec;

import org.whired.nspex.tools.logging.Log;

/**
 * The set of RSA keys that can be used during an RSA session
 * @author Whired
 */
public class RSAKeySet {
	/** The specification for our public key */
	private final ByteBuffer localPublicKeySpec;
	/** Our private key */
	private final PrivateKey privateKey;

	public RSAKeySet(RSAPublicKeySpec localPublicKeySpec, PrivateKey privateKey) {
		this.privateKey = privateKey;

		// Set the buffer spec, should only need to be done once
		final byte[] mod = localPublicKeySpec.getModulus().toByteArray();
		final byte[] exp = localPublicKeySpec.getPublicExponent().toByteArray();
		// Allocate lengths and account for int size (4+4)
		this.localPublicKeySpec = ByteBuffer.allocate(mod.length + exp.length + 8);
		this.localPublicKeySpec.putInt(mod.length);
		this.localPublicKeySpec.put(mod);
		this.localPublicKeySpec.putInt(exp.length);
		this.localPublicKeySpec.put(exp);
		this.localPublicKeySpec.flip();
	}

	/**
	 * Generates new keys
	 * @param keySize the size of the keys to generate, in bits
	 * @return the keyset that was generated
	 * @throws GeneralSecurityException when keys cannot be generated
	 */
	public static final RSAKeySet generateKeys(final int keySize) throws GeneralSecurityException {
		final RSAKeySet set;
		final long start = System.currentTimeMillis();
		final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(keySize);
		final KeyPair kp = kpg.genKeyPair();
		set = new RSAKeySet(KeyFactory.getInstance("RSA").getKeySpec(kp.getPublic(), RSAPublicKeySpec.class), kp.getPrivate());
		Log.l.config("RSA keys generated (" + (System.currentTimeMillis() - start) + "ms)");
		return set;
	}

	/**
	 * Gets the specification for our public key
	 * @return the data for the specification
	 */
	public final ByteBuffer getPublicKeySpec() {
		return localPublicKeySpec;
	}

	/** Gets our private key */
	protected final PrivateKey getPrivateKey() {
		return privateKey;
	}
}
