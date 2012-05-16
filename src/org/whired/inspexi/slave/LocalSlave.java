package org.whired.inspexi.slave;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.imageio.ImageIO;

import org.whired.inspexi.tools.DirectRobot;
import org.whired.inspexi.tools.JPEGImageWriter;
import org.whired.inspexi.tools.NetTask;
import org.whired.inspexi.tools.NetTaskQueue;
import org.whired.inspexi.tools.ReactServer;
import org.whired.inspexi.tools.Robot;
import org.whired.inspexi.tools.ScreenCapture;
import org.whired.inspexi.tools.SessionListener;
import org.whired.inspexi.tools.Slave;
import org.whired.inspexi.tools.WinRobot;

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
	/** The listener that is notified when a session ends */
	private final SessionListener sessl = new SessionListener() {
		@Override
		public void sessionEnded(final String reason, final Throwable t) {
			System.out.println("Session ended: " + reason);
		}
	};
	/** The server that will accept connections */
	private final ReactServer server = new ReactServer(new ServerSocket(PORT)) {
		@Override
		public NetTask getOnConnectTask(final Socket sock) {
			return new NetTask(sessl, sock) {
				@Override
				public void run(final DataInputStream dis, final DataOutputStream dos) throws IOException {
					// Read intent before doing anything
					// Beware of timeout, malicious clients could hold us open
					System.out.println("Exec_task_handshake");
					final int intent = dis.read();
					if (intent == -1) {
						throw new IOException("End of stream");
					}

					if (intent == INTENT_REBUILD) {
						System.exit(0);
					}
					else if (intent == INTENT_CHECK || intent == INTENT_CHECK_BULK || intent == INTENT_CONNECT) {
						dos.writeUTF(getUser());
						dos.writeUTF(getOS());
						dos.writeUTF(getVersion());
						dos.writeShort(robot.getZoom(robot.getBounds().width));
						dos.writeShort(robot.getZoom(robot.getBounds().height));
						if (intent == INTENT_CHECK) {
							// Send preview
							final byte[] previewImage = capture.getSingleFrame();
							dos.writeInt(previewImage.length);
							dos.write(previewImage);
						}
						else {
							NetTaskQueue.add(new NetTask(sessl, sock) {
								@Override
								public void run(final DataInputStream dis, final DataOutputStream dos) throws IOException {
									System.out.println("Exec_task_read_opcode");
									String fs = System.getProperty("file.separator");
									int op;
									sock.setSoTimeout(10);
									timeoutFatal = false;
									while ((op = dis.read()) != -1) {
										socket.setSoTimeout(25000);
										timeoutFatal = true;
										System.out.println("op: " + op);
										switch (op) {
											case INTENT_REBUILD:
												System.exit(0);
											break;
											case OP_DO_COMMAND:
												final String cmd = dis.readUTF();
												System.out.print("EXEC: " + cmd + "..");
												final String[] args = cmd.split(" ");
												try {
													new ProcessBuilder(args).start();
													System.out.println("success.");
												}
												catch (final Throwable t) {
													System.out.println("fail.");
												}
											break;
											case OP_GET_FILE_THUMB://TODO
												Image img = ImageIO.read(new File(dis.readUTF().replace("|", fs)));
												byte[] imgBytes = JPEGImageWriter.getImageBytes((RenderedImage) img);
												dos.write(OP_GET_FILE_THUMB);
												dos.writeInt(imgBytes.length);
												dos.write(imgBytes);
											break;
											case OP_GET_FILES:
												final String parentPath = dis.readUTF();
												dos.write(OP_GET_FILES);
												dos.writeUTF(parentPath);
												File[] files;
												// Top
												if (parentPath.length() == 0) {
													files = File.listRoots();
													dos.writeInt(files.length);
													for (File f : files) {
														dos.writeUTF(f.getPath().replace(fs, ""));
														dos.writeBoolean(true);
													}
												}
												else {
													String rp = parentPath.replace("|", fs);
													File top = new File(rp);
													files = top.listFiles();
													if (files != null) {
														dos.writeInt(files.length);
														for (File f : files) {
															if (f != null) {
																System.out.println("found: " + f.getName());
																dos.writeUTF(f.getName());
																dos.writeBoolean(f.isDirectory());
															}
														}
													}
													else {
														dos.writeInt(0);
													}
												}
											break;
											default:
												throw new IOException("Unhandled operation: " + op);
										}
										sock.setSoTimeout(10);
										timeoutFatal = false;
									}
									throw new IOException("End of stream");
								}
							});
							final ImageConsumer ic = new ImageConsumer() {
								@Override
								public void imageProduced(final ImageConsumer consumer, final byte[] image) {
									NetTaskQueue.add(new NetTask(sessl, sock) {
										@Override
										public void run(final DataInputStream dis, final DataOutputStream dos) throws IOException {
											System.out.println("Exec_task_transfer_image");
											dos.write(OP_TRANSFER_IMAGE);
											dos.writeInt(image.length);
											dos.write(image);
										}

										@Override
										public void onFail() {
											capture.removeListener(consumer);
										}
									});

								}
							};
							capture.addListener(ic);
						}
					}
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
					System.out.println("Exec: " + JAVA + " -classpath ispx_updt.jar org.whired.inspexi.updater.SlaveUpdater");
					new ProcessBuilder(JAVA, "-classpath", "ispx_updt.jar", "org.whired.inspexi.updater.SlaveUpdater").start();
				}
				catch (final IOException e) {
					e.printStackTrace();
				}
			}

		}));

		// Set this slave's properties
		setUser(System.getProperty("user.name"));
		setOS(System.getProperty("os.name") + "_" + System.getProperty("os.arch"));
		setVersion(VERSION);

		// Set up capture
		robot = Platform.isWindows() ? new WinRobot(.7D) : new DirectRobot(.7D);
		capture = new ScreenCapture(robot, 1);

		// Start the server
		System.out.print("Starting server..");
		server.startAccepting();
	}

	public static void main(final String[] args) throws IOException, AWTException {
		new LocalSlave();
	}
}
