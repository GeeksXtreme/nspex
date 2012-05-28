package org.whired.inspexi.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Reads organized packet data from a channel into a buffer
 * @author Whired
 */
public abstract class NioCommunicable extends Communicable {
	/** The header -- 1 byte for id, 4 for size */
	private final ByteBuffer headerBuffer = ByteBuffer.allocate(5);
	/** The payload */
	private ByteBuffer payloadBuffer;
	/** The channel to buffer from */
	final SocketChannel channel;
	/** The id of the buffered packet data */
	private int id;
	/** The size of the buffered packet data */
	private int size;
	/** The key for this communicable */
	private final SelectionKey key;

	/**
	 * Creates a new reader for the specified channel
	 * @param channel the channel to read from
	 */
	public NioCommunicable(SelectionKey key) {
		this.key = key;
		this.channel = (SocketChannel) key.channel();
		setReadTimeout(3000);
	}

	/**
	 * Attempts to read data from {@link #channel}
	 * @return {@code false} if {@code channel.read()} returned {@code -1}, otherwise {@code true}
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	protected final int read() throws IOException {
		// We still need header data
		if (headerBuffer.hasRemaining()) {
			return readHeader();
		}
		// We're clear to read the payload, if applicable
		else {
			setReadTimeout(3000);
			return readPayload();
		}
	}

	/**
	 * Attempts to read data from {@link #channel} into {@link #headerBuffer}
	 * @return {@code false} if {@code channel.read()} returned {@code -1}, otherwise {@code true}
	 * @throws IOException if an I/O error occurs
	 */
	private final int readHeader() throws IOException {
		int val;
		if ((val = channel.read(headerBuffer)) != -1) {
			// The buffer isn't full yet, return immediately
			if (headerBuffer.hasRemaining()) {
				if (headerBuffer.position() != 0) {
					setReadTimeout(3000);
				}
				return val;
			}
			// We're ready to get the information from the header
			else {
				headerBuffer.flip();
				id = headerBuffer.get() & 0xff;
				size = headerBuffer.getInt();
				// Handle and reset if it's time
				if (size == 0) {
					handle(id);
					headerBuffer.clear();
					return val;
				}
				// There's a chance that we can read more without blocking
				else {
					int i = readPayload();
					// If readPayload is -1, we're at EOF
					return i == -1 ? i : val + i;
				}
			}
		}
		else {
			return val;
		}
	}

	/**
	 * Attempts to read data from {@link #channel} into {@link #payloadBuffer}
	 * @return {@code false} if {@code channel.read()} returned {@code -1}, otherwise {@code true}
	 * @throws IOException if an I/O error occurs
	 */
	private int readPayload() throws IOException {
		int val;
		if ((val = channel.read(payloadBuffer == null ? (payloadBuffer = ByteBuffer.allocateDirect(size)) : payloadBuffer)) != -1) {
			if (!payloadBuffer.hasRemaining()) {
				// All done, clean up and notify that the packet is ready
				setReadTimeout(30 * 60000);
				payloadBuffer.flip();
				handle(id, payloadBuffer.asReadOnlyBuffer());
				payloadBuffer = null;
				headerBuffer.clear();
			}
		}
		return val;
	}

	@Override
	public final void send(int id) {
		ByteBuffer packet = ByteBuffer.allocate(5);
		packet.put((byte) id);
		packet.putInt(0);
		packet.flip();

		try {
			// TODO Sleep here and DC purposely to see result
			channel.write(packet);
		}
		catch (IOException e) {
			// Maybe..?
			disconnect();
		}
	}

	@Override
	public final void send(int id, ByteBuffer payload) {
		payload.flip();
		ByteBuffer packet = ByteBuffer.allocate(payload.capacity() + 5);
		packet.put((byte) id);
		packet.putInt(payload.capacity());
		packet.put(payload);
		packet.flip();

		try {
			channel.write(packet);
		}
		catch (IOException e) {
			// Maybe..?
			disconnect();
		}
	}

	@Override
	public final void disconnect() {
		connected = false;
		try {
			key.channel().close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}