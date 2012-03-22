package org.whired.inspexi.slave;

import java.awt.AWTException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.whired.inspexi.master.SessionListener;
import org.whired.inspexi.tools.DirectRobot;
import org.whired.inspexi.tools.ImageProducer;
import org.whired.inspexi.tools.JNARobot;
import org.whired.inspexi.tools.NetTask;
import org.whired.inspexi.tools.NetTaskQueue;
import org.whired.inspexi.tools.Robot;
import org.whired.inspexi.tools.ScreenCapture;
import org.whired.inspexi.tools.Slave;

import com.sun.jna.Platform;

public class LocalSlave extends Slave {
	private static String FS = System.getProperty("file.separator");

	private DataOutputStream dos;
	private DataInputStream dis;
	private final ServerSocket ssock;
	private final ScreenCapture capture;
	private Socket sock;

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
		final Robot robot = Platform.isWindows() ? new JNARobot(.7D) : new DirectRobot(.7D);
		capture = new ScreenCapture(robot, 1);
		capture.addListener(new ImageProducer() {
			@Override
			public void imageProduced(final byte[] image) {
				taskQueue.add(new NetTask() {
					@Override
					public void run() throws IOException {
						dos.write(OP_TRANSFER_IMAGE);
						dos.writeInt(image.length);
						dos.write(image);
					};
				});
			}
		});
		while (true) {
			try {
				System.out.print("Waiting for connection..");
				sock = ssock.accept();
				dos = new DataOutputStream(sock.getOutputStream());
				dis = new DataInputStream(sock.getInputStream());
				System.out.println("connected.");

				// Read intent before doing anything
				int intent = dis.read();
				if (intent == -1) {
					throw new IOException("End of stream");
				}

				if (intent == INTENT_REBUILD) {
					ssock.close();
					System.exit(0);
				}
				else if (intent == INTENT_CHECK || intent == INTENT_CONNECT) {
					dos.writeUTF(getHost());
					dos.writeUTF(getOS());
					dos.writeUTF(getVersion());
				}
				if (intent == INTENT_CONNECT) {
					dos.writeShort(robot.getZoom(robot.getBounds().width));
					dos.writeShort(robot.getZoom(robot.getBounds().height));

					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								int op;
								while ((op = dis.read()) != -1) {
									System.out.println("op: " + op);
									switch (op) {
									case INTENT_REBUILD:
										capture.stop();
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
							}
							catch (Throwable t) {
								try {
									sock.close();
								}
								catch (IOException e) {
								}
							}
						}
					}).start();
					capture.start();
				}
			}
			catch (IOException e) {
				try {
					sock.close();
				}
				catch (IOException e1) {
				}
			}
		}
	}

	private final NetTaskQueue taskQueue = new NetTaskQueue(new SessionListener() {
		@Override
		public void sessionEnded(String reason) {
			System.out.println("Session ended: " + reason);
			if (sock != null) {
				try {
					if (capture != null) {
						capture.stop();
					}
					sock.close();
				}
				catch (IOException e) {
				}
			}
		}
	});

	public static void main(String[] args) throws IOException, AWTException {
		new LocalSlave();
	}
}
