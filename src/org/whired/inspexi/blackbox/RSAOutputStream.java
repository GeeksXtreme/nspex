package org.whired.inspexi.blackbox;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

public strictfp class RSAOutputStream {

	private final DataOutputStream dos;
	private final RSASession rsa;

	/**
	 * Creates a secure output stream by utilizing the specified output stream and rsa session
	 * @param dos the data output stream to use
	 * @param rsa the rsa session to use
	 */
	public RSAOutputStream(DataOutputStream dos, RSASession rsa) {
		this.dos = dos;
		this.rsa = rsa;
	}

	private int count;

	public int size() {
		return count;
	}

	public void write(byte[] b) throws IOException, GeneralSecurityException {
		byte[] enc = rsa.encrpyt(b);
		dos.writeInt(enc.length);
		dos.write(enc);
		count += enc.length;
	}

	public void write(byte[] b, int off, int len) throws IOException, GeneralSecurityException {
		byte[] enc = new byte[len];
		System.arraycopy(b, off, enc, 0, enc.length);
		write(enc);
	}

	public void writeBoolean(boolean v) throws IOException, GeneralSecurityException {
		// Encrypting a boolean seems a little silly..
		// Sacrifice ALL the performance!
		write(new byte[] { (v ? (byte) 1 : (byte) 0) });
	}

	public void writeByte(int v) throws IOException, GeneralSecurityException {
		write(new byte[] { (byte) v });
	}

	public void writeChar(int v) throws IOException, GeneralSecurityException {
		// TODO Not really sure how to handle this one..
		writeShort(v);
	}

	public void writeChars(String s) throws IOException, GeneralSecurityException {
		// TODO Not really sure how to handle this one..
		byte[] enc = new byte[s.length() * 2];
		for (int i = 0; i < s.length();) {
			int v = s.charAt(i);
			enc[i++] = (byte) ((v >>> 8) & 0xFF);
			enc[i++] = (byte) ((v >>> 0) & 0xFF);
		}
		write(enc);
	}

	public void writeDouble(double v) throws IOException, GeneralSecurityException {
		writeLong(Double.doubleToLongBits(v));
	}

	public void writeFloat(float v) throws IOException, GeneralSecurityException {
		writeInt(Float.floatToIntBits(v));
	}

	public void writeInt(int v) throws IOException, GeneralSecurityException {
		write(new byte[] { (byte) ((v >>> 24) & 0xFF), (byte) ((v >>> 16) & 0xFF), (byte) ((v >>> 8) & 0xFF), (byte) ((v >>> 0) & 0xFF) });
	}

	public void writeLong(long v) throws IOException, GeneralSecurityException {
		write(new byte[] { (byte) (v >>> 56), (byte) (v >>> 48), (byte) (v >>> 40), (byte) (v >>> 32), (byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) (v >>> 0) });
	}

	public void writeShort(int v) throws IOException, GeneralSecurityException {
		write(new byte[] { (byte) ((v >>> 8) & 0xFF), (byte) ((v >>> 0) & 0xFF) });
	}

	public void writeUTF(String s) throws IOException, GeneralSecurityException {
		write(new String(s).getBytes("utf8"));
	}

}
