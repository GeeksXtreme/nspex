package org.whired.inspexi.tools;

import java.util.HashSet;

import org.whired.inspexi.slave.ImageConsumer;

/**
 * Captures the screen
 * 
 * @author Whired
 */
public class ScreenCapture implements ImageConsumer {
	/**
	 * The robot that will grab the screen's pixels
	 */
	private final Robot robot;
	/**
	 * The frames per second
	 */
	private final int fps;
	/**
	 * Whether or not this screen capture has been started
	 */
	private boolean started;
	/**
	 * A collection of listeners to notify when an image has been produced
	 */
	private final HashSet<ImageConsumer> listeners = new HashSet<ImageConsumer>();

	/**
	 * Creates a new screen capture with the specified robot and a default FPS of 5
	 * 
	 * @param robot the robot that will grab the screen's pixels
	 */
	public ScreenCapture(Robot robot) {
		this(robot, 5);
	}

	/**
	 * Creates a new screen capture with the specified robot and fps
	 * 
	 * @param robot the robot that will grab the screen's pixels
	 * @param fps the frames to produce per second
	 */
	public ScreenCapture(Robot robot, int fps) {
		this.robot = robot;
		this.fps = fps;
	}

	/**
	 * Adds a listener to the collection. If the listener is already registered, this method does nothing
	 * 
	 * @param listener the listener to add
	 */
	public synchronized void addListener(ImageConsumer listener) {
		listeners.add(listener);
	}

	/**
	 * Removes a listener from the collection. If the listener is not registered, this method does nothing
	 * 
	 * @param listener the listener to remove
	 */
	public synchronized void removeListener(ImageConsumer listener) {
		listeners.remove(listener);
	}

	/**
	 * Starts this screen capture
	 */
	public void start() {
		synchronized (this) {
			if (!started) {
				started = true;
			}
			else {
				return;
			}
		}
		long start;
		while (isStarted()) {
			start = System.currentTimeMillis();
			imageProduced(getSingleFrame());
			try {
				Thread.sleep(Math.max(1000 / fps - (System.currentTimeMillis() - start), 0));
			}
			catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Produces a single frame from this capture
	 * 
	 * @return
	 */
	public byte[] getSingleFrame() {
		return robot.getBytePixels();
	}

	private synchronized boolean isStarted() {
		return started;
	}

	/**
	 * Stops this screen capture
	 */
	public synchronized void stop() {
		started = false;
	}

	@Override
	public synchronized void imageProduced(byte[] image) {
		for (ImageConsumer l : listeners) {
			l.imageProduced(image);
		}
	}
}
