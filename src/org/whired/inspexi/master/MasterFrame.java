package org.whired.inspexi.master;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;

import org.whired.inspexi.tools.RemoteFile;
import org.whired.inspexi.tools.Slave;
import org.whired.inspexi.tools.SlaveView;
import org.whired.inspexi.tools.logging.Log;
import org.whired.inspexi.tools.logging.LogFormatter;

/**
 * Controls and displays feedback from a master server
 * @author Whired
 */
public class MasterFrame extends JFrame implements ControllerEventListener, SlaveView {
	/** The pane that contains content components */
	private final JPanel contentPane;
	/** The table that lists available slaves */
	private final JTable table;
	/** The common font that is prettier than OS defaults */
	private final Font font = new Font("SansSerif", Font.PLAIN, 9);
	/** The text pane that outputs log messages */
	private final JTextPane pane;
	/** Listens for control commands */
	private final ControllerEventListener listener;
	/** The pane that is used as a canvas for {@link #previewImage} */
	private final JPanel pnlPreview;
	/** The table model used by {@link #table} */
	private final DefaultTableModel model = new DefaultTableModel(new String[] { "IP", "User", "OS", "Version", "Status" }, 0) {
		@Override
		public boolean isCellEditable(final int rowIndex, final int mColIndex) {
			return false;
		}
	};
	/** A preview image for the selected slave */
	private Image previewImage;

