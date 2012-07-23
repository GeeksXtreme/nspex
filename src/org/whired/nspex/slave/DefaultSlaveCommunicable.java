package org.whired.nspex.slave;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.whired.nspex.net.BufferUtil;
import org.whired.nspex.net.ExpandableByteBuffer;
import org.whired.nspex.net.NioCommunicable;
import org.whired.nspex.tools.JPEGImageWriter;
import org.whired.nspex.tools.Processor;
import org.whired.nspex.tools.Slave;
import org.whired.nspex.tools.logging.Log;

/**
 * The default slave-side implementation of {@link NioCommunicable}
 * @author Whired
 */
public class DefaultSlaveCommunicable extends NioCommunicable {
	/** The slave for this communicable */
	private final LocalSlave slave;
	/** Whether or not a protocol handshake has been performed */
	private boolean hasShook;

	private ImageConsumer consumer;
	private Shell shell;
	/** The file separator for this host */
	private static char FS = System.getProperty("file.separator").charAt(0);

	public DefaultSlaveCommunicable(SelectionKey key, LocalSlave slave) {
		super(key, slave);
		this.slave = slave;
	}

	@Override
	public void handle(final int id, final ByteBuffer payload) {
		// Make sure we get what we need first
		if (!hasShook && id != Slave.OP_HANDSHAKE) {
			Log.l.warning("[" + this + "] Handshake expected, but not received");
			disconnect();
		}
		else {
			switch (id) {
				case Slave.OP_HANDSHAKE:
					final int intent = payload.get();
					if (intent == Slave.INTENT_REBUILD) {
						System.exit(0);
						return;
					}

					ExpandableByteBuffer buffer = new ExpandableByteBuffer();
					buffer.put(intent).putJTF(slave.getUser()).putJTF(slave.getOS()).putJTF(slave.getVersion()).putShort((short) slave.robot.scale(slave.robot.getCaptureBounds().width)).putShort((short) slave.robot.scale(slave.robot.getCaptureBounds().height));

					if (intent != Slave.INTENT_CHECK_BULK && intent != Slave.INTENT_CONNECT) {
						// Checking or connecting so send preview
						final byte[] previewImage = slave.capture.getSingleFrame();
						buffer.putInt(previewImage.length).put(previewImage);
					}
					if (intent == Slave.INTENT_CONNECT) {
						slave.thumbSize = new Dimension(payload.getShort(), payload.getShort());
						// Set this communicable as an image consumer
						consumer = new ImageConsumer() {
							@Override
							public void imageProduced(final byte[] image) {
								final ByteBuffer buf = ByteBuffer.allocate(image.length);
								buf.put(image);
								send(Slave.OP_TRANSFER_IMAGE, buf);
							}

							@Override
							public int hashCode() {
								return DefaultSlaveCommunicable.this.hashCode();
							}

							@Override
							public boolean equals(Object obj) {
								return obj instanceof ImageConsumer && ((ImageConsumer) obj).hashCode() == this.hashCode();
							}
						};
						slave.capture.addListener(consumer);
					}
					else {
						// They got their info but they aren't sticking around much longer
						setReadTimeout(2500);
					}
					send(Slave.OP_HANDSHAKE, buffer.asByteBuffer());
					hasShook = true;
				break;
				case Slave.OP_DO_COMMAND: // TODO send output

					// The process and streams will be attached to the NioComm

					// This will be written (and flushed) to out
					final String cmd = BufferUtil.getJTF(payload);

					// If there isn't currently a shell, make one using the string received
					if (shell != null) {
						Log.l.finest("Exec=" + cmd);
						shell.executeCommand(cmd);
					}
					else {
						try {
							shell = new Shell(cmd) {
								@Override
								protected void outputReceived(String output) {
									send(Slave.OP_REMOTE_SHELL, new ExpandableByteBuffer().putJTF(output).asByteBuffer());
								}

								@Override
								protected void closed() {
									shell = null;
								}
							};
						}
						catch (IOException e) {
							// Bad program name
							send(Slave.OP_REMOTE_SHELL, new ExpandableByteBuffer().putJTF(e.toString() + "\r\n").asByteBuffer());
						}
					}
				break;
				case Slave.OP_FILE_ACTION:
					final int fop = payload.get() & 0xFF;
					final String path = BufferUtil.getJTF(payload);
					handleFileOperation(fop, path);
				break;
				case Slave.OP_GET_FILES:
					final String parentPath = BufferUtil.getJTF(payload);
					Log.l.fine("[" + this + "] parent=" + parentPath);
					buffer = new ExpandableByteBuffer();
					buffer.putChar(FS);
					buffer.putJTF(parentPath);
					File[] files;
					// parentPath is root
					if (parentPath.length() == 0) {
						files = File.listRoots();
						buffer.putInt(files.length);
						String rname;
						for (final File f : files) {
							rname = f.getPath();
							Log.l.fine("[" + this + "] rootfile=" + rname);
							buffer.putJTF(rname);
							buffer.put(1);
						}
					}
					else {
						final File top = new File(parentPath);
						files = top.listFiles();
						if (files != null) {
							buffer.putInt(files.length);
							for (final File f : files) {
								if (f != null) {
									buffer.putJTF(f.getName());
									Log.l.fine("[" + this + "] child=" + f.getName());
									buffer.put(f.isDirectory() ? 1 : 0);
								}
							}
						}
						else {
							buffer.putInt(0);
						}
					}

					send(Slave.OP_GET_FILES, buffer.asByteBuffer());

				break;

				default:
					Log.l.warning("[" + this + "] Unhandled packet=" + id + " payload=" + payload.capacity() + " local=" + Slave.VERSION + " remote=" + slave.getVersion());
				break;
			}
		}
	}

