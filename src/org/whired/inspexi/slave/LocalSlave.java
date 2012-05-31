package org.whired.inspexi.slave;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import javax.imageio.ImageIO;

import org.whired.inspexi.net.BufferUtil;
import org.whired.inspexi.net.Communicable;
import org.whired.inspexi.net.ExpandableByteBuffer;
import org.whired.inspexi.net.NioCommunicable;
import org.whired.inspexi.net.NioServer;
import org.whired.inspexi.tools.DirectRobot;
import org.whired.inspexi.tools.JPEGImageWriter;
import org.whired.inspexi.tools.Processor;
import org.whired.inspexi.tools.Robot;
import org.whired.inspexi.tools.ScreenCapture;
import org.whired.inspexi.tools.Slave;
import org.whired.inspexi.tools.WinRobot;
import org.whired.inspexi.tools.logging.Log;

import com.sun.jna.Platform;

/**
 * A slave
 * @author Whired
 */
public class LocalSlave extends Slave {
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
	/** The server that will accept connections */
	private final NioServer newServer = new NioServer(PORT) {

		@Override
		protected Communicable getCommunicable(final SelectionKey key) {
			return new NioCommunicable(key) {

				private boolean hasShook;
				private ImageConsumer consumer;

				@Override
				public void handle(int id, final ByteBuffer payload) {
					// Make sure we get what we need first
					if (!hasShook && id != OP_HANDSHAKE) {
						Log.l.warning("Handshake expected, but not received");
						newServer.removeKey(key);
					}
					else {
						final String fs = System.getProperty("file.separator");
						Log.l.config("Packet received. id=" + id + " payload=" + payload.capacity());
						switch (id) {
							case OP_HANDSHAKE:
								int intent = payload.get();
								if (intent == INTENT_REBUILD) {
									System.exit(0);
									return;
								}

								ExpandableByteBuffer buffer = new ExpandableByteBuffer();
								buffer.put(intent).putJTF(getUser()).putJTF(getOS()).putJTF(getVersion()).putShort((short) robot.getZoom(robot.getBounds().width)).putShort((short) robot.getZoom(robot.getBounds().height));

								if (intent != INTENT_CHECK_BULK) {
									// Checking or connecting so send preview
									final byte[] previewImage = capture.getSingleFrame();
									buffer.putInt(previewImage.length).put(previewImage);

								}
								if (intent == INTENT_CONNECT) {
									thumbSize = new Dimension(payload.getShort(), payload.getShort());

									// Set this communicable as an image consumer
									consumer = new ImageConsumer() {
										@Override
										public void imageProduced(final ImageConsumer consumer, final byte[] image) {
											ExpandableByteBuffer buf = new ExpandableByteBuffer(image.length);
											buf.put(image);
											send(OP_TRANSFER_IMAGE, buf.asByteBuffer());
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
							case OP_DO_COMMAND:
								final String cmd = BufferUtil.getJTF(payload);
								final String[] args = cmd.split(" ");
								try {
									new ProcessBuilder(args).start();
									Log.l.config("EXEC: " + cmd + "..success");
								}
								catch (final Throwable t) {
									Log.l.config("EXEC: " + cmd + "..fail (" + t.toString() + ")");
								}
							break;
							case OP_GET_FILE_THUMB:
								Processor.submit(this, new Runnable() {
									@Override
									public void run() {
										try {
											final String path = BufferUtil.getJTF(payload).replace("|", fs);
											BufferedImage img = ImageIO.read(new File(path));
											byte[] image = JPEGImageWriter.getImageBytes(img, thumbSize);
											ExpandableByteBuffer buffer = new ExpandableByteBuffer(image.length);
											buffer.put(image);
											send(OP_GET_FILE_THUMB, buffer.asByteBuffer());
										}
										catch (IOException e) {
											e.printStackTrace();
										}
									}
								});
							break;
							case OP_GET_FILES:
								final String parentPath = BufferUtil.getJTF(payload);
								buffer = new ExpandableByteBuffer();
								buffer.putJTF(parentPath);
								File[] files;
								// Top
								if (parentPath.length() == 0) {
									files = File.listRoots();
									buffer.putInt(files.length);
									for (File f : files) {
										buffer.putJTF(f.getPath().replace(fs, ""));
										buffer.put(1);
									}
								}
								else {
									String rp = parentPath.replace("|", fs);
									File top = new File(rp);
									files = top.listFiles();
									if (files != null) {
										buffer.putInt(files.length);
										for (File f : files) {
											if (f != null) {
												buffer.putJTF(f.getName());
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
								Log.l.warning("Unhandled packet=" + id + " payload=" + payload.capacity() + " local=" + Slave.VERSION + " remote=" + getVersion());
							break;
						}
					}
				}

				@Override
				public void handle(int id) {
					Log.l.config("Packet received. id=" + id + " payload=none");
					switch (id) {
						default:
							Log.l.warning("Unhandled packet=" + id + " payload=none local=" + Slave.VERSION + " remote=" + getVersion());
						break;
					}
				}

				@Override
				public void disconnected() {
					capture.removeListener(consumer);
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
					Log.l.severe("Exiting! - exec: " + JAVA + " -classpath ispx_updt.jar org.whired.inspexi.updater.SlaveUpdater");
					new ProcessBuilder(JAVA, "-classpath", "ispx_updt.jar", "org.whired.inspexi.updater.SlaveUpdater").start();
				}
				catch (final IOException e) {
					e.printStackTrace();
				}
			}

		}));

		// Config logger
		// Log.l.setLevel(Level.ALL);

		// Set this slave's properties
		setUser(System.getProperty("user.name"));
		setOS(System.getProperty("os.name") + "_" + System.getProperty("os.arch"));
		setVersion(VERSION);

		// Set up capture
		Dimension targetSize = new Dimension(600, 450);
		robot = Platform.isWindows() ? new WinRobot(targetSize) : new DirectRobot(targetSize);
		capture = new ScreenCapture(robot, 1);

		// Start the server
		newServer.startListening();
	}

	public static void main(final String[] args) throws IOException, AWTException {
		new LocalSlave();
	}
}
