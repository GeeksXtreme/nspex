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
	 * Creates a new expandable {@link ByteBuffer} by allocating a new buffer with the default buffer size ({@link Constants#BUFFER_SIZE}).
	 */
	public ExpandableByteBuffer() {
		buf = ByteBuffer.allocate(BUFFER_SIZE);
	}

	/**
	 * Creates a new expandable {@link ByteBuffer}, initializing the ByteBuffer instance stored using the provided one.
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
	 * Puts a single byte into the buffer.
	 * @param b
	 */
	public ExpandableByteBuffer put(final int b) {
		ensureCapacity(1);
		buf.put((byte) b);
		return this;
	}

	/**
	 * Puts an array of bytes into the buffer.
	 * @param bytes
	 */
	public ExpandableByteBuffer put(final byte[] bytes) {
		ensureCapacity(bytes.length);
		buf.put(bytes);
		return this;
	}

	/**
	 * Puts an array of bytes from the specified offset into the buffer.
	 * @param bytes
	 * @param offset
	 * @param length
	 */
	public ExpandableByteBuffer put(final byte[] bytes, final int offset, final int length) {
		ensureCapacity(length);
		buf.put(bytes, offset, length);
		return this;
	}

	/**
	 * Puts the data from another {@link ByteBuffer} into this buffer.
	 * @param from
	 */
	public ExpandableByteBuffer put(final ByteBuffer from) {
		ensureCapacity(from.capacity() - from.remaining());
		buf.put(from);
		return this;
	}

	/**
	 * Puts a character into the the buffer.
	 * @param c
	 */
	public ExpandableByteBuffer putChar(final char c) {
		ensureCapacity(2);
		buf.putChar(c);
		return this;
	}

	/**
	 * Puts a short into the buffer.
	 * @param s
	 */
	public ExpandableByteBuffer putShort(final short s) {
		ensureCapacity(2);
		buf.putShort(s);
		return this;
	}

	/**
	 * Puts an integer into the buffer.
	 * @param i
	 */
	public ExpandableByteBuffer putInt(final int i) {
		ensureCapacity(4);
		buf.putInt(i);
		return this;
	}

	/**
	 * Puts a float into the buffer.
	 * @param f
	 */
	public ExpandableByteBuffer putFloat(final float f) {
		ensureCapacity(4);
		buf.putFloat(f);
		return this;
	}

	/**
	 * Puts a double into the buffer.
	 * @param d
	 */
	public ExpandableByteBuffer putDouble(final double d) {
		ensureCapacity(8);
		buf.putDouble(d);
		return this;
	}

	/**
	 * Puts a long into the buffer.
	 * @param l
	 */
	public ExpandableByteBuffer putLong(final long l) {
		ensureCapacity(8);
		buf.putLong(l);
		return this;
	}

	public ExpandableByteBuffer putJTF(final String s) {
		final byte[] enc = BufferUtil.encodeJTF(s);
		ensureCapacity(2 + enc.length);
		buf.putShort((short) enc.length);
		buf.put(enc);
		return this;
	}

	/**
	 * Retrieves a compressed version of this buffer The returned buffer will be compressed so that limit=capacity
	 * @return the buffer
	 */
	public ByteBuffer asByteBuffer() {
		buf.flip();
		return buf = ByteBuffer.allocate(buf.limit()).put(buf);
	}

	/**
	 * Retrieves the writable version of this buffer. All the bytes from the internal {@link ByteBuffer} are taken and put into a byte array (with the size of the {@link ByteBuffer#position()}, and then the array is wrapped and returned (<tt>return ByteBuffer.wrap(array)</tt>).
	 * <p>
	 * <i>This is an optional method, this is just more convenient than doing so manually!</i>
	 * </p>
	 * @return a writable version of this buffer
	 */
	public ByteBuffer getWritableBuffer() {
		final byte[] data = new byte[buf.position()];
		buf.get(data);
		return ByteBuffer.wrap(data);
	}

	/**
	 * Checks if more space needs to be allocated.
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
