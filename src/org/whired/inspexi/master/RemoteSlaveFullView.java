package org.whired.inspexi.master;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import org.whired.inspexi.net.Communicable;
import org.whired.inspexi.tools.RemoteFile;
import org.whired.inspexi.tools.SlaveView;

public class RemoteSlaveFullView extends JFrame implements SlaveView {
	/** The image to draw, as received by the remote slave */
	private Image image;
	/** The panel to draw on */
	private final JPanel panel;
	private RemoteFileChooserPanel fileChooser;
	private JScrollPane scrollPane;

	/**
	 * Creates a new full view for the specified slave
	 * @param slave the slave to create the view for
	 */
	public RemoteSlaveFullView(final RemoteSlave slave) {
		super(slave.getIp());
		panel = new JPanel() {
			@Override
			public void paint(final Graphics g) {
				if (image == null) {
					return;
				}
				// This actually shouldn't be a problem at 1 fps
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2.drawImage(image, 0, 0, this);
				g.dispose();//..?
			}
		};
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

				scrollPane = new JScrollPane();
				scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				scrollPane.getVerticalScrollBar().setUI(new MinimalScrollBar(scrollPane.getVerticalScrollBar()));
				scrollPane.setViewportBorder(null);

				final JConsole console = new JConsole();
				console.addCommandListener(new CommandListener() {
					@Override
					public void doCommand(final String command) {
						slave.executeRemoteCommand(command);
					}
				});
				scrollPane.setViewportView(console);

				fileChooser = new RemoteFileChooserPanel() {

					@Override
					protected void requestThumbnail(String path) {
						slave.requestThumbnail(path);
					}

					@Override
					protected void requestChildren(String parentPath) {
						slave.requestChildFiles(parentPath);
					}
				};

				BorderLayout layout = new BorderLayout();
				layout.addLayoutComponent(scrollPane, BorderLayout.SOUTH);
				layout.addLayoutComponent(panel, BorderLayout.CENTER);
				layout.addLayoutComponent(fileChooser, BorderLayout.WEST);
				getContentPane().add(scrollPane);
				getContentPane().add(panel);
				getContentPane().add(fileChooser);
				getContentPane().setLayout(layout);

				addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(final WindowEvent e) {
						Communicable c;
						if ((c = slave.getCommunicable()) != null) {
							c.disconnect();
						}
						super.windowClosing(e);
					}
				});
			}
		});

		// Keep last
		slave.setView(this);
	}

	@Override
	public void imageProduced(final Image image) {
		this.image = image;
		panel.repaint();
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
				Dimension d = new Dimension(width, height);
				panel.setPreferredSize(d);
				panel.setMaximumSize(d);
				d = new Dimension(200, height);
				fileChooser.setPreferredSize(d);
				fileChooser.setMaximumSize(d);
				scrollPane.setPreferredSize(new Dimension(getWidth(), 100));
				invalidate();
				pack();
				setMinimumSize(getSize());
				setLocationRelativeTo(null);
				setVisible(true);
			}
		});

	}

	@Override
	public void setThumbnail(Image thumb) {
		fileChooser.setThumbnail(thumb);
	}

	@Override
	public void addChildFiles(String parentPath, RemoteFile[] childFiles) {
		fileChooser.addChildren(parentPath, childFiles);
	}

	@Override
	public Dimension getThumbSize() {
		return fileChooser.getThumbSize();
	}

	@Override
	public void disconnected() {
		this.dispose();
	}
}
