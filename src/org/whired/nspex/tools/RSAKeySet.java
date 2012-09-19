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
	private final RSAPublicKeySpec localPublicKeySpec;
	/** Our private key */
	private final PrivateKey privateKey;

	/**
	 * Generates new local keys. This operation is expensive and should not be done repetitively.
	 * @throws GeneralSecurityException when keys could not be generated for any reason
	 */
	public RSAKeySet(final int keySize) throws GeneralSecurityException {
		final long start = System.currentTimeMillis();
		final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(keySize);
		final KeyPair kp = kpg.genKeyPair();
		localPublicKeySpec = KeyFactory.getInstance("RSA").getKeySpec(kp.getPublic(), RSAPublicKeySpec.class);
		privateKey = kp.getPrivate();
		Log.l.config("RSA keys generated (" + (System.currentTimeMillis() - start) + "ms)");
	}

	/**
	 * Gets the specification for our public key
	 * @return the data for the specification
	 */
	public final ByteBuffer getPublicKeySpec() {
		final byte[] mod = localPublicKeySpec.getModulus().toByteArray();
		final byte[] exp = localPublicKeySpec.getPublicExponent().toByteArray();
		// Allocate lengths and account for int size (4+4)
		final ByteBuffer buf = ByteBuffer.allocate(mod.length + exp.length + 8);
		buf.putInt(mod.length);
		buf.put(mod);
		buf.putInt(exp.length);
		buf.put(exp);
		buf.flip();
		return buf;
	}

	protected final PrivateKey getPrivateKey() {
		return privateKey;
	}
}
