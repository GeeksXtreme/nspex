package org.whired.inspexi.blackbox;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.RSAPublicKeySpec;

import org.whired.inspexi.tools.logging.Log;

public class RSAKeySet {
	private final RSAPublicKeySpec localPublicKeySpec;
	private final PrivateKey privateKey;
	private final KeyFactory fact = KeyFactory.getInstance("RSA");

	/**
	 * Generates new local keys. This operation is expensive and should not be done repetitively.
	 * @throws GeneralSecurityException when keys could not be generated for any reason
	 */
	public RSAKeySet() throws GeneralSecurityException {
		final long start = System.currentTimeMillis();
		final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		final KeyPair kp = kpg.genKeyPair();
		localPublicKeySpec = fact.getKeySpec(kp.getPublic(), RSAPublicKeySpec.class);
		privateKey = kp.getPrivate();
		Log.l.info("RSA keys generated (" + (System.currentTimeMillis() - start) + "ms)");
	}

	protected final ByteBuffer getPublicKeySpec() {
		final byte[] mod = localPublicKeySpec.getModulus().toByteArray();
		final byte[] exp = localPublicKeySpec.getPublicExponent().toByteArray();
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
