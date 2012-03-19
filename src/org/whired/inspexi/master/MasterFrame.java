package org.whired.inspexi.master;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

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
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final MasterFrame frame = new MasterFrame(new EventListener() {

						@Override
						public void connect(String ip) {
							System.out.println("Connecting to " + ip + "..");
						}

						@Override
						public void rebuild(String ip) {
							System.out.println("Requesting that " + ip + " rebuilds..");
						}

						@Override
						public void refresh() {
							System.out.println("Refreshing slaves");
						}

					});
					frame.setVisible(true);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public MasterFrame(EventListener listener) {
		this.listener = listener;

		setTitle("Inspexi");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 353);
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
				int row = table.getSelectedRow();
				if (row != -1) {
					connect((String) model.getValueAt(row, 0));
				}
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
				int row = table.getSelectedRow();
				if (row != -1) {
					rebuild((String) model.getValueAt(row, 0));
				}
			}
		});

		JButton btnTex = new JButton("");
		btnTex.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String ip;
				if ((ip = JOptionPane.showInputDialog(MasterFrame.this, "Enter IP:")) != null && ip.length() > 0) {
					model.addRow(new String[] { ip, "-", "-", "-", "Offline" });
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
				refresh();
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
		pane.setEditable(false);
		pane.setFont(font);

		scrollPane_1.setViewportView(pane);
		scrollPane_1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane_1.getVerticalScrollBar().setUI(new MinimalScrollBar(scrollPane_1.getVerticalScrollBar()));
	}

	public void addClient(String ip, String host, String os, String version, String status) {
		for (int i = 0; i < model.getRowCount(); i++) {
			if (model.getValueAt(i, 0).equals(ip)) {
				return;
			}
		}
		model.addRow(new Object[] { ip, host, os, version, status });
	}

	public void removeClient(String ip) {
		for (int i = 0; i < model.getRowCount(); i++) {
			if (model.getValueAt(i, 0).equals(ip)) {
				model.removeRow(i);
				break;
			}
		}
	}

	public void updateSlaveList(String ip, String host, String os, String version) {
		for (int i = 0; i < model.getRowCount(); i++) {
			if (model.getValueAt(i, 0).equals(ip)) {
				model.removeRow(i);
				model.insertRow(i, new String[] { ip, host, os, version, "Online" });
				return;
			}
		}
		model.addRow(new String[] { ip, host, os, version, "Online" });
	}

	public void setSlaveOffline(String ip) {
		for (int i = 0; i < model.getRowCount(); i++) {
			if (model.getValueAt(i, 0).equals(ip)) {
				String[] vals = new String[] { ip, (String) model.getValueAt(i, 1), (String) model.getValueAt(i, 2), (String) model.getValueAt(i, 3), "Offline" };
				model.removeRow(i);
				model.insertRow(i, vals);
				return;
			}
		}
		model.addRow(new String[] { ip, "-", "-", "-", "Offline" });
	}

	public void log(String message) {
		Document doc = pane.getStyledDocument();
		try {
			doc.insertString(doc.getLength(), message + "\n", null);
		}
		catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	public String[] getIps() {
		String[] ips = new String[model.getRowCount()];
		for (int i = 0; i < model.getRowCount(); i++) {
			ips[i] = (String) model.getValueAt(i, 0);
		}
		return ips;
	}

	@Override
	public void connect(String ip) {
		listener.connect(ip);
	}

	@Override
	public void rebuild(String ip) {
		listener.rebuild(ip);
	}

	@Override
	public void refresh() {
		listener.refresh();
	}

}
