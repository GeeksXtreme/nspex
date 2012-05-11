package org.whired.inspexi.blackbox;

import java.io.DataInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

public strictfp class RSAInputStream {
	private final RSASession rsa;
	private final DataInputStream dis;

	/**
	 * Creates a secure input stream by utilizing the specified input stream and rsa session
	 * @param dis the data input stream to use
	 * @param rsa the rsa session to use
	 */
	public RSAInputStream(DataInputStream dis, RSASession rsa) {
		this.dis = dis;
		this.rsa = rsa;
	}

	public boolean readBoolean() throws IOException, GeneralSecurityException {
		return readFully()[0] == 1;
	}

	public byte readByte() throws IOException, GeneralSecurityException {
		return readFully()[0];
	}

	public char readChar() throws IOException, GeneralSecurityException {
		return (char) readShort();
	}

	public double readDouble() throws IOException, GeneralSecurityException {
		return Double.longBitsToDouble(readLong());
	}

	public float readFloat() throws IOException, GeneralSecurityException {
		return Float.intBitsToFloat(readInt());
	}

	public byte[] readFully() throws IOException, GeneralSecurityException {
		// These will throw EOF, so any manuals are redundant
		byte[] dec = new byte[dis.readInt()];
		dis.readFully(dec);
		return rsa.decrypt(dec);
	}

	public int readInt() throws IOException, GeneralSecurityException {
		byte[] dec = readFully();
		return (((dec[0] & 0xFF) << 24) + ((dec[1] & 0xFF) << 16) + ((dec[2] & 0xFF) << 8) + ((dec[3] & 0xFF) << 0));
	}

	public long readLong() throws IOException, GeneralSecurityException {
		byte[] dec = readFully();
		return (((long) dec[0] << 56) + ((long) (dec[1] & 255) << 48) + ((long) (dec[2] & 255) << 40) + ((long) (dec[3] & 255) << 32) + ((long) (dec[4] & 255) << 24) + ((dec[5] & 255) << 16) + ((dec[6] & 255) << 8) + ((dec[7] & 255) << 0));
	}

	public short readShort() throws IOException, GeneralSecurityException {
		byte[] dec = readFully();
		return (short) (((dec[0] & 0xFF) << 8) + ((dec[1] & 0xFF) << 0));
	}

	public String readUTF() throws IOException, GeneralSecurityException {
		return new String(readFully(), "utf8");
	}
}
