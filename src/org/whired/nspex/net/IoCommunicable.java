package org.whired.nspex.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.whired.nspex.tools.logging.Log;

/**
 * A communicable that utilizes {@code java.io} streams
 * @author whired
 */
public abstract class IoCommunicable extends Communicable {
	/** The input stream for this communicable */
	private final DataInputStream dis;
	/** The output stream for this communicable */
	private final DataOutputStream dos;
	/** The socket for this communicable */
	private final Socket socket;

	/**
	 * Creates a new IO communicable for the specified {@code Socket}
	 * @param socket the socket for that this communicable will use
	 * @throws IOException
	 */
	public IoCommunicable(final Socket socket) throws IOException {
		// Provide the host name
		super(((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().getHostAddress());
		this.socket = socket;
		dis = new DataInputStream(socket.getInputStream());
		dos = new DataOutputStream(socket.getOutputStream());

		// IO streams block, create a new thread
		new Thread(new Runnable() {
			@Override
			public void run() {
				int op; // The op code
				try {
					while ((op = dis.read()) != -1) {
						op &= 0xFF; // Unsign the byte
						socket.setSoTimeout(3500);
						int fillLen = dis.readInt(); // Read the length of the packet
						final byte[] toFill = new byte[fillLen];
						if (toFill.length > 0) {
							dis.readFully(toFill); // Read the packet
							Log.l.fine("[" + IoCommunicable.this + "] Packet received=" + op + " length=" + toFill.length);
							ByteBuffer payload = ByteBuffer.wrap(toFill).asReadOnlyBuffer();
							if (op != REMOTE_LOG) {
								handle(op, payload);
							}
							else {
								remoteLogged(Level.parse("" + (payload.getInt())), BufferUtil.getJTF(payload));
							}
						}
						else {
							Log.l.fine("[" + IoCommunicable.this + "] Packet received=" + op + " length=0");
							handle(op);
						}
						socket.setSoTimeout((int) TimeUnit.MINUTES.toMillis(30)); // Wait up to 30 minutes for a new packet
					}
				}
				catch (final IOException e) {
					Log.l.log(Level.FINE, "", e);
				}
				finally {
					disconnect();
				}
			}
		}).start();
	}

	@Override
	public final void send(final int id) {
		Log.l.fine("[" + this + "] Sending packet=" + id + " length=0");
		try {
			dos.write(id);
		}
		catch (final IOException e) {
			Log.l.log(Level.FINE, "", e);
			disconnect();
		}
	}

	@Override
	protected final int read() throws IOException {
		return 0; // Reads are done internally, so this is unused
	}

	@Override
	public final void send(final int id, final ByteBuffer payload) {
		try {
			if (payload.position() > 0) {
				payload.flip();
			}
			Log.l.fine("[" + this + "] Sending packet=" + id + " length=" + payload.capacity() + " pos=" + payload.position() + " rem=" + payload.remaining());
			dos.write(id);
			final byte[] raw = new byte[payload.capacity()];
			dos.writeInt(raw.length);
			payload.get(raw, 0, raw.length);
			dos.write(raw);
		}
		catch (final IOException e) {
			Log.l.log(Level.FINE, "", e);
			disconnect();
		}
	}

	@Override
	public final void disconnect() {
		if (connected) {
			connected = false;
			try {
				socket.close();
			}
			catch (final IOException e) {
				Log.l.log(Level.FINE, "", e);
			}
			disconnected();
			Log.l.config("[" + this + "] Disconnected");
		}
	}
}
