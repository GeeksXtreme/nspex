package org.whired.nspex.net;

import java.nio.ByteBuffer;

public class BufferUtil {
	/**
	 * Encodes a string in JTF16
	 * @author Killer99, Whired
	 * @param string the string to encode
	 * @return the encoded bytes
	 */
	public static byte[] encodeJTF(final String string) {

		// Skip the linear-time counting iteration
		final char[] chrArr = string.toCharArray();
		// We can assume max length (len * 3)
		final byte[] encoded = new byte[string.length() * 3];
		int idx = 0;
		for (final int chr : chrArr) {
			if (chr < 0x80) {
				encoded[idx++] = (byte) chr;
			}
			else if (chr < 0x3fff) {
				encoded[idx++] = (byte) (chr | 0x80);
				encoded[idx++] = (byte) (chr >>> 7);
			}
			else {
				encoded[idx++] = (byte) (chr | 0x80);
				encoded[idx++] = (byte) (chr >>> 7 | 0x80);
				encoded[idx++] = (byte) (chr >>> 14);
			}
		}
		// Skip constant time and use arraycopy
		final byte[] sizedEncoded = new byte[idx];
		System.arraycopy(encoded, 0, sizedEncoded, 0, idx);
		return sizedEncoded;
	}

	/**
	 * Decodes a JTF16-encoded string
	 * @author Killer99, Whired
	 * @param encoded the encoded bytes
	 * @return the decoded string
	 */
	public static String decodeJTF(final byte[] encoded) {
		int offset = 0;
		int length = encoded.length;
		final char[] chars = new char[length];
		int count = 0;
		for (length += offset; offset != length; ++count) {
			final byte v1 = encoded[offset++];
			if (v1 >= 0) {
				chars[count] = (char) v1;
			}
			else if (offset != length) {
				final byte v2 = encoded[offset++];
				if (v2 >= 0) {
					chars[count] = (char) (v1 & 0x7f | (v2 & 0x7f) << 7);
				}
				else if (offset != length) {
					chars[count] = (char) (v1 & 0x7f | (v2 & 0x7f) << 7 | (encoded[offset++] & 0x3) << 14);
				}
				else {
					break;
				}
			}
			else {
				break;
			}
		}
		return new String(chars, 0, count);
	}

	/**
	 * Gets a JTF String from the specified {@code ByteBuffer}
	 * @param buf the {@code ByteBuffer} that contains the {@code String}
	 * @return the decoded {@code String}
	 */
	public static String getJTF(final ByteBuffer buf) {
		final byte[] enc = new byte[buf.getShort() & 0xffff];
		buf.get(enc);
		return decodeJTF(enc);
	}

	/**
	 * Encodes the specified {@code String} to JTF and puts it into the specified {@code ByteBuffer}
	 * @param buf the {@code ByteBuffer} to encode the {@code String} into
	 * @param str the {@code String} to encode
	 * @return the modified {@code ByteBuffer}
	 */
	public static ByteBuffer putJTF(final ByteBuffer buf, final String str) {
		final byte[] toEnc = encodeJTF(str);
		return buf.putShort((short) toEnc.length).put(toEnc);
	}
}
