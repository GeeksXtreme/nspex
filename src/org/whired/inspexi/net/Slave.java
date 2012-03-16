package org.whired.inspexi.net;

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

import com.sun.jna.Platform;

public class Slave {
	private static String FS = System.getProperty("file.separator");

	private DataOutputStream dos = null;
	private DataInputStream dis = null;

	public Slave() throws IOException, AWTException {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new ProcessBuilder("\"" + System.getProperty("java.home") + FS + "bin" + FS + "java\"", "-classpath", "ispx_updt.jar", "org.whired.inspexi.updater.SlaveUpdater").start();
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}));

		final int PORT = 43596;

		// Start the server
		System.out.print("Starting server..");
		final ServerSocket ssock = new ServerSocket(PORT);
		System.out.println("success.");

		// Set up capture
		final Robot robot = Platform.isWindows() ? new JNARobot() : new DirectRobot();
		final ScreenCapture cap = new ScreenCapture(robot, 1);
		cap.addListener(new ImageProducer() {
			@Override
			public void imageProduced(byte[] image) {
				try {
					dos.write(0);
					dos.writeInt(image.length);
					dos.write(image);
				}
				catch (Throwable e) {
					cap.stop();
				}

			}
		});
		while (true) {
			System.out.print("[Server] Waiting for connection..");
			final Socket sock = ssock.accept();
			dos = new DataOutputStream(sock.getOutputStream());
			dis = new DataInputStream(sock.getInputStream());
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						int op;
						while ((op = dis.read()) != -1) {
							switch (op) {
							case 0:
								cap.stop();
								sock.close();
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
			System.out.println("connected.");
			dos.writeShort(getZoom(cap.getBounds().width));
			dos.writeShort(getZoom(cap.getBounds().height));
			dos.writeUTF(System.getProperty("user.name") + " on " + System.getProperty("os.name") + "-" + System.getProperty("os.version") + "(" + System.getProperty("os.arch") + ")");
			cap.start();
		}
	}

	private static final double ZOOM = .70f;

	public static int getZoom(int orig) {
		return (int) (orig * ZOOM);
	}

	public static void main(String[] args) throws IOException, AWTException {
		new Slave();
	}
}
