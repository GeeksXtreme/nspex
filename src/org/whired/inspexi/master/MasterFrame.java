package org.whired.inspexi.master;

import java.awt.Color;
import java.awt.Cursor;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;

import org.whired.inspexi.tools.logging.Log;
import org.whired.inspexi.tools.logging.LogFormatter;

public class MasterFrame extends JFrame implements ControllerEventListener, ImageConsumer {

	private final JPanel contentPane;
	private final JTable table;
	private final Font font = new Font("SansSerif", Font.PLAIN, 9);
	private final JTextPane pane;
	private final ControllerEventListener listener;
	private final JPanel pnlPreview;
	private final DefaultTableModel model = new DefaultTableModel(new String[] { "IP", "Host", "OS", "Version", "Status" }, 0) {
		@Override
		public boolean isCellEditable(int rowIndex, int mColIndex) {
			return false;
		}
	};
	private Image previewImage;

	/**
	 * Create the frame.
	 */
	public MasterFrame(ControllerEventListener listener) {
		this.listener = listener;

		setTitle("Inspexi");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 700, 500);
		contentPane = new JPanel();
		contentPane.setBorder(null);
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 494, 80, 27, 27, 0 };
		gbl_contentPane.rowHeights = new int[] { 245, 1, 75, 0 };
		gbl_contentPane.columnWeights = new double[] { 1.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_contentPane.rowWeights = new double[] { 1.0, 0.0, 1.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getVerticalScrollBar().setUI(new MinimalScrollBar(scrollPane.getVerticalScrollBar()));
		scrollPane.setViewportBorder(null);
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.insets = new Insets(0, 0, -3, 0);
		gbc_scrollPane.gridwidth = 4;
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		contentPane.add(scrollPane, gbc_scrollPane);

		table = new JTable();

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
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
			public void valueChanged(ListSelectionEvent e) {
				previewImage = null;
				pnlPreview.repaint();
				if (e.getValueIsAdjusting() || table.getSelectionModel().isSelectionEmpty()) {
					return; // Don't refresh if the selection isn't complete or empty
				}
				refresh(new String[] { model.getValueAt(table.getSelectionModel().getLeadSelectionIndex(), 0).toString() });
			}
		});

		JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				connect(getSelectedIps());
			}
		});
		btnConnect.setToolTipText("Connect");
		btnConnect.setFont(font);
		GridBagConstraints gbc_btnConnect = new GridBagConstraints();
		gbc_btnConnect.anchor = GridBagConstraints.SOUTHWEST;
		gbc_btnConnect.gridx = 0;
		gbc_btnConnect.gridy = 1;
		contentPane.add(btnConnect, gbc_btnConnect);

		JButton btnRefresh = new JButton("");
		btnRefresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				refresh(getSelectedIps());
			}
		});

		JButton btnBuild = new JButton("");
		btnBuild.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rebuild(getSelectedIps());
			}
		});

		JButton btnAdd = new JButton("");
		btnAdd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String ip;
				if ((ip = JOptionPane.showInputDialog(MasterFrame.this, "Enter IP:")) != null && ip.length() > 0) {
					addSlaves(new String[] { ip });
				}
			}
		});
		btnAdd.setIcon(new ImageIcon(this.getClass().getResource("/org/whired/inspexi/master/resources/plus.gif")));
		GridBagConstraints gbc_btnAdd = new GridBagConstraints();
		gbc_btnAdd.anchor = GridBagConstraints.EAST;
		gbc_btnAdd.fill = GridBagConstraints.VERTICAL;
		gbc_btnAdd.gridx = 1;
		gbc_btnAdd.gridy = 1;
		contentPane.add(btnAdd, gbc_btnAdd);
		btnBuild.setToolTipText("Rebuild");
		btnBuild.setIcon(new ImageIcon(this.getClass().getResource("/org/whired/inspexi/master/resources/rebuild.gif")));
		GridBagConstraints gbc_btnBuild = new GridBagConstraints();
		gbc_btnBuild.fill = GridBagConstraints.VERTICAL;
		gbc_btnBuild.gridx = 2;
		gbc_btnBuild.gridy = 1;
		contentPane.add(btnBuild, gbc_btnBuild);
		btnRefresh.setToolTipText("Refresh list");
		btnRefresh.setIcon(new ImageIcon(this.getClass().getResource("/org/whired/inspexi/master/resources/refresh.png")));
		GridBagConstraints gbc_btnRefresh = new GridBagConstraints();
		gbc_btnRefresh.fill = GridBagConstraints.VERTICAL;
		gbc_btnRefresh.gridx = 3;
		gbc_btnRefresh.gridy = 1;
		contentPane.add(btnRefresh, gbc_btnRefresh);

		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
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
					updateTextArea(pane, formatter.format(record), true);
				}
			}

			@Override
			public void flush() {
			}

			@Override
			public void close() throws SecurityException {
			}
		});

		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				updateTextArea(pane, String.valueOf((char) b), false);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				updateTextArea(pane, new String(b, off, len), false);
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};
		PrintStream p = new PrintStream(out, true);
		System.setOut(p);
		System.setErr(p);

		scrollPane_1.setViewportView(pane);
		scrollPane_1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		pnlPreview = new JPanel() {
			@Override
			public void paint(Graphics g) {
				if (previewImage == null) {
					super.paint(g);
					return;
				}
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
				g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
				g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
				g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
				g2.drawImage(previewImage, 0, 0, this.getWidth(), this.getHeight(), 0, 0, previewImage.getWidth(this), previewImage.getHeight(this), this);
				g2.dispose();
				g.dispose();
			}
		};
		pnlPreview.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		pnlPreview.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				connect(getSelectedIps());
			}
		});
		GridBagConstraints gbc_pnlPreview = new GridBagConstraints();
		gbc_pnlPreview.insets = new Insets(2, 0, 2, 2);
		gbc_pnlPreview.gridwidth = 3;
		gbc_pnlPreview.fill = GridBagConstraints.BOTH;
		gbc_pnlPreview.gridx = 1;
		gbc_pnlPreview.gridy = 2;
		contentPane.add(pnlPreview, gbc_pnlPreview);
		scrollPane_1.getVerticalScrollBar().setUI(new MinimalScrollBar(scrollPane_1.getVerticalScrollBar()));
	}

	public void addSlaves(final String[] ips) {
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				for (String ip : ips) {
					for (int i = 0; i < model.getRowCount(); i++) {
						if (model.getValueAt(i, 0).equals(ip)) {
							break;
						}
					}
					model.addRow(new Object[] { ip, "-", "-", "-", "Offline" });
				}
			}
		});
		refresh(ips);
	}

	public void removeSlave(final String ip) {
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < model.getRowCount(); i++) {
					if (model.getValueAt(i, 0).equals(ip)) {
						model.removeRow(i);
						break;
					}
				}
			}
		});
	}

	public void updateSlaveList(final String ip, final String host, final String os, final String version) {
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < model.getRowCount(); i++) {
					if (model.getValueAt(i, 0).equals(ip)) {
						model.setValueAt(ip, i, 0);
						model.setValueAt(host, i, 1);
						model.setValueAt(os, i, 2);
						model.setValueAt(version, i, 3);
						model.setValueAt("Online", i, 4);
						return;
					}
				}
				model.addRow(new String[] { ip, host, os, version, "Online" });
			}
		});
	}

	public void setSlaveOffline(final String ip) {
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < model.getRowCount(); i++) {
					if (model.getValueAt(i, 0).equals(ip)) {
						model.setValueAt("Offline", i, 4);
						return;
					}
				}
				model.addRow(new String[] { ip, "-", "-", "-", "Offline" });
			}
		});
	}

	private void updateTextArea(final JTextPane pane, final String text, final boolean preformatted) {
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				Document doc = pane.getStyledDocument();
				try {
					doc.insertString(doc.getLength(), !text.contains(System.getProperty("line.separator")) && !preformatted ? "[" + LogFormatter.DATE_FORMAT.format(Calendar.getInstance().getTime()) + "] System: " + text : text, null);
					pane.setCaretPosition(doc.getLength());
				}
				catch (BadLocationException e) {
				}
			}
		});
	}

	private static void runOnEdt(Runnable run) {
		if (EventQueue.isDispatchThread()) {
			run.run();
		}
		else {
			try {
				EventQueue.invokeAndWait(run);
			}
			catch (Throwable t) {
			}
		}
	}

	private String[] getAllIps() {
		final String[] ips = new String[model.getRowCount()];
		runOnEdt(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < model.getRowCount(); i++) {
					ips[i] = (String) model.getValueAt(i, 0);
				}
			}
		});
		return ips;
	}

	public String[] getSelectedIps() {
		final int[] rows = table.getSelectedRows();
		final String[] ips = new String[rows.length];
		if (rows.length > 0) {
			runOnEdt(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < rows.length; i++) {
						ips[i] = (String) model.getValueAt(rows[i], 0);
					}
				}
			});
			return ips;
		}
		else {
			return getAllIps();
		}
	}

	@Override
	public void connect(String[] ips) {
		listener.connect(ips);
	}

	@Override
	public void rebuild(String[] ips) {
		listener.rebuild(ips);
	}

	@Override
	public void refresh(String[] ips) {
		listener.refresh(ips);
	}

	@Override
	public void imageProduced(Image image) {
		this.previewImage = image;
		pnlPreview.repaint();
	}

	@Override
	public void imageResized(int width, int height) {
		// TODO Auto-generated method stub

	}

}
