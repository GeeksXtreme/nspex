package org.whired.inspexi.master;

import java.awt.Dimension;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.whired.inspexi.net.BufferUtil;
import org.whired.inspexi.net.Communicable;
import org.whired.inspexi.net.ExpandableByteBuffer;
import org.whired.inspexi.net.IoCommunicable;
import org.whired.inspexi.tools.RemoteFile;
import org.whired.inspexi.tools.Slave;
import org.whired.inspexi.tools.SlaveModel;
import org.whired.inspexi.tools.SlaveView;

public abstract class RemoteSlave extends Slave implements SlaveModel {
	private final Socket socket = new Socket();
	private SlaveView view;
	private final IoCommunicable comm;

	public RemoteSlave(final String ip, final int port) throws IOException {
		super(ip);
		try {
			socket.connect(new InetSocketAddress(ip, port), 1500);
			setOnline(true);
			comm = new IoCommunicable(socket) {

				@Override
				public void handle(int id, ByteBuffer payload) {
					switch (id) {
						case OP_HANDSHAKE:
							int intent = payload.get();
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
								catch (IOException e) {
									e.printStackTrace();
								}
							}
							onPropertyChange();
						break;
						case OP_TRANSFER_IMAGE:
							byte[] image = new byte[payload.capacity()];
							payload.get(image);
							try {
								imageProduced(ImageIO.read(new ByteArrayInputStream(image)));
							}
							catch (IOException e) {
								e.printStackTrace();
							}
						break;
						case OP_GET_FILES:
							String parentPath = BufferUtil.getJTF(payload);
							RemoteFile[] rf = new RemoteFile[payload.getInt()];
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
							catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						break;
						default:
							System.out.println("Unhandled packet=" + id + " len=" + payload.capacity());
						break;
					}
				}

				@Override
				public void handle(int id) {
					// TODO Auto-generated method stub
				}

				@Override
				protected void disconnected() {
					setOnline(false);
					view.disconnected();
					onPropertyChange();
				}
			};
		}
		catch (IOException e) {
			setOnline(false);
			onPropertyChange();
			throw (e);
		}
	}

	protected abstract void onPropertyChange();

	public void connect(final int intent) throws IOException {
		ExpandableByteBuffer buf = new ExpandableByteBuffer();
		buf.put(intent);

		if (intent == INTENT_CONNECT) {
			Dimension d = view.getThumbSize();
			buf.putShort((short) 200);// d.width);// TODO
			buf.putShort((short) 160);// d.height);
		}
		comm.send(OP_HANDSHAKE, buf.asByteBuffer());
	}

	@Override
	public void requestThumbnail(final String path) {
		ExpandableByteBuffer buf = new ExpandableByteBuffer();
		buf.putJTF(path);
		comm.send(OP_GET_FILE_THUMB, buf.asByteBuffer());
	}

	@Override
	public void requestChildFiles(final String parentPath) {
		ExpandableByteBuffer buf = new ExpandableByteBuffer();
		buf.putJTF(parentPath);
		comm.send(OP_GET_FILES, buf.asByteBuffer());
	}

	public void executeRemoteCommand(final String command) {
		ExpandableByteBuffer buf = new ExpandableByteBuffer();
		buf.putJTF(command);
		comm.send(OP_DO_COMMAND, buf.asByteBuffer());
	}

	@Override
	public void setView(final SlaveView view) {
		this.view = view;
	}

	public SlaveView getView() {
		return view;
	}

	public Communicable getCommunicable() {
		return this.comm;
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
		return getIp();
	}
}