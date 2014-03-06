package org.whired.nspex.net;

import java.nio.ByteBuffer;

/**
 * Represents an expandable {@link ByteBuffer}
 * @author Martin Tuskevicius
 * @author Whired
 */
public class ExpandableByteBuffer {
	/**
	 * The default size of the buffer
	 */
	private static final int BUFFER_SIZE = 1024;
	private ByteBuffer buf;

	/**
	 * Creates a new expandable {@link ByteBuffer}
	 */
	public ExpandableByteBuffer() {
		this.buf = ByteBuffer.allocate(BUFFER_SIZE);
	}

	/**
	 * Creates a new expandable {@link ByteBuffer} containing data from the specified {@code ByteBuffer}
	 * @param buf
	 */
	public ExpandableByteBuffer(final ByteBuffer buf) {
		this.buf = buf;
	}

	/**
	 * Creates a new expandable byte buffer with the specified initial capacity
	 * @param capacity the initial capacity
	 */
	public ExpandableByteBuffer(final int capacity) {
		buf = ByteBuffer.allocate(capacity);
	}

	/**
	 * Puts a {@code byte} into this buffer
	 * @param b
	 * @return this buffer, for chaining
	 */
	public ExpandableByteBuffer put(final int b) {
		ensureCapacity(1);
		buf.put((byte) b);
		return this;
	}

	/**
	 * Puts a {@code byte[]} into this buffer
	 * @param bytes
	 * @return this buffer, for chaining
	 */
	public ExpandableByteBuffer put(final byte[] bytes) {
		ensureCapacity(bytes.length);
		buf.put(bytes);
		return this;
	}

	/**
	 * Puts a {@code byte[]} into this buffer
	 * @param bytes
	 * @param offset the offset in the array to begin copying from
	 * @param length the length of bytes in the array to copy
	 * @return this buffer, for chaining
	 */
	public ExpandableByteBuffer put(final byte[] bytes, final int offset, final int length) {
		ensureCapacity(length);
		buf.put(bytes, offset, length);
		return this;
	}

	/**
	 * Puts the data from another {@link ByteBuffer} into this buffer
	 * @param from the {@code ByteBuffer} to copy information from
	 * @return this buffer, for chaining
	 */
	public ExpandableByteBuffer put(final ByteBuffer from) {
		ensureCapacity(from.capacity() - from.remaining());
		buf.put(from);
		return this;
	}

	/**
	 * Puts the specified {@code char} into the this buffer
	 * @param c
	 * @return this buffer, for chaining
	 */
	public ExpandableByteBuffer putChar(final char c) {
		ensureCapacity(2);
		buf.putChar(c);
		return this;
	}

	/**
	 * Puts the specified {@code short} into this buffer
	 * @param s
	 * @return this buffer, for chaining
	 */
	public ExpandableByteBuffer putShort(final short s) {
		ensureCapacity(2);
		buf.putShort(s);
		return this;
	}

	/**
	 * Puts the specified {@code int} into this buffer
	 * @param i
	 * @return this buffer, for chaining
	 */
	public ExpandableByteBuffer putInt(final int i) {
		ensureCapacity(4);
		buf.putInt(i);
		return this;
	}

	/**
	 * Puts the specified {@code float} into this buffer
	 * @param f
	 * @return this buffer, for chaining
	 */
	public ExpandableByteBuffer putFloat(final float f) {
		ensureCapacity(4);
		buf.putFloat(f);
		return this;
	}

	/**
	 * Puts the specified {@code double} into this buffer
	 * @param d
	 * @return this buffer, for chaining
	 */
	public ExpandableByteBuffer putDouble(final double d) {
		ensureCapacity(8);
		buf.putDouble(d);
		return this;
	}

	/**
	 * Puts the specified {@code long} into this buffer
	 * @param l
	 * @return this buffer, for chaining
	 */
	public ExpandableByteBuffer putLong(final long l) {
		ensureCapacity(8);
		buf.putLong(l);
		return this;
	}

	/**
	 * Encodes the specified {@code String} to JTF and puts it into this buffer
	 * @param s
	 * @return this buffer, for chaining
	 */
	public ExpandableByteBuffer putJTF(final String s) {
		final byte[] enc = BufferUtil.encodeJTF(s);
		ensureCapacity(2 + enc.length);
		buf.putShort((short) enc.length);
		buf.put(enc);
		return this;
	}

	/**
	 * Retrieves a compressed version of this buffer. The returned buffer will be compressed so that limit=capacity.
	 * @return the buffer
	 */
	public ByteBuffer asByteBuffer() {
		buf.flip();
		return buf = ByteBuffer.allocate(buf.limit()).put(buf);
	}

	/**
	 * Retrieves the writable version of this buffer. All the bytes from the internal {@link ByteBuffer} are taken and put into a byte array (with the size of the {@link ByteBuffer#position()}, and then the array is wrapped and returned (<tt>return ByteBuffer.wrap(array)</tt>).
	 * @return a writable version of this buffer
	 */
	public ByteBuffer getWritableBuffer() {
		final byte[] data = new byte[buf.position()];
		buf.get(data);
		return ByteBuffer.wrap(data);
	}

	/**
	 * Expands this buffer if more space needs to be allocated
	 */
	private void ensureCapacity(final int amount) {
		if (buf.remaining() >= amount) {
			return;
		}
		final ByteBuffer buf = ByteBuffer.allocate((this.buf.capacity() + amount) * 3 / 2 + 1);
		this.buf.flip();
		buf.put(this.buf);
		this.buf = buf;
	}
}
