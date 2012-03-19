package org.whired.inspexi.slave;

import java.awt.AWTException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.whired.inspexi.tools.DirectRobot;
import org.whired.inspexi.tools.ImageProducer;
import org.whired.inspexi.tools.JNARobot;
import org.whired.inspexi.tools.Robot;
import org.whired.inspexi.tools.ScreenCapture;
import org.whired.inspexi.tools.Slave;

import com.sun.jna.Platform;

public class LocalSlave extends Slave {
	private static String FS = System.getProperty("file.separator");

	private DataOutputStream dos = null;
	private DataInputStream dis = null;
	private ServerSocket ssock = null;

	public LocalSlave() throws IOException, AWTException {
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
		setVersion(REMOTE_VERSION);

		final int PORT = 43596;

		// Start the server
		System.out.print("Starting server..");
		ssock = new ServerSocket(PORT);
		System.out.println("success.");

		// Set up capture
		final Robot robot = Platform.isWindows() ? new JNARobot(.7D) : new DirectRobot(.7D);
		final ScreenCapture cap = new ScreenCapture(robot, 1);
		cap.addListener(new ImageProducer() {
			@Override
			public void imageProduced(byte[] image) {
				try {
					dos.write(OP_TRANSFER_IMAGE);
					dos.writeInt(image.length);
					dos.write(image);
				}
				catch (Throwable e) {
					cap.stop();
				}
			}
		});
		while (true) {
			System.out.print("Waiting for connection..");
			final Socket sock = ssock.accept();
			dos = new DataOutputStream(sock.getOutputStream());
			dis = new DataInputStream(sock.getInputStream());
			System.out.println("connected.");

			// Read intent before doing anything
			int intent = dis.read();

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
				cap.start();
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							int op;
							while ((op = dis.read()) != -1) {
								switch (op) {
								case INTENT_REBUILD:
									cap.stop();
									ssock.close();
									System.exit(0);
								break;
								}
							}
						}
						catch (Throwable t) {
						}
					}
				}).start();
			}
		}
	}

	public static void main(String[] args) throws IOException, AWTException {
		new LocalSlave();
	}
}
