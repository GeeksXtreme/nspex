package org.whired.inspexi.slave;

import java.awt.AWTException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.whired.inspexi.tools.DirectRobot;
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
	/** The queue that will accept net tasks */
	private final NetTaskQueue queue = new NetTaskQueue(sessl);
	/** The server that will accept connections */
	private final ReactServer server = new ReactServer(new ServerSocket(PORT), queue) {
		@Override
		public NetTask getOnConnectTask(final Socket sock) {
			return new NetTask("handshake_reactor", sock) {
				@Override
				public void run(final DataInputStream dis, final DataOutputStream dos) throws IOException {
					// Read intent before doing anything
					final int intent = dis.read();
					if (intent == -1) {
						throw new IOException("End of stream");
					}

					if (intent == INTENT_REBUILD) {
						System.exit(0);
					}
					else if (intent == INTENT_CHECK || intent == INTENT_CHECK_BULK || intent == INTENT_CONNECT) {
						dos.writeUTF(getHost());
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
							queue.add(new NetTask("handle_opcode", sock) {
								@Override
								public void run(final DataInputStream dis, final DataOutputStream dos) throws IOException {
									int op;
									while ((op = dis.read()) != -1) {
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
										}
									}
									throw new IOException("End of stream");
								}
							});
							final ImageConsumer ic = new ImageConsumer() {
								@Override
								public void imageProduced(final ImageConsumer consumer, final byte[] image) {
									queue.add(new NetTask("send_image", sock) {
										@Override
										public void run(final DataInputStream dis, final DataOutputStream dos) throws IOException {
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
		setHost(System.getProperty("user.name"));
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
