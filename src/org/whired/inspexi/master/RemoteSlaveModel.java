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

	public RemoteSlaveModel(String ip, int port) throws IOException {
		this.ip = ip;
		socket.connect(new InetSocketAddress(ip, port), 250);
	}

	public void connect(int intent) {
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
						int imgLen = dis.readInt();
						byte[] buf = new byte[imgLen];
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
													int imgLen = dis.readInt();
													byte[] buf = new byte[imgLen];
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
										catch (Throwable t) {
											endSession(t.toString());
										}
									}
								}, "SlavePacketReceiver").start();
							}
							catch (Throwable t) {
								endSession(t.toString());
							}
						}
					}, "SlaveConnecter").start();
				}
			}
		}
		catch (IOException e) {
			endSession(e.toString());
		}

	}

	public void executeRemoteCommand(String command) {
		try {
			dos.write(OP_DO_COMMAND);
			dos.writeUTF(command);
		}
		catch (Throwable t) {
			endSession(t.toString());
		}
	}

	protected void endSession(String reason) {
		try {
			if (socket != null) {
				socket.close();
			}
		}
		catch (IOException e) {
		}
		sessionEnded(reason);
	}

	public String getIp() {
		return this.ip;
	}

	public void setSessionListener(SessionListener listener) {
		this.listener = listener;
	}

	public void setImageConsumer(ImageConsumer consumer) {
		this.consumer = consumer;
	}

	@Override
	public void sessionEnded(String reason) {
		if (listener != null) {
			listener.sessionEnded(reason);
		}
		Log.l.info("Session with " + ip + " ended: " + reason);
	}

	@Override
	public void imageProduced(Image image) {
		if (consumer != null) {
			consumer.imageProduced(image);
		}
	}

	@Override
	public void imageResized(int width, int height) {
		if (consumer != null) {
			consumer.imageResized(width, height);
		}
	}
}