package org.whired.inspexi.tools;

import java.awt.Rectangle;
import java.util.HashSet;

public class ScreenCapture implements ImageProducer {
	private final Robot robot;
	private final int fps;
	private final Rectangle bounds;
	private boolean started;
	private final HashSet<ImageProducer> listeners = new HashSet<ImageProducer>();

	public ScreenCapture(Robot robot) {
		this(robot, robot.getScreenBounds(), 5);
	}

	public ScreenCapture(Robot robot, Rectangle bounds) {
		this(robot, bounds, 5);
	}

	public ScreenCapture(Robot robot, int fps) {
		this(robot, robot.getScreenBounds(), fps);
	}

	public ScreenCapture(Robot robot, Rectangle bounds, int fps) {
		this.robot = robot;
		this.bounds = bounds;
		this.fps = fps;
	}

	public synchronized void addListener(ImageProducer listener) {
		listeners.add(listener);
	}

	public synchronized void removeListener(ImageProducer listener) {
		listeners.remove(listener);
	}

	public void start() {
		if (started) {
			throw new RuntimeException("Capturing already started!");
		}
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

	public void stop() {
		started = false;
	}

	public Rectangle getBounds() {
		return bounds;
	}

	public int getArea() {
		return bounds.width * bounds.height;
	}

	@Override
	public synchronized void imageProduced(byte[] image) {
		for (ImageProducer l : listeners) {
			l.imageProduced(image);
		}
	}
}
