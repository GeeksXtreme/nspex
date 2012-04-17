package org.whired.inspexi.master;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.imageio.ImageIO;

import org.whired.inspexi.tools.SessionListener;
import org.whired.inspexi.tools.Slave;
import org.whired.inspexi.tools.logging.Log;

public class RemoteSlaveModel extends Slave implements SessionListener, ImageConsumer {
	private final Socket socket = new Socket();
	private DataInputStream dis;
	private DataOutputStream dos;
	private SessionListener listener;
	private final String ip;
	private ImageConsumer consumer;

	public RemoteSlaveModel(final String ip, final int port) throws IOException {
		this.ip = ip;
		socket.connect(new InetSocketAddress(ip, port), 250);
	}

	public void connect(final int intent) {
		try {
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			dos.write(intent);
			if (intent == INTENT_CHECK || intent == INTENT_CHECK_BULK || intent == INTENT_CONNECT) {
				socket.setSoTimeout(2500);
				setHost(dis.readUTF());
				setOS(dis.readUTF());
				setVersion(dis.readUTF());
				imageResized(dis.readShort(), dis.readShort());
				if (intent != INTENT_CONNECT) {
					if (intent == INTENT_CHECK) {
						// Read preview if this isn't a bulk check
						final int imgLen = dis.readInt();
						final byte[] buf = new byte[imgLen];
						int read = 0;
						while ((read += dis.read(buf, read, imgLen - read)) != imgLen) {
							;
						}
						imageProduced(ImageIO.read(new ByteArrayInputStream(buf)));
					}
					endSession("Check successful");
				}
				else {
					socket.setSoTimeout(0);
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								if (!getVersion().equals(VERSION)) {
									Log.l.warning("Remote version differs! (" + VERSION + " vs " + getVersion() + ")");
								}

								// The business
								new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											int op;
											while ((op = dis.read()) != -1) {
												switch (op) {
													case 0:
														final int imgLen = dis.readInt();
														final byte[] buf = new byte[imgLen];
														int read = 0;
														while ((read += dis.read(buf, read, imgLen - read)) != imgLen) {
															;
														}
														imageProduced(ImageIO.read(new ByteArrayInputStream(buf)));
													break;
												}
											}
											endSession("End of stream");
										}
										catch (final Throwable t) {
											endSession(t.toString());
										}
									}
								}, "SlavePacketReceiver").start();
							}
							catch (final Throwable t) {
								endSession(t.toString());
							}
						}
					}, "SlaveConnecter").start();
				}
			}
		}
		catch (final IOException e) {
			endSession(e.toString());
		}

	}

	public void executeRemoteCommand(final String command) {
		try {
			dos.write(OP_DO_COMMAND);
			dos.writeUTF(command);
		}
		catch (final Throwable t) {
			endSession(t.toString());
		}
	}

	protected void endSession(final String reason) {
		try {
			if (socket != null) {
				socket.close();
			}
		}
		catch (final IOException e) {
		}
		sessionEnded(reason, null);
	}

	public String getIp() {
		return this.ip;
	}

	public void setSessionListener(final SessionListener listener) {
		this.listener = listener;
	}

	public void setImageConsumer(final ImageConsumer consumer) {
		this.consumer = consumer;
	}

	@Override
	public void sessionEnded(final String reason, final Throwable t) {
		if (listener != null) {
			listener.sessionEnded(reason, t);
		}
		Log.l.info("Session with " + ip + " ended: " + reason);
	}

	@Override
	public void imageProduced(final Image image) {
		if (consumer != null) {
			consumer.imageProduced(image);
		}
	}

	@Override
	public void imageResized(final int width, final int height) {
		if (consumer != null) {
			consumer.imageResized(width, height);
		}
	}
}