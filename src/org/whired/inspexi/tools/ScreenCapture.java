package org.whired.inspexi.tools;

import java.util.HashSet;

public class ScreenCapture implements ImageProducer {
	private final Robot robot;
	private final int fps;
	private boolean started;
	private final HashSet<ImageProducer> listeners = new HashSet<ImageProducer>();

	public ScreenCapture(Robot robot) {
		this(robot, 5);
	}

	public ScreenCapture(Robot robot, int fps) {
		this.robot = robot;
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

	@Override
	public synchronized void imageProduced(byte[] image) {
		for (ImageProducer l : listeners) {
			l.imageProduced(image);
		}
	}
}
