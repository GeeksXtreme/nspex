package org.whired.inspexi.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.whired.inspexi.tools.logging.Log;

public abstract class IoCommunicable extends Communicable {
	private final DataInputStream dis;
	private final DataOutputStream dos;
	private final Socket socket;

	public IoCommunicable(final Socket socket) throws IOException {
		this.socket = socket;
		dis = new DataInputStream(socket.getInputStream());
		dos = new DataOutputStream(socket.getOutputStream());

		new Thread(new Runnable() {
			@Override
			public void run() {
				int op;
				try {
					while ((op = dis.read()) != -1) {
						op &= 0xFF; // Unsign
						socket.setSoTimeout(3000);
						final byte[] toFill = new byte[dis.readInt()];
						if (toFill.length > 0) {
							dis.readFully(toFill);
							Log.l.config("Packet recevied=" + op + " length=" + toFill.length);
							handle(op, ByteBuffer.wrap(toFill).asReadOnlyBuffer());
						}
						else {
							Log.l.config("Packet recevied=" + op + " length=0");
							handle(op);
						}
						socket.setSoTimeout(60000 * 30);
					}
				}
				catch (final IOException e) {
				}
				disconnect();
			}
		}).start();
	}

	@Override
	public final void send(final int id) {
		Log.l.config("Sending packet=" + id + " length=0");
		try {
			dos.write(id);
		}
		catch (final IOException e) {
			disconnect();
		}
	}

	@Override
	protected final int read() throws IOException {
		return 0;
	}

	@Override
	public final void send(final int id, final ByteBuffer payload) {
		Log.l.config("Sending packet=" + id + " length=" + payload.capacity() + " pos=" + payload.position() + " rem=" + payload.remaining());
		try {
			if (payload.position() > 0) {
				payload.flip();
			}
			dos.write(id);
			final byte[] raw = new byte[payload.capacity()];
			dos.writeInt(raw.length);
			payload.get(raw, 0, raw.length);
			dos.write(raw);
		}
		catch (final IOException e) {
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
			}
			disconnected();
		}
	}
}
