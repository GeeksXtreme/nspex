package org.whired.inspexi.master;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.whired.inspexi.tools.NetTask;
import org.whired.inspexi.tools.NetTaskQueue;
import org.whired.inspexi.tools.RemoteFile;
import org.whired.inspexi.tools.RemoteFileChooserDialog;
import org.whired.inspexi.tools.SessionListener;
import org.whired.inspexi.tools.Slave;
import org.whired.inspexi.tools.logging.Log;

public class RemoteSlave extends Slave implements SessionListener, ImageConsumer {
	private final Socket socket = new Socket();
	private DataInputStream dis;
	private DataOutputStream dos;
	private SessionListener listener;
	private ImageConsumer consumer;

	public RemoteSlave(final String ip, final int port) throws IOException {
		super(ip);
		socket.connect(new InetSocketAddress(ip, port), 250);
		setOnline(true);
	}

	public void connect(final int intent) throws IOException {
		//try {
		dis = new DataInputStream(socket.getInputStream());
		dos = new DataOutputStream(socket.getOutputStream());
		dos.write(intent);
		if (intent == INTENT_CHECK || intent == INTENT_CHECK_BULK || intent == INTENT_CONNECT) {
			socket.setSoTimeout(2500);
			setUser(dis.readUTF());
			setOS(dis.readUTF());
			setVersion(dis.readUTF());
			imageResized(dis.readShort(), dis.readShort());
			if (intent != INTENT_CONNECT) {
				if (intent == INTENT_CHECK) {
					// Read preview if this isn't a bulk check
					final int imgLen = dis.readInt();
					final byte[] buf = new byte[imgLen];
					dis.readFully(buf);
					imageProduced(ImageIO.read(new ByteArrayInputStream(buf)));
				}
				socket.close();
			}
			else {
				// Reset the timeout, we're going to stick around
				socket.setSoTimeout(0);
				browseFiles();
				if (!getVersion().equals(VERSION)) {
					Log.l.warning("Remote version differs! (" + VERSION + " vs " + getVersion() + ")");
				}
				NetTaskQueue.add(new NetTask(listener, socket) {
					@Override
					public void run(DataInputStream dis, DataOutputStream dos) throws IOException, GeneralSecurityException {
						socket.setSoTimeout(10);
						timeoutFatal = false;
						int op;
						while ((op = dis.read()) != -1) {
							socket.setSoTimeout(25000);
							timeoutFatal = true;
							System.out.println(Thread.currentThread().getName() + " handling packet " + op);
							switch (op) {
								case OP_TRANSFER_IMAGE:
									int imgLen = dis.readInt();
									byte[] buf = new byte[imgLen];
									dis.readFully(buf);
									imageProduced(ImageIO.read(new ByteArrayInputStream(buf)));
								break;

								case OP_GET_FILES:
									String parentPath = dis.readUTF();
									RemoteFile[] rf = new RemoteFile[dis.readInt()];
									for (int i = 0; i < rf.length; i++) {
										rf[i] = new RemoteFile(dis.readUTF(), dis.readBoolean());
									}
									if (dialog != null) {
										dialog.addChildren(parentPath, rf);
									}
								break;
								case OP_GET_FILE_THUMB:
									imgLen = dis.readInt();
									buf = new byte[imgLen];
									dis.readFully(buf);
									if (dialog != null) {
										dialog.setThumbnail(ImageIO.read(new ByteArrayInputStream(buf)));
									}
								break;
								default:
									throw new IOException("Unhandled operation: " + op);
							}
							socket.setSoTimeout(10);
							timeoutFatal = false;
						}
						throw new IOException("End of stream");
					}
				});
			}
		}
	}

	RemoteFileChooserDialog dialog;

	public void browseFiles() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				dialog = new RemoteFileChooserDialog() {

					@Override
					protected void requestThumbnail(final String path) {
						NetTaskQueue.add(new NetTask(RemoteSlave.this, socket) {
							@Override
							public void run(DataInputStream dis, DataOutputStream dos) throws IOException, GeneralSecurityException {
								dos.write(OP_GET_FILE_THUMB);
								dos.writeUTF(path);
							}
						});
					}

					@Override
					protected void requestChildren(final String parentPath) {
						NetTaskQueue.add(new NetTask(RemoteSlave.this, socket) {
							@Override
							public void run(DataInputStream dis, DataOutputStream dos) throws IOException, GeneralSecurityException {
								dos.write(OP_GET_FILES);
								dos.writeUTF(parentPath);
							}
						});
					}
				};
				dialog.setVisible(true);
			}
		});

	}

	public void executeRemoteCommand(final String command) {
		NetTaskQueue.add(new NetTask(RemoteSlave.this, socket) {
			@Override
			public void run(DataInputStream dis, DataOutputStream dos) throws IOException, GeneralSecurityException {
				dos.write(OP_DO_COMMAND);
				dos.writeUTF(command);
			}
		});
	}

	public void setSessionListener(final SessionListener listener) {
		this.listener = listener;
	}

	public void setImageConsumer(final ImageConsumer consumer) {
		this.consumer = consumer;
	}

	@Override
	public void sessionEnded(final String reason, final Throwable t) {
		if (socket != null) {
			try {
				socket.close();
			}
			catch (IOException e) {
			}
		}
		if (listener != null) {
			listener.sessionEnded(reason, t);
		}
		Log.l.info("Session with " + getIp() + " ended: " + reason);
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

	@Override
	public String toString() {
		return getIp();
	}
}