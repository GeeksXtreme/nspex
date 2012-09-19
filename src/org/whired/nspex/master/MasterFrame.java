package org.whired.nspex.master;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
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
import javax.swing.JPanel;
import javax.swing.JProgressBar;
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

import org.whired.nspex.tools.RemoteFile;
import org.whired.nspex.tools.Slave;
import org.whired.nspex.tools.SlaveView;
import org.whired.nspex.tools.logging.Log;
import org.whired.nspex.tools.logging.LogFormatter;

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
	private final DefaultTableModel model = new DefaultTableModel(new String[] { "Host", "User", "OS", "Version", "Status" }, 0) {
		@Override
		public boolean isCellEditable(final int rowIndex, final int mColIndex) {
			return false;
		}
	};
	/** A preview image for the selected slave */
	private Image previewImage;
	/** The connect button */
	final JButton btnConnect;
	/** The refresh button */
	final JButton btnRefresh;
	/** The build button */
	final JButton btnBuild;
	private final JPanel panel;
	private final JProgressBar progressBar;

	/**
	 * Creates a new frame for the specified controller event listener
	 * @param listener the listener to notify of controller events
	 */
	public MasterFrame(final ControllerEventListener listener) {
		this.listener = listener;
		setTitle("nspex v" + Slave.VERSION);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 730, 530);
		contentPane = new JPanel();
		contentPane.setBorder(null);
		setContentPane(contentPane);
		final GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 494, 192, 0 };
		gbl_contentPane.rowHeights = new int[] { 85, 23, 151, 0 };
		gbl_contentPane.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		gbl_contentPane.rowWeights = new double[] { 1.0, 0.0, 0.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getVerticalScrollBar().setUI(new MinimalScrollBar(scrollPane.getVerticalScrollBar()));
		scrollPane.setViewportBorder(null);
		final GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.insets = new Insets(0, 0, -3, 0);
		gbc_scrollPane.gridwidth = 2;
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
		final TableRowSorter<TableModel> trs = new TableRowSorter<TableModel>(model);
		trs.setSortsOnUpdates(true);
		table.setRowSorter(trs);
		table.setBorder(null);
		table.getTableHeader().setFont(font);
		table.getTableHeader().setReorderingAllowed(false);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					boolean selectionEmpty = table.getSelectionModel().isSelectionEmpty();
					btnConnect.setEnabled(!selectionEmpty);
					btnRefresh.setEnabled(!selectionEmpty);
					btnBuild.setEnabled(!selectionEmpty);
					if (selectionEmpty) {
						updatePreviewImage(null);
					}
					else {
						refresh(new RemoteSlave[] { (RemoteSlave) model.getValueAt(table.convertRowIndexToModel(table.getSelectionModel().getLeadSelectionIndex()), 0) });
					}
				}
			}
		});

		panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel.gridwidth = 2;
		gbc_panel.anchor = GridBagConstraints.NORTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 1;
		contentPane.add(panel, gbc_panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[] { 34, 208, 19, 36, 23, 0 };
		gbl_panel.rowHeights = new int[] { 25, 0 };
		gbl_panel.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_panel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		panel.setLayout(gbl_panel);

		btnConnect = new JButton("");
		GridBagConstraints gbc_btnConnect = new GridBagConstraints();
		gbc_btnConnect.anchor = GridBagConstraints.WEST;
		gbc_btnConnect.fill = GridBagConstraints.VERTICAL;
		gbc_btnConnect.gridx = 0;
		gbc_btnConnect.gridy = 0;
		panel.add(btnConnect, gbc_btnConnect);
		btnConnect.setEnabled(false);
		btnConnect.setIcon(new ImageIcon(this.getClass().getResource("/org/whired/nspex/master/resources/connect.png")));
		btnConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final RemoteSlave[] slaves = getSelectedSlaves();
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

		progressBar = new JProgressBar();
		GridBagConstraints gbc_progressBar = new GridBagConstraints();
		gbc_progressBar.anchor = GridBagConstraints.EAST;
		gbc_progressBar.gridx = 1;
		gbc_progressBar.gridy = 0;
		panel.add(progressBar, gbc_progressBar);

		final JButton btnDownload = new JButton("");
		GridBagConstraints gbc_btnDownload = new GridBagConstraints();
		gbc_btnDownload.anchor = GridBagConstraints.EAST;
		gbc_btnDownload.fill = GridBagConstraints.VERTICAL;
		gbc_btnDownload.gridx = 2;
		gbc_btnDownload.gridy = 0;
		panel.add(btnDownload, gbc_btnDownload);
		btnDownload.setToolTipText("Download slave list");
		btnDownload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				downloadSlaves();
			}
		});
		btnDownload.setIcon(new ImageIcon(this.getClass().getResource("/org/whired/nspex/master/resources/download.png")));

		btnBuild = new JButton("");
		GridBagConstraints gbc_btnBuild = new GridBagConstraints();
		gbc_btnBuild.anchor = GridBagConstraints.EAST;
		gbc_btnBuild.fill = GridBagConstraints.VERTICAL;
		gbc_btnBuild.gridx = 3;
		gbc_btnBuild.gridy = 0;
		panel.add(btnBuild, gbc_btnBuild);
		btnBuild.setEnabled(false);
		btnBuild.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final RemoteSlave[] slaves = getSelectedSlaves();
				if (slaves.length > 0) {
					rebuild(slaves);
				}
				else {
					Log.l.info("Select the slaves to rebuild");
				}
			}
		});
		btnBuild.setToolTipText("Rebuild slave");
		btnBuild.setIcon(new ImageIcon(this.getClass().getResource("/org/whired/nspex/master/resources/build.png")));

		btnRefresh = new JButton("");
		GridBagConstraints gbc_btnRefresh = new GridBagConstraints();
		gbc_btnRefresh.anchor = GridBagConstraints.EAST;
		gbc_btnRefresh.fill = GridBagConstraints.VERTICAL;
		gbc_btnRefresh.gridx = 4;
		gbc_btnRefresh.gridy = 0;
		panel.add(btnRefresh, gbc_btnRefresh);
		btnRefresh.setEnabled(false);
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
		btnRefresh.setToolTipText("Refresh information");
		btnRefresh.setIcon(new ImageIcon(this.getClass().getResource("/org/whired/nspex/master/resources/refresh.png")));

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
		FlowLayout flowLayout_1 = (FlowLayout) pnlPreview.getLayout();
		flowLayout_1.setVgap(0);
		pnlPreview.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent e) {
				if (previewImage != null) {
					final RemoteSlave[] slaves = getSelectedSlaves();
					if (slaves.length > 0) {
						connect(slaves);
					}
				}
			}
		});
		final GridBagConstraints gbc_pnlPreview = new GridBagConstraints();
		gbc_pnlPreview.insets = new Insets(2, 0, 2, 2);
		gbc_pnlPreview.fill = GridBagConstraints.BOTH;
		gbc_pnlPreview.gridx = 1;
		gbc_pnlPreview.gridy = 2;
		contentPane.add(pnlPreview, gbc_pnlPreview);
		scrollPane_1.getVerticalScrollBar().setUI(new MinimalScrollBar(scrollPane_1.getVerticalScrollBar()));
	}

	private void updateInformation(final Slave slv) {
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
							if (slaves.length == 1 && !slv.isOnline()) {
								updatePreviewImage(null);
							}
							model.fireTableRowsUpdated(i, i);
							return;
						}
					}
					model.addRow(new Object[] { slv, slv.getUser(), slv.getOS(), slv.getVersion(), slv.isOnline() ? "Online" : "Offline" });
					if (slaves.length == 1 && !slv.isOnline()) {
						updatePreviewImage(null);
					}
					model.fireTableRowsUpdated(model.getRowCount() - 1, model.getRowCount() - 1);
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
	private final void updatePreviewImage(final Image newImg) {
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
					slaves[i] = (RemoteSlave) model.getValueAt(table.convertRowIndexToModel(rows[i]), 0);
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
	public void setFile(final RemoteFile file) {
		Log.l.config("");
	}

	@Override
	public void addChildFiles(final char fs, final String parentPath, final RemoteFile[] childFiles) {
		Log.l.config("");
	}

	@Override
	public void disconnected(final Slave slave) {
		updateInformation(slave);
	}

	@Override
	public Dimension getThumbSize() {
		return pnlPreview.getPreferredSize();
	}

	@Override
	public void connected(final Slave slave) {
		updateInformation(slave);
	}

	@Override
	public void displayOutput(String output) {
		Log.l.info(output);
	}

	@Override
	public void downloadSlaves() {
		listener.downloadSlaves();
	}

}
