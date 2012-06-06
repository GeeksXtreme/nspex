package org.whired.nspex.slave;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.whired.nspex.net.BufferUtil;
import org.whired.nspex.net.Communicable;
import org.whired.nspex.net.ExpandableByteBuffer;
import org.whired.nspex.net.NioCommunicable;
import org.whired.nspex.net.NioServer;
import org.whired.nspex.tools.DirectRobot;
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
			return new NioCommunicable(key, newServer) {

				private boolean hasShook;
				private ImageConsumer consumer;

				@Override
				public void handle(final int id, final ByteBuffer payload) {
					// Make sure we get what we need first
					if (!hasShook && id != OP_HANDSHAKE) {
						Log.l.warning("Handshake expected, but not received");
						disconnect();
					}
					else {
						final String fs = System.getProperty("file.separator");
						Log.l.config("Packet received. id=" + id + " payload=" + payload.capacity());
						switch (id) {
							case OP_HANDSHAKE:
								final int intent = payload.get();
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
										public void imageProduced(final byte[] image) {
											final ExpandableByteBuffer buf = new ExpandableByteBuffer(image.length);
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
							case OP_DO_COMMAND: // TODO send output
								final String cmd = BufferUtil.getJTF(payload);
								final String[] args = cmd.split(" ");
								try {
									final ProcessBuilder pb = new ProcessBuilder(args);
									pb.redirectErrorStream();
									final Process p = pb.start();
									final InputStreamReader in = new InputStreamReader(p.getInputStream());
									final StringBuilder sb = new StringBuilder();
									final Future<Integer> f = Executors.newSingleThreadExecutor().submit(new Callable<Integer>() {
										@Override
										public Integer call() throws Exception {
											return p.waitFor();
										}
									});
									try {
										f.get(500, TimeUnit.MILLISECONDS);
									}
									catch (final TimeoutException e) {
									}
									if (in.ready()) {
										final BufferedReader br = new BufferedReader(in);
										String line;
										Log.l.info("Waiting for output");
										while ((line = br.readLine()) != null) {
											sb.append(line + "\r\n");
										}
									}
									Log.l.info("EXEC: " + cmd + "..success: \r\n" + sb.toString());
								}
								catch (final Throwable t) {
									Log.l.info("EXEC: " + cmd + "..fail (" + t.toString() + ")");
								}
							break;
							case OP_GET_FILE_THUMB:
								Processor.submit(this, new Runnable() {
									@Override
									public void run() {
										try {
											final String path = BufferUtil.getJTF(payload).replace("|", fs);
											final BufferedImage img = ImageIO.read(new File(path));
											final byte[] image = JPEGImageWriter.getImageBytes(img, thumbSize);
											final ExpandableByteBuffer buffer = new ExpandableByteBuffer(image.length);
											buffer.put(image);
											send(OP_GET_FILE_THUMB, buffer.asByteBuffer());
										}
										catch (final IOException e) {
											e.printStackTrace();
										}
									}
								});
							break;
							case OP_GET_FILES:
								final String parentPath = BufferUtil.getJTF(payload);
								Log.l.fine(parentPath);
								buffer = new ExpandableByteBuffer();
								buffer.putJTF(parentPath);
								File[] files;
								// Top
								if (parentPath.length() == 0) {
									files = File.listRoots();
									buffer.putInt(files.length);
									for (final File f : files) {
										// Guess what, linux root is "/":
										Log.l.fine("rootfile=" + f.getPath());
										buffer.putJTF(f.getPath().replace(fs, ""));
										buffer.put(1);
									}
								}
								else {
									final String rp = parentPath.replace("|", fs);
									final File top = new File(rp);
									files = top.listFiles();
									if (files != null) {
										buffer.putInt(files.length);
										for (final File f : files) {
											if (f != null) {
												buffer.putJTF(f.getName());
												Log.l.fine(f.getName());
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
				public void handle(final int id) {
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
					Log.l.severe("Exiting! - exec: " + JAVA + " -classpath ispx_updt.jar org.whired.nspex.updater.SlaveUpdater");
					new ProcessBuilder(JAVA, "-classpath", "ispx_updt.jar", "org.whired.nspex.updater.SlaveUpdater").start();
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
		final Dimension targetSize = new Dimension(600, 450);
		robot = Platform.isWindows() ? new WinRobot(targetSize) : new DirectRobot(targetSize);
		capture = new ScreenCapture(robot, 1);

		// Start the server
		newServer.startListening();
	}

	public static void main(final String[] args) throws IOException, AWTException {
		Log.l.setLevel(Level.ALL);
		new LocalSlave();
	}
}
