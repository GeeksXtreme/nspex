package org.whired.nspex.master;

import java.awt.Dimension;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.whired.nspex.net.BufferUtil;
import org.whired.nspex.net.Communicable;
import org.whired.nspex.net.ExpandableByteBuffer;
import org.whired.nspex.net.IoCommunicable;
import org.whired.nspex.tools.DefaultSlave;
import org.whired.nspex.tools.RemoteFile;
import org.whired.nspex.tools.Slave;
import org.whired.nspex.tools.SlaveModel;
import org.whired.nspex.tools.SlaveView;
import org.whired.nspex.tools.logging.Log;

public class RemoteSlave extends DefaultSlave implements SlaveModel {
	private Socket socket = new Socket();
	private SlaveView view;
	private IoCommunicable comm;
	private InetSocketAddress endpoint;

	public RemoteSlave(final String ip) {
		super(ip);
	}

	/**
	 * Connects to {@link #endpoint} if not already connected. If already connected, this method returns immediately.
	 * @throws IOException if a connection cannot be established
	 */
	private IoCommunicable connectToRemote() throws IOException {
		if (!socket.isConnected() || socket.isClosed()) {
			socket = new Socket();
			// Create endpoint if we haven't already (expensive operation!)
			if (endpoint == null) {
				endpoint = new InetSocketAddress(getHost(), Slave.PORT);
			}
			socket.connect(endpoint, 500);
			return comm = new IoCommunicable(socket) {

				@Override
				public void handle(final int id, final ByteBuffer payload) {
					switch (id) {
						case OP_HANDSHAKE:
							final int intent = payload.get();
							setUser(BufferUtil.getJTF(payload));
							setOS(BufferUtil.getJTF(payload));
							setVersion(BufferUtil.getJTF(payload));
							imageResized(payload.getShort(), payload.getShort());

							if (intent != INTENT_CHECK_BULK && intent != INTENT_CONNECT) {
								// Read preview if this isn't a bulk check
								final int imgLen = payload.getInt();
								final byte[] buf = new byte[imgLen];
								payload.get(buf);
								try {
									imageProduced(ImageIO.read(new GZIPInputStream(new ByteArrayInputStream(buf))));
								}
								catch (final IOException e) {
									e.printStackTrace();
								}
							}
							setOnline(true);
							view.connected(RemoteSlave.this);
						break;
						case OP_TRANSFER_IMAGE:
							byte[] image = new byte[payload.capacity()];
							payload.get(image);
							try {
								imageProduced(ImageIO.read(new GZIPInputStream(new ByteArrayInputStream(image))));
							}
							catch (final IOException e) {
								e.printStackTrace();
							}
						break;
						case OP_GET_FILES:
							final char fs = payload.getChar();
							final String parentPath = BufferUtil.getJTF(payload);
							final RemoteFile[] rf = new RemoteFile[payload.getInt()];
							for (int i = 0; i < rf.length; i++) {
								rf[i] = new RemoteFile(BufferUtil.getJTF(payload), 0, payload.get() != 0);
								Log.l.fine("parentfolder=" + parentPath + " child=" + rf[i]);
							}
							view.addChildFiles(fs, parentPath, rf);
						break;

						case OP_FILE_ACTION:
							switch (payload.get() & 0xFF) {
								case FOP_GET_INFO:
									final String name = BufferUtil.getJTF(payload);
									final long size = payload.getLong();
									final boolean hasThumb = payload.get() != 0;
									Image thumb = null;
									if (hasThumb) {
										image = new byte[payload.remaining()];
										payload.get(image);
										try {
											thumb = ImageIO.read(new GZIPInputStream(new ByteArrayInputStream(image)));
										}
										catch (IOException e) {
											Log.l.log(Level.INFO, "", e);
										}
									}
									image = new byte[payload.capacity() - 1];
									view.setFile(new RemoteFile(name, size, false, thumb));
								break;
								case FOP_DOWNLOAD:
									final String fileName = BufferUtil.getJTF(payload);
									final byte[] fileBytes = new byte[payload.remaining()];
									payload.get(fileBytes);
									// TODO Looks like we aren't invoking a timeout, this could
									// be a server-side problem

									SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
											final JFileChooser chooser = new JFileChooser();
											chooser.setDialogTitle("Saving '" + fileName + "'");
											chooser.setSelectedFile(new File(fileName));
											final int returnVal = chooser.showSaveDialog(null);
											FileOutputStream fos = null;
											if (returnVal == JFileChooser.APPROVE_OPTION) {
												final File file = chooser.getSelectedFile();
												try {
													file.createNewFile();
													fos = new FileOutputStream(file);
													fos.write(fileBytes);
												}
												catch (IOException e) {
													Log.l.log(Level.INFO, "Unable to save file=" + file.getName(), e);
												}
												finally {
													if (fos != null) {
														try {
															fos.close();
														}
														catch (IOException e) {
														}
													}
												}
											}
										}
									});

								break;
							}
						break;
						case OP_REMOTE_SHELL:
							// Send to the view
							view.displayOutput(BufferUtil.getJTF(payload));
						break;
						default:
							Log.l.warning("Unhandled packet=" + id + " payload=" + payload.capacity() + " local=" + Slave.VERSION + " remote=" + getVersion());
						break;
					}
				}

				@Override
				public void handle(final int id) {
					switch (id) {
						default:
							Log.l.warning("Unhandled packet=" + id + " payload=none local=" + Slave.VERSION + " remote=" + getVersion());
						break;
					}
				}

				@Override
				protected void disconnected() {
					view.disconnected(RemoteSlave.this);
				}
			};
		}
		else {
			return comm;
		}
	}

	public void connect(final int intent) {
		try {
			final IoCommunicable ioc = connectToRemote();
			final ExpandableByteBuffer buf = new ExpandableByteBuffer();
			buf.put(intent);
			if (intent == INTENT_CONNECT) {
				final Dimension d = view.getThumbSize();
				buf.putShort((short) 200);// d.width);// TODO !
				buf.putShort((short) 160);// d.height);
			}
			ioc.send(OP_HANDSHAKE, buf.asByteBuffer());
		}
		catch (final IOException e) {
			setOnline(false);
			view.disconnected(this);
		}
	}

	@Override
	public void requestFileAction(final int action, final String path) throws IOException {
		final IoCommunicable ioc = connectToRemote();
		final ExpandableByteBuffer buf = new ExpandableByteBuffer();
		buf.put(action);
		buf.putJTF(path);
		ioc.send(OP_FILE_ACTION, buf.asByteBuffer());
	}

	@Override
	public void requestChildFiles(final String parentPath) throws IOException {
		final IoCommunicable ioc = connectToRemote();
		final ExpandableByteBuffer buf = new ExpandableByteBuffer();
		buf.putJTF(parentPath);
		ioc.send(OP_GET_FILES, buf.asByteBuffer());
	}

	public void executeRemoteCommand(final String command) throws IOException {
		final IoCommunicable ioc = connectToRemote();
		final ExpandableByteBuffer buf = new ExpandableByteBuffer();
		buf.putJTF(command);
		ioc.send(OP_DO_COMMAND, buf.asByteBuffer());
	}

	@Override
	public void setView(final SlaveView view) {
		this.view = view;
	}

	public SlaveView getView() {
		return view;
	}

	public Communicable getCommunicable() throws IOException {
		return connectToRemote();
	}

	@Override
	public void imageProduced(final Image image) {
		getView().imageProduced(image);
	}

	@Override
	public void imageResized(final int width, final int height) {
		getView().imageResized(width, height);
	}

	@Override
	public String toString() {
		return getHost();
	}
}