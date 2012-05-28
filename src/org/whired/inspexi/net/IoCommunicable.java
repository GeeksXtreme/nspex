package org.whired.inspexi.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

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
						byte[] toFill = new byte[dis.readInt()];
						if (toFill.length > 0) {
							dis.readFully(toFill);
							handle(op, ByteBuffer.wrap(toFill));
						}
						else {
							handle(op);
						}
						socket.setSoTimeout(60000 * 30);
					}
				}
				catch (IOException e) {
				}
				disconnect();
			}
		}).start();
	}

	@Override
	public final void send(int id) {
		try {
			dos.write(id);
		}
		catch (IOException e) {
			disconnect();
		}
	}

	@Override
	protected final int read() throws IOException {
		return 0;
	}

	@Override
	public final void send(int id, ByteBuffer payload) {
		try {
			payload.flip();
			dos.write(id);
			byte[] raw = new byte[payload.limit()];
			dos.writeInt(raw.length);
			payload.get(raw, 0, raw.length);
			dos.write(raw);
		}
		catch (IOException e) {
			disconnect();
		}
	}

	@Override
	public final void disconnect() {
		connected = false;
		try {
			socket.close();
		}
		catch (IOException e) {
		}
		disconnected();
	}
}