	/**
	 * Creates a new frame for the specified controller event listener
	 * @param listener the listener to notify of controller events
	 */
	public MasterFrame(final ControllerEventListener listener) {
		this.listener = listener;
		setTitle("Inspexi v" + Slave.VERSION);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 700, 500);
		contentPane = new JPanel();
		contentPane.setBorder(null);
		setContentPane(contentPane);
		final GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 494, 80, 27, 27, 0 };
		gbl_contentPane.rowHeights = new int[] { 245, 1, 75, 0 };
		gbl_contentPane.columnWeights = new double[] { 1.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_contentPane.rowWeights = new double[] { 1.0, 0.0, 1.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getVerticalScrollBar().setUI(new MinimalScrollBar(scrollPane.getVerticalScrollBar()));
		scrollPane.setViewportBorder(null);
		final GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.insets = new Insets(0, 0, -3, 0);
		gbc_scrollPane.gridwidth = 4;
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		contentPane.add(scrollPane, gbc_scrollPane);

		table = new JTable();

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent e) {
				if (table.rowAtPoint(e.getPoint()) == -1 || e.getButton() == MouseEvent.BUTTON3) {
					table.getSelectionModel().clearSelection();
				}
			}
		});

		table.setShowVerticalLines(true);
		table.setFont(font);
		table.setShowHorizontalLines(true);
		table.setFillsViewportHeight(true);
		scrollPane.setViewportView(table);

		table.setModel(model);
		table.setRowSorter(new TableRowSorter<TableModel>(model));

		table.setBorder(null);
		table.getTableHeader().setFont(font);
		table.getTableHeader().setReorderingAllowed(false);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(final ListSelectionEvent e) {
				// If we're still picking things we don't want to spam
				if (!e.getValueIsAdjusting()) {
					if (table.getSelectionModel().isSelectionEmpty()) {
						updatePreviewImage(null);
					}
					else {
						refresh(new RemoteSlave[] { (RemoteSlave) model.getValueAt(table.getSelectionModel().getLeadSelectionIndex(), 0) });
					}
				}
			}
		});

		final JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				RemoteSlave[] slaves = getSelectedSlaves();
				if (slaves.length > 0) {
					connect(slaves);
				}
				else {
					Log.l.info("Select the slaves to connect to");
				}
			}
		});
		btnConnect.setToolTipText("Connect");
		btnConnect.setFont(font);
		final GridBagConstraints gbc_btnConnect = new GridBagConstraints();
		gbc_btnConnect.anchor = GridBagConstraints.SOUTHWEST;
		gbc_btnConnect.gridx = 0;
		gbc_btnConnect.gridy = 1;
		contentPane.add(btnConnect, gbc_btnConnect);

		final JButton btnRefresh = new JButton("");
		btnRefresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				RemoteSlave[] slaves = getSelectedSlaves();
				if (slaves.length == 0) {
					slaves = getAllSlaves();
				}
				refresh(slaves);
			}
		});

		final JButton btnBuild = new JButton("");
		btnBuild.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				RemoteSlave[] slaves = getSelectedSlaves();
				if (slaves.length > 0) {
					rebuild(slaves);
				}
				else {
					Log.l.info("Select the slaves to rebuild");
				}
			}
		});

		final JButton btnAdd = new JButton("");
		btnAdd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				String ip;
				if ((ip = JOptionPane.showInputDialog(MasterFrame.this, "Enter IP:")) != null && ip.length() > 0) {
					updateInformation(new RemoteSlave[] { new RemoteSlave(ip) });
				}
			}
		});
		btnAdd.setIcon(new ImageIcon(this.getClass().getResource("/org/whired/inspexi/master/resources/plus.gif")));
		final GridBagConstraints gbc_btnAdd = new GridBagConstraints();
		gbc_btnAdd.anchor = GridBagConstraints.EAST;
		gbc_btnAdd.fill = GridBagConstraints.VERTICAL;
		gbc_btnAdd.gridx = 1;
		gbc_btnAdd.gridy = 1;
		contentPane.add(btnAdd, gbc_btnAdd);
		btnBuild.setToolTipText("Rebuild");
		btnBuild.setIcon(new ImageIcon(this.getClass().getResource("/org/whired/inspexi/master/resources/rebuild.gif")));
		final GridBagConstraints gbc_btnBuild = new GridBagConstraints();
		gbc_btnBuild.fill = GridBagConstraints.VERTICAL;
		gbc_btnBuild.gridx = 2;
		gbc_btnBuild.gridy = 1;
		contentPane.add(btnBuild, gbc_btnBuild);
		btnRefresh.setToolTipText("Refresh list");
		btnRefresh.setIcon(new ImageIcon(this.getClass().getResource("/org/whired/inspexi/master/resources/refresh.png")));
		final GridBagConstraints gbc_btnRefresh = new GridBagConstraints();
		gbc_btnRefresh.fill = GridBagConstraints.VERTICAL;
		gbc_btnRefresh.gridx = 3;
		gbc_btnRefresh.gridy = 1;
		contentPane.add(btnRefresh, gbc_btnRefresh);

		final JScrollPane scrollPane_1 = new JScrollPane();
		final GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 2;
		contentPane.add(scrollPane_1, gbc_scrollPane_1);

		pane = new JTextPane();
		pane.setBackground(Color.WHITE);
		((DefaultCaret) pane.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		pane.setEditable(false);
		pane.setFont(font);

		final LogFormatter formatter = new LogFormatter();
		Log.l.addHandler(new Handler() {

			@Override
			public void publish(final LogRecord record) {
				if (isLoggable(record)) {
					appendText(pane, formatter.format(record), true);
				}
			}

			@Override
			public void flush() {
			}

			@Override
			public void close() throws SecurityException {
			}
		});

		final OutputStream out = new OutputStream() {
			@Override
			public void write(final int b) throws IOException {
				appendText(pane, String.valueOf((char) b), false);
			}

			@Override
			public void write(final byte[] b, final int off, final int len) throws IOException {
				appendText(pane, new String(b, off, len), false);
			}

			@Override
			public void write(final byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};
		final PrintStream p = new PrintStream(out, true);
		System.setOut(p);
		System.setErr(p);

		scrollPane_1.setViewportView(pane);
		scrollPane_1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		final Color grayBorder = new Color(146, 151, 161);
		pnlPreview = new JPanel() {
			@Override
			public void paint(final Graphics g) {
				g.setColor(this.getBackground());
				g.fillRect(0, 0, this.getWidth(), this.getHeight());
				if (previewImage != null) {
					final Graphics2D g2 = (Graphics2D) g;
					g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					g2.drawImage(previewImage, 0, 0, this.getWidth(), this.getHeight(), 0, 0, previewImage.getWidth(this), previewImage.getHeight(this), this);
				}
				g.setColor(grayBorder);
				g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);
				g.dispose();
			}
		};
		pnlPreview.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent e) {
				if (previewImage != null) {
					RemoteSlave[] slaves = getSelectedSlaves();
					if (slaves.length > 0) {
						connect(slaves);
					}
				}
			}
		});
		final GridBagConstraints gbc_pnlPreview = new GridBagConstraints();
		gbc_pnlPreview.insets = new Insets(2, 0, 2, 2);
		gbc_pnlPreview.gridwidth = 3;
		gbc_pnlPreview.fill = GridBagConstraints.BOTH;
		gbc_pnlPreview.gridx = 1;
		gbc_pnlPreview.gridy = 2;
		contentPane.add(pnlPreview, gbc_pnlPreview);
		scrollPane_1.getVerticalScrollBar().setUI(new MinimalScrollBar(scrollPane_1.getVerticalScrollBar()));
	}

	private void updateInformation(Slave slv) {
		updateInformation(new Slave[] { slv });
	}

	/**
	 * Adds the specified slaves to the current list
	 * @param slaves the slaves to add
	 */
	private void updateInformation(final Slave[] slaves) {
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				for (final Slave slv : slaves) {
					for (int i = 0; i < model.getRowCount(); i++) {
						if (model.getValueAt(i, 0).toString().equals(slv.toString())) {
							model.setValueAt(slv.getUser(), i, 1);
							model.setValueAt(slv.getOS(), i, 2);
							model.setValueAt(slv.getVersion(), i, 3);
							model.setValueAt(slv.isOnline() ? "Online" : "Offline", i, 4);
							return;
						}
					}
					model.addRow(new Object[] { slv, slv.getUser(), slv.getOS(), slv.getVersion(), slv.isOnline() ? "Online" : "Offline" });
				}
			}
		});
	}

	/**
	 * Removes the specified slave
	 * @param slave the slave to remove
	 */
	public void removeSlave(final Slave slv) {
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < model.getRowCount(); i++) {
					if (model.getValueAt(i, 0).equals(slv)) {
						model.removeRow(i);
						break;
					}
				}
			}
		});
	}

	/**
	 * Updates {@link #previewImage} and {@link #pnlPreview} according to the specified image
	 * @param newImg the new image to display; can be null
	 */
	private final void updatePreviewImage(Image newImg) {
		previewImage = newImg;
		if (newImg != null) {
			pnlPreview.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		else {
			pnlPreview.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		pnlPreview.repaint();
	}

	/**
	 * Appends the given string to the given text pane with proper formatting and scrolling
	 * @param pane the pane to append text to
	 * @param text the text to append
	 * @param timeStamped whether or not the text contains timestamp information
	 */
	private void appendText(final JTextPane pane, final String text, final boolean timeStamped) {
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				final Document doc = pane.getStyledDocument();
				try {
					doc.insertString(doc.getLength(), !text.contains(System.getProperty("line.separator")) && !timeStamped ? "[" + LogFormatter.DATE_FORMAT.format(Calendar.getInstance().getTime()) + "] System: " + text : text, null);
					pane.setCaretPosition(doc.getLength());
				}
				catch (final BadLocationException e) {
				}
			}
		});
	}

	/**
	 * Pauses the current thread and runs the specified runnable on the event dispatch thread
	 * @param run the runnable to run
	 */
	private static void runOnEdt(final Runnable run) {
		if (EventQueue.isDispatchThread()) {
			run.run();
		}
		else {
			EventQueue.invokeLater(run);
		}
	}

	/**
	 * Gets all the listed slaves regardless of selection
	 * @return the slaves
	 */
	private RemoteSlave[] getAllSlaves() {
		final RemoteSlave[] slaves = new RemoteSlave[model.getRowCount()];
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < model.getRowCount(); i++) {
					slaves[i] = (RemoteSlave) model.getValueAt(i, 0);
				}
			}
		});
		return slaves;
	}

	/**
	 * Gets all the currently selected slaves
	 * @return the selected slaves, or an empty array if none are selected
	 */
	public RemoteSlave[] getSelectedSlaves() {
		final int[] rows = table.getSelectedRows();
		final RemoteSlave[] slaves = new RemoteSlave[rows.length];
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < rows.length; i++) {
					slaves[i] = (RemoteSlave) model.getValueAt(rows[i], 0);
				}
			}
		});
		return slaves;
	}

	@Override
	public void connect(final RemoteSlave[] slaves) {
		listener.connect(slaves);
	}

	@Override
	public void rebuild(final RemoteSlave[] slaves) {
		listener.rebuild(slaves);
	}

	@Override
	public void refresh(final RemoteSlave[] slaves) {
		listener.refresh(slaves);
	}

	@Override
	public void imageProduced(final Image image) {
		updatePreviewImage(image);
	}

	@Override
	public void imageResized(final int width, final int height) {
		Log.l.config("");
	}

	@Override
	public void setThumbnail(Image thumb) {
		Log.l.config("");
	}

	@Override
	public void addChildFiles(String parentPath, RemoteFile[] childFiles) {
		Log.l.config("");
	}

	@Override
	public void disconnected(Slave slave) {
		updateInformation(slave);
	}

	@Override
	public Dimension getThumbSize() {
		return pnlPreview.getPreferredSize();
	}

	@Override
	public void connected(Slave slave) {
		updateInformation(slave);
	}

}