	public void handleFileOperation(final int fop, final String path) {
		switch (fop) {
			case Slave.FOP_GET_INFO:
				Processor.submit(this, new Runnable() {
					@Override
					public void run() {
						Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
						Log.l.config("[" + this + "] Info requested=" + path);

						// Set up the response
						final ExpandableByteBuffer buffer = new ExpandableByteBuffer();
						buffer.put(Slave.FOP_GET_INFO);

						// Load up the file & info
						File f = new File(path);
						final String name = f.getName();
						final long size = f.length();
						final String uname = name.toLowerCase();

						// Drop it in
						buffer.putJTF(name);
						buffer.putLong(size);

						if (uname.endsWith("jpeg") || uname.endsWith("jpg") || uname.endsWith("bmp") || uname.endsWith("png") || uname.endsWith("gif")) {
							byte[] image = null;
							try {
								final BufferedImage img = ImageIO.read(new File(path));
								image = JPEGImageWriter.getImageBytes(img, slave.thumbSize);
							}
							catch (final Throwable e) {
								// Seems to only happen when the extension is wrong
								Log.l.log(Level.CONFIG, "Failed to get thumb:", e);
							}
							if (image != null) {
								buffer.put(1);
								buffer.put(image);
							}

						}
						else {
							buffer.put(0);
						}
						send(Slave.OP_FILE_ACTION, buffer.asByteBuffer());
					}
				});
			break;
			case Slave.FOP_DELETE:
				boolean deleted = new File(path).delete();
				Log.l.info(this + " deleted " + path + "?=" + deleted);
				if (deleted) {
					remoteLog(Level.INFO, path + " successfully deleted");
				}
			break;
			case Slave.FOP_RENAME:
			// TODO Oops, this can't work here -- needs a target
			break;
			case Slave.FOP_DOWNLOAD:
				Log.l.config(this + " requesting download=" + path);
				// TODO will this hold up other people?

				// Looks like we're going to enforce a size limit
				final int maxSize = 10240000; // 10MB
				File toDownload = new File(path);
				if (toDownload.exists()) {
					long size = toDownload.length();
					if (size <= maxSize) {
						byte[] pathBytes = BufferUtil.encodeJTF(toDownload.getName());
						ByteBuffer retBuf = ByteBuffer.allocateDirect((int) size + 3 + pathBytes.length);
						retBuf.put((byte) Slave.FOP_DOWNLOAD).putShort((short) pathBytes.length).put(pathBytes);
						FileInputStream fis = null;
						try {
							long start = System.currentTimeMillis();
							fis = new FileInputStream(toDownload);
							FileChannel fc = fis.getChannel();
							while (retBuf.hasRemaining()) {
								if (fc.read(retBuf) == -1) {
									throw new EOFException();
								}
							}
							send(Slave.OP_FILE_ACTION, retBuf);
							Log.l.info("File send time=" + (System.currentTimeMillis() - start));
						}
						catch (IOException e) {
							Log.l.log(Level.WARNING, "Unable to download file: ", e);
						}
						finally {
							if (fis != null) {
								try {
									fis.close();
								}
								catch (IOException e) {
								}
							}
						}
					}
					else {
						remoteLog(Level.WARNING, "File size is too large");
					}
				}
			break;
		}
	}

	@Override
	public void handle(final int id) {
		switch (id) {
			default:
				Log.l.warning("[" + this + "] Unhandled packet=" + id + " payload=none local=" + Slave.VERSION + " remote=" + slave.getVersion());
			break;
		}
	}

	@Override
	public void disconnected() {
		if (shell != null) {
			shell.close();
		}
		slave.capture.removeListener(consumer);
	}

	@Override
	public void remoteLog(final Level level, final String message) {
		ExpandableByteBuffer buffer = new ExpandableByteBuffer();
		buffer.put((byte) level.intValue()).putJTF(message);
		send(Slave.OP_LOG, buffer.asByteBuffer());
	}

}
