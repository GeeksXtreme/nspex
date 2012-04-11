package org.whired.inspexi.tools;

import java.util.HashSet;

/**
 * Captures the screen
 * 
 * @author Whired
 */
public class ScreenCapture implements ImageProducer {
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
	private final HashSet<ImageProducer> listeners = new HashSet<ImageProducer>();

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
	public synchronized void addListener(ImageProducer listener) {
		listeners.add(listener);
	}

	/**
	 * Removes a listener from the collection. If the listener is not registered, this method does nothing
	 * 
	 * @param listener the listener to remove
	 */
	public synchronized void removeListener(ImageProducer listener) {
		listeners.remove(listener);
	}

	/**
	 * Starts this screen capture
	 */
	public synchronized void start() {
		if (!started) {
			started = true;
			long start;
			while (started) {
				start = System.currentTimeMillis();
				imageProduced(robot.getBytePixels());
				try {
					Thread.sleep(Math.max(1000 / fps - (System.currentTimeMillis() - start), 0));
				}
				catch (InterruptedException e) {
				}
			}
		}
	}

	/**
	 * Stops this screen capture
	 */
	public synchronized void stop() {
		started = false;
	}

	@Override
	public synchronized void imageProduced(byte[] image) {
		for (ImageProducer l : listeners) {
			l.imageProduced(image);
		}
	}
}
