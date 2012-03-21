package org.whired.inspexi.master;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.whired.inspexi.tools.Slave;
import org.whired.inspexi.tools.logging.Log;

public class RemoteSlave extends Slave implements SessionListener {
	private final Socket socket;
	private DataInputStream dis;
	private DataOutputStream dos;
	private final SessionListener listener;
	private Image image;
	private final String ip;

	public RemoteSlave(String ip, int port, int intent, SessionListener listener) throws UnknownHostException, IOException {
		this.ip = ip;
		this.listener = listener;
		socket = new Socket();
		socket.connect(new InetSocketAddress(ip, port), 250);
		try {
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			dos.write(intent);
			if (intent == INTENT_CHECK || intent == INTENT_CONNECT) {
				setHost(dis.readUTF());
				setOS(dis.readUTF());
				setVersion(dis.readUTF());
				if (intent == INTENT_CHECK) {
					socket.close();
				}
				else {
					beginSession();
				}
			}
		}
		catch (IOException e) {
			try {
				socket.close();
			}
			catch (IOException ie) {
			}
		}
	}

	private void beginSession() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (!getVersion().equals(VERSION)) {
						Log.l.warning("Remote version differs! (" + VERSION + " vs " + getVersion() + ")");
					}
					final int width = dis.readShort();
					final int height = dis.readShort();
					final JPanel panel = new JPanel() {
						@Override
						public void paint(Graphics g) {
							if (image == null) {
								return;
							}
							Graphics2D g2 = (Graphics2D) g;
							g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
							g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
							g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
							g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
							g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
							g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
							g2.drawImage(image, 0, 0, this);
							g2.dispose();
							g.dispose();
						}
					};
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							final JFrame sframe = new JFrame("Inspexi - " + ip);
							sframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
							sframe.setAlwaysOnTop(true);
							panel.setPreferredSize(new Dimension(width, height));

							JScrollPane scrollPane = new JScrollPane();
							scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
							scrollPane.getVerticalScrollBar().setUI(new MinimalScrollBar(scrollPane.getVerticalScrollBar()));
							scrollPane.setViewportBorder(null);

							GroupLayout groupLayout = new GroupLayout(sframe.getContentPane());

							groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(groupLayout.createSequentialGroup().addComponent(panel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)).addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
							groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(groupLayout.createSequentialGroup().addComponent(panel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)));

							JConsole console = new JConsole();
							console.addCommandListener(new CommandListener() {
								@Override
								public void doCommand(String command) {
									try {
										dos.write(OP_DO_COMMAND);
										dos.writeUTF(command);
									}
									catch (Throwable t) {
										t.printStackTrace();
										try {
											socket.close();
										}
										catch (IOException e) {
										}
									}
								}
							});

							scrollPane.setViewportView(console);
							sframe.getContentPane().setLayout(groupLayout);

							sframe.pack();
							sframe.setMinimumSize(sframe.getSize());
							sframe.setLocationRelativeTo(null);
							sframe.setVisible(true);

							sframe.addWindowListener(new WindowAdapter() {
								@Override
								public void windowClosing(WindowEvent e) {
									try {
										socket.close();
									}
									catch (IOException e1) {
										e1.printStackTrace();
									}
									super.windowClosing(e);
								}
							});
						}
					});
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								int op;
								while ((op = dis.read()) != -1) {
									switch (op) {
									case 0:
										int imgLen = dis.readInt();
										byte[] buf = new byte[imgLen];
										int read = 0;
										while ((read += dis.read(buf, read, imgLen - read)) != imgLen) {
											;
										}
										image = ImageIO.read(new ByteArrayInputStream(buf));
										panel.repaint();
									break;
									}
								}
								if (socket != null) {
									try {
										socket.close();
									}
									catch (IOException e) {
									}
								}
								throw new IOException("End of stream");
							}
							catch (Throwable t) {
								if (socket != null) {
									try {
										socket.close();
									}
									catch (IOException e) {
									}
								}
								sessionEnded(t.toString());
							}
						}
					}, "SlavePacketReceiver").start();
				}
				catch (Throwable t) {
					try {
						socket.close();
					}
					catch (IOException e) {
					}
				}
			}
		}, "SlaveConnecter").start();

	}

	@Override
	public void sessionEnded(String reason) {
		listener.sessionEnded(reason);
	}
}
