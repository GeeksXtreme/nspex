package org.whired.nspex.blackbox;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

public class ConnectDialog extends JDialog {
	private final JPanel contentPanel = new JPanel();
	private final JPasswordField txtPass;
	private final JTextField txtUsername;
	private final JTextField txtIp;
	private boolean canceled = true;
	private String ip;
	private char[] password;
	private String username;

	/**
	 * Create the dialog.
	 */
	public ConnectDialog(Component par) {
		setUndecorated(true);
		setModal(true);
		setResizable(false);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setSize(230, 102);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPanel.setOpaque(false);
		getContentPane().add(contentPanel, BorderLayout.CENTER);

		contentPanel.setLayout(new GridLayout(3, 1, 2, 4));
		txtIp = new JTextField();
		txtIp.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				txtIp.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (txtIp.getText().length() == 0) {
					txtIp.setText("IP");
				}
			}
		});
		txtIp.setText("IP");
		txtIp.setOpaque(false);
		Border bor = BorderFactory.createLineBorder(Color.GRAY);
		txtIp.setBorder(bor);
		txtIp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				validateAndClose();
			}

		});
		contentPanel.add(txtIp);
		txtIp.setColumns(10);
		txtUsername = new JTextField();
		txtUsername.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				txtUsername.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (txtUsername.getText().length() == 0) {
					txtUsername.setText("Username");
				}
			}
		});
		txtUsername.setText("Username");
		txtUsername.setOpaque(false);
		txtUsername.setBorder(bor);
		txtUsername.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				validateAndClose();
			}

		});
		contentPanel.add(txtUsername);
		txtUsername.setColumns(10);
		txtPass = new JPasswordField();
		txtPass.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				txtPass.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (txtPass.getPassword().length == 0) {
					txtPass.setText("password");
				}
			}
		});
		txtPass.setText("password");
		txtPass.setOpaque(false);
		txtPass.setBorder(bor);
		txtPass.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				validateAndClose();
			}

		});
		contentPanel.add(txtPass);
		txtPass.setColumns(10);
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttonPane.setOpaque(false);
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		{
			JLabel btnOk = new JLabel("OK");
			btnOk.setBorder(bor);
			btnOk.setOpaque(false);
			btnOk.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					validateAndClose();
				}
			});
			buttonPane.add(btnOk);
		}
		{
			JLabel btnCancel = new JLabel("Cancel");
			btnCancel.setBorder(bor);
			btnCancel.setOpaque(false);
			btnCancel.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					setVisible(false);
				}
			});
			buttonPane.add(btnCancel);
		}
		setLocationRelativeTo(par);
	}

	public void validateAndClose() {
		try {
			username = txtUsername.getText();
			if (username.length() == 0) {
				throw new Throwable("username");
			}
			ip = txtIp.getText();
			if (ip.length() == 0) {
				throw new Throwable("ip");
			}
			password = txtPass.getPassword();
			if (password.length == 0) {
				throw new Throwable("password");
			}
			canceled = false;
			setVisible(false);
		}
		catch (Throwable t) {
			JOptionPane.showMessageDialog(this, "The " + t.getMessage() + " is invalid", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public boolean isCancelled() {
		return canceled;
	}

	public String getIp() {
		return ip;
	}

	public String getUsername() {
		return username;
	}

	public char[] getPassword() {
		return password;
	}
}
