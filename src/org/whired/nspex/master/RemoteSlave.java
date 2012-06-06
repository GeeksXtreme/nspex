package org.whired.nspex.master;

import java.awt.Dimension;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.whired.nspex.net.BufferUtil;
import org.whired.nspex.net.Communicable;
import org.whired.nspex.net.ExpandableByteBuffer;
import org.whired.nspex.net.IoCommunicable;
import org.whired.nspex.tools.RemoteFile;
import org.whired.nspex.tools.Slave;
import org.whired.nspex.tools.SlaveModel;
import org.whired.nspex.tools.SlaveView;
import org.whired.nspex.tools.logging.Log;

public class RemoteSlave extends Slave implements SlaveModel {
	private Socket socket = new Socket();
	private SlaveView view;
	private IoCommunicable comm;
	private final InetSocketAddress endpoint;

	public RemoteSlave(final String ip) {
		super(ip);
		endpoint = new InetSocketAddress(ip, Slave.PORT);
	}

	/**
	 * Connects to {@link #endpoint} if not already connected. If already connected, this method returns immediately.
	 * @throws IOException if a connection cannot be established
	 */
	private IoCommunicable connectToRemote() throws IOException {
		if (!socket.isConnected() || socket.isClosed()) {
			socket = new Socket();
			socket.connect(endpoint, 1500);
			return comm = new IoCommunicable(socket) {

				@Override
				public void handle(final int id, final ByteBuffer payload) {
					Log.l.config("Packet received. id=" + id + " payload=" + payload.capacity());
					switch (id) {
						case OP_HANDSHAKE:
							final int intent = payload.get();
							setUser(BufferUtil.getJTF(payload));
							setOS(BufferUtil.getJTF(payload));
							setVersion(BufferUtil.getJTF(payload));
							imageResized(payload.getShort(), payload.getShort());

							if (intent != INTENT_CHECK_BULK) {
								// Read preview if this isn't a bulk check
								final int imgLen = payload.getInt();
								final byte[] buf = new byte[imgLen];
								payload.get(buf);
								try {
									imageProduced(ImageIO.read(new ByteArrayInputStream(buf)));
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
								imageProduced(ImageIO.read(new ByteArrayInputStream(image)));
							}
							catch (final IOException e) {
								e.printStackTrace();
							}
						break;
						case OP_GET_FILES:
							final String parentPath = BufferUtil.getJTF(payload);
							final RemoteFile[] rf = new RemoteFile[payload.getInt()];
							for (int i = 0; i < rf.length; i++) {
								rf[i] = new RemoteFile(BufferUtil.getJTF(payload), payload.get() != 0);
							}
							view.addChildFiles(parentPath, rf);
						break;
						case OP_GET_FILE_THUMB:
							image = new byte[payload.capacity()];
							payload.get(image);
							try {
								view.setThumbnail(ImageIO.read(new ByteArrayInputStream(image)));
							}
							catch (final IOException e) {
								e.printStackTrace();
							}
						break;
						default:
							Log.l.warning("Unhandled packet=" + id + " payload=" + payload.capacity() + " local=" + Slave.VERSION + " remote=" + getVersion());
						break;
					}
				}

				@Override
				public void handle(final int id) {
					Log.l.config("Packet received. id=" + id + " payload=none");
					switch (id) {
						default:
							Log.l.warning("Unhandled packet=" + id + " payload=none local=" + Slave.VERSION + " remote=" + getVersion());
						break;
					}
				}

				@Override
				protected void disconnected() {
					//setOnline(false);
					view.disconnected(RemoteSlave.this);
				}
			};
		}
		else {
			return comm;
		}
	}

	public void connect(final int intent) throws IOException {
		try {
			final IoCommunicable ioc = connectToRemote();
			final ExpandableByteBuffer buf = new ExpandableByteBuffer();
			buf.put(intent);
			if (intent == INTENT_CONNECT) {
				final Dimension d = view.getThumbSize();
				buf.putShort((short) 200);// d.width);// TODO
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
	public void requestThumbnail(final String path) throws IOException {
		final IoCommunicable ioc = connectToRemote();
		final ExpandableByteBuffer buf = new ExpandableByteBuffer();
		buf.putJTF(path);
		ioc.send(OP_GET_FILE_THUMB, buf.asByteBuffer());
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