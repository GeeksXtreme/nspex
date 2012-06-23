package org.whired.nspex.tools;

import java.util.HashSet;

import org.whired.nspex.slave.ImageConsumer;
import org.whired.nspex.tools.logging.Log;

/**
 * Captures the screen
 * @author Whired
 */
public class ScreenCapture implements ImageConsumer {
	/** The robot that will grab the screen's pixels */
	private final Robot robot;
	/** The frames per second */
	private final int fps;
	/** Whether or not this screen capture has been running */
	private boolean running;
	/** A collection of listeners to notify when an image has been produced */
	private final HashSet<ImageConsumer> listeners = new HashSet<ImageConsumer>();
	/** The task to run */
	private final Runnable captureTask = new Runnable() {
		@Override
		public void run() {
			long start;
			while (isRunning()) {
				start = System.currentTimeMillis();
				imageProduced(getSingleFrame());
				try {
					Thread.sleep(Math.max(1000 / fps - (System.currentTimeMillis() - start), 0));
				}
				catch (final InterruptedException e) {
				}
			}
		}
	};
	/** The dedicated thread that the task runs on */
	private Thread capThread;

	/**
	 * Creates a new screen capture with the specified robot and a default FPS of 5
	 * @param robot the robot that will grab the screen's pixels
	 */
	public ScreenCapture(final Robot robot) {
		this(robot, 5);
	}

	/**
	 * Creates a new screen capture with the specified robot and fps
	 * @param robot the robot that will grab the screen's pixels
	 * @param fps the frames to produce per second
	 */
	public ScreenCapture(final Robot robot, final int fps) {
		this.robot = robot;
		this.fps = fps;
	}

	/**
	 * Adds a listener to the collection. If the listener is already registered, this method does nothing
	 * @param listener the listener to add
	 */
	public synchronized void addListener(final ImageConsumer listener) {

		// Restart the capping process if we have a viewer
		if (listeners.size() == 0) {
			running = true;
			capThread = new Thread(captureTask);
			capThread.start();
		}
		Log.l.fine("Adding listener=" + listener.hashCode() + "=" + listeners.add(listener));
	}

	/**
	 * Removes a listener from the collection. If the listener is not registered, this method does nothing
	 * @param listener the listener to remove
	 */
	public synchronized void removeListener(final ImageConsumer listener) {
		listeners.remove(listener);
		// If we have no viewers, stop the capping process
		if (running && listeners.size() == 0) {
			running = false;
			capThread.interrupt();
		}
	}

	/**
	 * Produces a single frame from this capture
	 * @return
	 */
	public byte[] getSingleFrame() {
		return robot.getPixels();
	}

	/**
	 * Determines whether or not this capture is running
	 * @return {@code true} if this capture is running, otherwise {@code false}
	 */
	private synchronized boolean isRunning() {
		return running;
	}

	@Override
	public synchronized void imageProduced(final byte[] image) {
		for (final ImageConsumer l : listeners) {
			l.imageProduced(image);
		}
	}
}
