package org.whired.nspex.slave;

import java.awt.AWTException;
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
import org.whired.nspex.net.Communicable;
import org.whired.nspex.net.ExpandableByteBuffer;
import org.whired.nspex.net.NioCommunicable;
import org.whired.nspex.net.NioServer;
import org.whired.nspex.tools.AWTRobot;
import org.whired.nspex.tools.JPEGImageWriter;
import org.whired.nspex.tools.Processor;
import org.whired.nspex.tools.Robot;
import org.whired.nspex.tools.ScreenCapture;
import org.whired.nspex.tools.Slave;
import org.whired.nspex.tools.WinRobot;
import org.whired.nspex.tools.logging.Log;

import com.sun.jna.Platform;

/**
 * A slave
 * @author Whired
 */
public class LocalSlave implements Slave {
	/** Basic information for this slave */
	private String os, user, host, version;
	private boolean online;
	/** The port the server will listen on */
	private static final int PORT = 43596;
	/** The file separator for this host */
	private static String FS = System.getProperty("file.separator");
	/** The location of the jre executable for this host */
	private static final String JAVA = System.getProperty("java.home") + FS + "bin" + FS + "java";
	/** The robot that handles screen captures */
	private final Robot robot;
	/** The screen capture that will record the screen */
	private final ScreenCapture capture;
	// TODO mv..
	private Dimension thumbSize;
	/** The file separator char for this machine */
	private final char fs = System.getProperty("file.separator").charAt(0);
	/** The server that will accept connections */
	private final NioServer newServer = new NioServer(PORT) {

		@Override
		protected Communicable getCommunicable(final SelectionKey key) {
			return new NioCommunicable(key, newServer) {
				private final NioCommunicable localComm = this;
				private boolean hasShook;
				private ImageConsumer consumer;
				private Shell shell;

				@Override
				public void handle(final int id, final ByteBuffer payload) {
					// Make sure we get what we need first
					if (!hasShook && id != OP_HANDSHAKE) {
						Log.l.warning("[" + this + "] Handshake expected, but not received");
						disconnect();
					}
					else {
						switch (id) {
							case OP_HANDSHAKE:
								final int intent = payload.get();
								if (intent == INTENT_REBUILD) {
									System.exit(0);
									return;
								}

								ExpandableByteBuffer buffer = new ExpandableByteBuffer();
								buffer.put(intent).putJTF(getUser()).putJTF(getOS()).putJTF(getVersion()).putShort((short) robot.scale(robot.getCaptureBounds().width)).putShort((short) robot.scale(robot.getCaptureBounds().height));

								if (intent != INTENT_CHECK_BULK && intent != INTENT_CONNECT) {
									// Checking or connecting so send preview
									final byte[] previewImage = capture.getSingleFrame();
									buffer.putInt(previewImage.length).put(previewImage);
								}
								if (intent == INTENT_CONNECT) {
									thumbSize = new Dimension(payload.getShort(), payload.getShort());
									// Set this communicable as an image consumer
									consumer = new ImageConsumer() {
										@Override
										public void imageProduced(final byte[] image) {
											final ByteBuffer buf = ByteBuffer.allocate(image.length);
											buf.put(image);
											send(OP_TRANSFER_IMAGE, buf);
										}

										@Override
										public int hashCode() {
											return localComm.hashCode();
										}

										@Override
										public boolean equals(Object obj) {
											return obj instanceof ImageConsumer && ((ImageConsumer) obj).hashCode() == this.hashCode();
										}
									};
									capture.addListener(consumer);
								}
								else {
									// They got their info but they aren't sticking around much longer
									setReadTimeout(2500);
								}
								send(OP_HANDSHAKE, buffer.asByteBuffer());
								hasShook = true;
							break;
							case OP_DO_COMMAND: // TODO send output

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
												send(OP_REMOTE_SHELL, new ExpandableByteBuffer().putJTF(output).asByteBuffer());
											}

											@Override
											protected void closed() {
												shell = null;
											}
										};
									}
									catch (IOException e) {
										// Bad program name
										send(OP_REMOTE_SHELL, new ExpandableByteBuffer().putJTF(e.toString() + "\r\n").asByteBuffer());
									}
								}
							break;
							case OP_FILE_ACTION:
								final int fop = payload.get() & 0xFF;
								final String path = BufferUtil.getJTF(payload);
								handleFileOperation(fop, path);
							break;
							case OP_GET_FILES:
								final String parentPath = BufferUtil.getJTF(payload);
								Log.l.fine("[" + this + "] parent=" + parentPath);
								buffer = new ExpandableByteBuffer();
								buffer.putChar(fs);
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

								send(OP_GET_FILES, buffer.asByteBuffer());

							break;

							default:
								Log.l.warning("[" + this + "] Unhandled packet=" + id + " payload=" + payload.capacity() + " local=" + Slave.VERSION + " remote=" + getVersion());
							break;
						}
					}
				}

				public void handleFileOperation(final int fop, final String path) {
					switch (fop) {
						case FOP_GET_THUMB:
							Processor.submit(this, new Runnable() {
								@Override
								public void run() {
									try {
										Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
										Log.l.config("[" + this + "] Thumb requested=" + path);
										final BufferedImage img = ImageIO.read(new File(path));
										final byte[] image = JPEGImageWriter.getImageBytes(img, thumbSize);
										final ExpandableByteBuffer buffer = new ExpandableByteBuffer(image.length + 1);
										buffer.put(FOP_GET_THUMB);
										buffer.put(image);
										send(OP_FILE_ACTION, buffer.asByteBuffer());
									}
									catch (final Throwable e) {
										// Seems to only happen when the extension is wrong
										Log.l.log(Level.CONFIG, "Failed to get thumb:", e);
									}
								}
							});
						break;
						case FOP_DELETE:
							boolean deleted = new File(path).delete();
							Log.l.info(localComm + " deleted " + path + "?=" + deleted);
							if (deleted) {
								remoteLog(Level.INFO, path + " successfully deleted");
							}
						break;
						case FOP_RENAME:
						// TODO Oops, this can't work here -- needs a target
						break;
						case FOP_DOWNLOAD:
							Log.l.config(localComm + " requesting download=" + path);
							// TODO will this hold up other people?

							// Looks like we're going to enforce a size limit
							final int maxSize = 10240000; // 10MB
							File toDownload = new File(path);
							if (toDownload.exists()) {
								long size = toDownload.length();
								if (size <= maxSize) {
									byte[] pathBytes = BufferUtil.encodeJTF(toDownload.getName());
									ByteBuffer retBuf = ByteBuffer.allocateDirect((int) size + 3 + pathBytes.length);
									retBuf.put((byte) FOP_DOWNLOAD).putShort((short) pathBytes.length).put(pathBytes);
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
										send(OP_FILE_ACTION, retBuf);
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
							Log.l.warning("[" + this + "] Unhandled packet=" + id + " payload=none local=" + Slave.VERSION + " remote=" + getVersion());
						break;
					}
				}

				@Override
				public void disconnected() {
					if (shell != null) {
						shell.close();
					}
					capture.removeListener(consumer);
				}

				@Override
				public void remoteLog(final Level level, final String message) {
					ExpandableByteBuffer buffer = new ExpandableByteBuffer();
					buffer.put((byte) level.intValue()).putJTF(message);
					send(OP_LOG, buffer.asByteBuffer());
				}
			};
		}

	};

	/**
	 * Creates a new local slave with a default server
	 * @throws IOException
	 * @throws AWTException
	 */
	public LocalSlave() throws IOException, AWTException {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Log.l.severe("Exiting! - exec: " + JAVA + " -classpath ispx_updt.jar org.whired.nspex.updater.SlaveUpdater");
					new ProcessBuilder(JAVA, "-classpath", "ispx_updt.jar", "org.whired.nspex.updater.SlaveUpdater").start();
				}
				catch (final IOException e) {
					e.printStackTrace();
				}
			}

		}));

		// Config logger
		Log.l.setLevel(Level.ALL);

		// Set this slave's properties
		setUser(System.getProperty("user.name"));
		setOS(System.getProperty("os.name") + "_" + System.getProperty("os.arch"));
		setVersion(VERSION);

		// Set up capture
		final Dimension targetSize = new Dimension(600, 450); // TODO send val and hold for each comm - resizing will have to be done for each comm
		robot = Platform.isWindows() ? new WinRobot(.8F) : new AWTRobot(.8F);
		capture = new ScreenCapture(robot, 1);

		// Start the server
		newServer.startListening();
	}

	public static void main(final String[] args) throws IOException, AWTException {
		new LocalSlave();
	}

	@Override
	public String getOS() {
		return os;
	}

	@Override
	public void setOS(String os) {
		this.os = os;
	}

	@Override
	public String getUser() {
		return user;
	}

	@Override
	public void setUser(String user) {
		this.user = user;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public boolean isOnline() {
		return online;
	}

	@Override
	public void setOnline(boolean online) {
		this.online = online;
	}
}
