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
import org.whired.inspexi.tools.Robot;
import org.whired.inspexi.tools.ScreenCapture;
import org.whired.inspexi.tools.SessionListener;
import org.whired.inspexi.tools.Slave;
import org.whired.inspexi.tools.WinRobot;

import com.sun.jna.Platform;

/**
 * A slave
 */
public class LocalSlave extends Slave {
	private static String FS = System.getProperty("file.separator");

	private final ServerSocket ssock;
	private final ScreenCapture capture;

	public LocalSlave() throws AWTException, IOException {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				if (ssock != null) {
					try {
						ssock.close();
					}
					catch (IOException e) {
					}
				}
				try {
					String JAVA = System.getProperty("java.home") + FS + "bin" + FS + "java";
					System.out.println("Exec: " + JAVA + " -classpath ispx_updt.jar org.whired.inspexi.updater.SlaveUpdater");
					new ProcessBuilder(JAVA, "-classpath", "ispx_updt.jar", "org.whired.inspexi.updater.SlaveUpdater").start();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}

		}));

		setHost(System.getProperty("user.name"));
		setOS(System.getProperty("os.name") + "_" + System.getProperty("os.arch"));
		setVersion(VERSION);

		final int PORT = 43596;

		// Start the server
		System.out.print("Starting server..");
		ssock = new ServerSocket(PORT);
		System.out.println("success.");

		// Set up capture
		final Robot robot = Platform.isWindows() ? new WinRobot(.7D) : new DirectRobot(.7D);
		capture = new ScreenCapture(robot, 1);

		while (true) {
			System.out.println("Waiting for connection..");

			// Instant reaction and delegation
			final Socket sock = ssock.accept();

			long start = System.nanoTime();
			queue.add(new NetTask("handshake_reactor", sock) {
				@Override
				public void run(DataInputStream dis, DataOutputStream dos) throws IOException {
					// Read intent before doing anything
					int intent = dis.read();
					if (intent == -1) {
						throw new IOException("End of stream");
					}

					if (intent == INTENT_REBUILD) {
						ssock.close();
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
							byte[] previewImage = capture.getSingleFrame();
							dos.writeInt(previewImage.length);
							dos.write(previewImage);
						}
						else {
							queue.add(new NetTask("handle_opcode", sock) {
								@Override
								public void run(DataInputStream dis, DataOutputStream dos) throws IOException {
									int op;
									while ((op = dis.read()) != -1) {
										System.out.println("op: " + op);
										switch (op) {
										case INTENT_REBUILD:
											ssock.close();
											System.exit(0);
										break;
										case OP_DO_COMMAND:
											String cmd = dis.readUTF();
											System.out.print("EXEC: " + cmd + "..");
											String[] args = cmd.split(" ");
											try {
												new ProcessBuilder(args).start();
												System.out.println("success.");
											}
											catch (Throwable t) {
												System.out.println("fail.");
											}
										break;
										}
									}
									throw new IOException("End of stream");
								}
							});
							// TODO remove listener on session end. Start capture if there are active cons
							// I feel an auxiliary class is needed
							final ImageConsumer ic = new ImageConsumer() {
								@Override
								public void imageProduced(final ImageConsumer consumer, final byte[] image) {
									queue.add(new NetTask("send_image", sock) {
										@Override
										public void run(DataInputStream dis, DataOutputStream dos) throws IOException {
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
			});
			System.out.println("Time after accept: ~" + (System.nanoTime() - start) / 1000000F + "ms");
		}
	}

	private final SessionListener sessl = new SessionListener() {
		@Override
		public void sessionEnded(String reason) {
			System.out.println("Session ended: " + reason);
		}
	};

	private final NetTaskQueue queue = new NetTaskQueue(sessl);

	public static void main(String[] args) throws IOException, AWTException {
		new LocalSlave();
	}
}
