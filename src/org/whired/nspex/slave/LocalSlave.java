package org.whired.nspex.slave;

import java.awt.AWTException;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.logging.Level;

import org.whired.nspex.net.Communicable;
import org.whired.nspex.net.NioServer;
import org.whired.nspex.tools.AWTRobot;
import org.whired.nspex.tools.Robot;
import org.whired.nspex.tools.ScreenCapture;
import org.whired.nspex.tools.Slave;
import org.whired.nspex.tools.WinRobot;
import org.whired.nspex.tools.logging.Log;

import com.sun.jna.Platform;

/**
 * A slave whose responsibility is to host masters
 * @author Whired
 */
public abstract class LocalSlave extends NioServer implements Slave {
	/** Basic information for this slave */
	private String os, user, host, version;
	private boolean online;
	/** The robot that handles screen captures */
	protected final Robot robot;
	/** The screen capture that will record the screen */
	protected final ScreenCapture capture;
	// TODO mv..
	protected Dimension thumbSize;

	/**
	 * Creates a new local slave that listens on the specified port
	 * @param port the port to listen on
	 * @throws IOException if the server cannot be started
	 */
	public LocalSlave(int port, Robot robot, ScreenCapture capture) throws IOException {
		super(port);

		this.robot = robot;
		this.capture = capture;

		// Set this slave's properties
		setUser(System.getProperty("user.name"));
		setOS(System.getProperty("os.name") + "_" + System.getProperty("os.arch"));
		setVersion(VERSION);
	}

	public static void main(String[] args) throws IOException, AWTException {
		Log.l.setLevel(Level.INFO);
		// DI -- entire(+/-) object graph is created here
		Robot robot = Platform.isWindows() ? new WinRobot(.8F) : new AWTRobot(.8F);
		ScreenCapture capture = new ScreenCapture(robot, 1);
		LocalSlave slave = new LocalSlave(43596, robot, capture) {
			@Override
			protected Communicable getCommunicable(SelectionKey key) {
				return new DefaultSlaveCommunicable(key, this);
			}
		};

		// Finally, start listening
		slave.startListening();
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
