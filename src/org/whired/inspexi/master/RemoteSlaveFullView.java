package org.whired.inspexi.master;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import org.whired.inspexi.tools.SessionListener;

public class RemoteSlaveFullView extends JFrame implements SessionListener, ImageConsumer {
	/** The image to draw, as received by the remote slave */
	private Image image;
	/** The panel to draw on */
	private final JPanel panel;

	/**
	 * Creates a new full view for the specified slave
	 * @param slave the slave to create the view for
	 */
	public RemoteSlaveFullView(final RemoteSlaveModel slave) {
		super(slave.getIp());
		panel = new JPanel() {
			@Override
			public void paint(final Graphics g) {
				if (image == null) {
					return;
				}
				// This actually shouldn't be a problem at 1 fps
				//final Graphics2D g2 = (Graphics2D) g;
				//g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
				//g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				//g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
				//g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
				//g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
				//g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
				g/* 2 */.drawImage(image, 0, 0, this);
				//g2.dispose();
				g.dispose();
			}
		};
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

				final JScrollPane scrollPane = new JScrollPane();
				scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				scrollPane.getVerticalScrollBar().setUI(new MinimalScrollBar(scrollPane.getVerticalScrollBar()));
				scrollPane.setViewportBorder(null);

				final GroupLayout groupLayout = new GroupLayout(getContentPane());

				groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(groupLayout.createSequentialGroup().addComponent(panel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)).addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
				groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(groupLayout.createSequentialGroup().addComponent(panel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)));

				final JConsole console = new JConsole();
				console.addCommandListener(new CommandListener() {
					@Override
					public void doCommand(final String command) {
						slave.executeRemoteCommand(command);
					}
				});

				scrollPane.setViewportView(console);
				getContentPane().setLayout(groupLayout);

				addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(final WindowEvent e) {
						slave.endSession("User requested");
						super.windowClosing(e);
					}
				});
			}
		});

		// Keep last
		slave.setImageConsumer(this);
		slave.setSessionListener(this);
	}

	@Override
	public void imageProduced(final Image image) {
		this.image = image;
		panel.repaint();
	}

	@Override
	public void sessionEnded(final String reason, final Throwable t) {
		this.dispose();
	}

	/**
	 * Runs the specified runnable on the EDT, respecting whether or not the current thread is the EDT
	 * @param run the runnable to run
	 */
	private static void runOnEdt(final Runnable run) {
		if (EventQueue.isDispatchThread()) {
			run.run();
		}
		else {
			try {
				EventQueue.invokeAndWait(run);
			}
			catch (final Throwable t) {
			}
		}
	}

	@Override
	public void imageResized(final int width, final int height) {
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				panel.setPreferredSize(new Dimension(width, height));
				panel.invalidate();
				pack();
				setMinimumSize(getSize());
				setLocationRelativeTo(null);
				setVisible(true);
			}
		});

	}
}
