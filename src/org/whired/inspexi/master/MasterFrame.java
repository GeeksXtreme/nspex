package org.whired.inspexi.master;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;

import org.whired.inspexi.tools.logging.Log;
import org.whired.inspexi.tools.logging.LogFormatter;

public class MasterFrame extends JFrame implements EventListener {

	private final JPanel contentPane;
	private final JTable table;
	private final Font font = new Font("SansSerif", Font.PLAIN, 9);
	private final JTextPane pane;
	private final EventListener listener;
	private final DefaultTableModel model = new DefaultTableModel(new String[] { "IP", "Host", "OS", "Version", "Status" }, 0) {
		@Override
		public boolean isCellEditable(int rowIndex, int mColIndex) {
			return false;
		}
	};

	/**
	 * Create the frame.
	 */
	public MasterFrame(EventListener listener) {
		this.listener = listener;

		setTitle("Inspexi");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 700, 500);
		contentPane = new JPanel();
		contentPane.setBorder(null);
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 359, 0, 0, 0, 0 };
		gbl_contentPane.rowHeights = new int[] { 245, 1, 75, 0 };
		gbl_contentPane.columnWeights = new double[] { 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
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
					table.clearSelection();
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

		JButton btnLoooong = new JButton("");
		btnLoooong.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rebuild(getSelectedIps());
			}
		});

		JButton btnTex = new JButton("");
		btnTex.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String ip;
				if ((ip = JOptionPane.showInputDialog(MasterFrame.this, "Enter IP:")) != null && ip.length() > 0) {
					addClients(new String[] { ip });
				}
			}
		});
		btnTex.setIcon(new ImageIcon(MasterFrame.class.getResource("/org/whired/inspexi/master/resources/plus.gif")));
		GridBagConstraints gbc_btnTex = new GridBagConstraints();
		gbc_btnTex.fill = GridBagConstraints.VERTICAL;
		gbc_btnTex.gridx = 1;
		gbc_btnTex.gridy = 1;
		contentPane.add(btnTex, gbc_btnTex);
		btnLoooong.setToolTipText("Rebuild");
		btnLoooong.setIcon(new ImageIcon(MasterFrame.class.getResource("/org/whired/inspexi/master/resources/rebuild.gif")));
		GridBagConstraints gbc_btnLoooong = new GridBagConstraints();
		gbc_btnLoooong.fill = GridBagConstraints.VERTICAL;
		gbc_btnLoooong.gridx = 2;
		gbc_btnLoooong.gridy = 1;
		contentPane.add(btnLoooong, gbc_btnLoooong);

		JButton btnRefresh = new JButton("");
		btnRefresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				refresh(getSelectedIps());
			}
		});
		btnRefresh.setToolTipText("Refresh list");
		btnRefresh.setIcon(new ImageIcon(MasterFrame.class.getResource("/org/whired/inspexi/master/resources/refresh.png")));
		GridBagConstraints gbc_btnRefresh = new GridBagConstraints();
		gbc_btnRefresh.fill = GridBagConstraints.VERTICAL;
		gbc_btnRefresh.gridx = 3;
		gbc_btnRefresh.gridy = 1;
		contentPane.add(btnRefresh, gbc_btnRefresh);

		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.gridwidth = 4;
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 2;
		contentPane.add(scrollPane_1, gbc_scrollPane_1);

		pane = new JTextPane();
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
		scrollPane_1.getVerticalScrollBar().setUI(new MinimalScrollBar(scrollPane_1.getVerticalScrollBar()));
	}

	public void addClients(final String[] ips) {
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

	public void removeClient(final String ip) {
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

}
