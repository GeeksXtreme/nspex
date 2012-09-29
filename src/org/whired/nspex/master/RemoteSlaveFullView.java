package org.whired.nspex.master;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.whired.nspex.net.Communicable;
import org.whired.nspex.tools.RemoteFile;
import org.whired.nspex.tools.Slave;
import org.whired.nspex.tools.SlaveView;
import org.whired.nspex.tools.logging.Log;

/**
 * A full, administrative view
 * @author Whired
 */
public class RemoteSlaveFullView extends JFrame implements SlaveView {
	/** The image to draw, as received by the remote slave */
	private Image image;
	/** The panel to draw on */
	private final JPanel panel;
	/** The remote file chooser */
	private RemoteFileChooserPanel fileChooser;
	private JScrollPane scrollPane;
	/** The main view that created this view */
	private final SlaveView mainView;
	/** The console used for controlling the remote slave */
	private JConsole console;
	/** Whether or not a disconnect was forced by the user */
	private boolean forcedDisconnect;

	/**
	 * Creates a new full view for the specified slave
	 * @param slave the slave to create the view for
	 */
	public RemoteSlaveFullView(final SlaveView mainView, final RemoteSlave slave) {
		super(slave.getUser() + "@" + slave.getHost() + " (" + slave.getOS() + ") v" + slave.getVersion());
		this.mainView = mainView;
		panel = new JPanel() {
			@Override
			public void paintComponent(final Graphics g) {
				if (image == null) {
					return;
				}
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.drawImage(image, 0, 0, this);
				g.dispose();
			}
		};
		final MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				try {
					if (SwingUtilities.isLeftMouseButton(e)) {
						slave.leftMouseDown((short) e.getPoint().x, (short) e.getPoint().y);
					}
					else if (SwingUtilities.isRightMouseButton(e)) {
						slave.rightMouseDown((short) e.getPoint().x, (short) e.getPoint().y);
					}
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				try {
					if (SwingUtilities.isLeftMouseButton(e)) {
						slave.leftMouseUp((short) e.getPoint().x, (short) e.getPoint().y);
					}
					else if (SwingUtilities.isRightMouseButton(e)) {
						slave.rightMouseUp((short) e.getPoint().x, (short) e.getPoint().y);
					}
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				try {
					slave.mouseMove((short) e.getPoint().x, (short) e.getPoint().y);
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		};
		panel.addMouseListener(ma);
		panel.addMouseMotionListener(ma);
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

				scrollPane = new JScrollPane();
				scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				scrollPane.getVerticalScrollBar().setUI(new MinimalScrollBar(scrollPane.getVerticalScrollBar()));
				scrollPane.setViewportBorder(null);

				console = new JConsole();
				console.addCommandListener(new CommandListener() {
					@Override
					public void doCommand(final String command) {
						try {
							slave.executeRemoteCommand(command);
						}
						catch (final IOException e) {
							e.printStackTrace();
						}
					}
				});
				scrollPane.setViewportView(console);

				fileChooser = new RemoteFileChooserPanel() {

					@Override
					protected void requestFileAction(final int action, final String path) {
						try {
							slave.requestFileAction(action, path);
						}
						catch (final IOException e) {
							e.printStackTrace();
						}
					}

					@Override
					protected void requestChildren(final String parentPath) {
						try {
							slave.requestChildFiles(parentPath);
						}
						catch (final IOException e) {
							e.printStackTrace();
						}
					}
				};

				final BorderLayout layout = new BorderLayout();
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
						forcedDisconnect = true;
						Communicable c;
						try {
							if ((c = slave.getCommunicable()) != null) {
								c.disconnect();
							}
							else {
								disconnected(slave);
							}
						}
						catch (final IOException e1) {
							e1.printStackTrace();
						}
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
				t.printStackTrace();
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
	public void setFile(final RemoteFile file) {
		fileChooser.setSelectedFile(file);
	}

	@Override
	public void addChildFiles(final char fs, final String parentPath, final RemoteFile[] childFiles) {
		fileChooser.addChildren(fs, parentPath, childFiles);
	}

	@Override
	public Dimension getThumbSize() {
		return fileChooser.getThumbSize();
	}

	@Override
	public void disconnected(final Slave slave) {
		Log.l.warning("Disconnected from " + slave);
		if (!forcedDisconnect) {
			slave.setOnline(false);
			mainView.disconnected(slave);
		}
		this.dispose();
	}

	@Override
	public void connected(final Slave slave) {
	}

	@Override
	public void displayOutput(String output) {
		console.append(output);
	}
}
