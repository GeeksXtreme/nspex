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
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.whired.inspexi.tools.Slave;

public class RemoteSlave extends Slave implements SessionListener {
	private final int intent;
	private final Socket socket;
	private final DataInputStream dis;
	private final DataOutputStream dos;
	private final SessionListener listener;
	private Image image;

	public RemoteSlave(String ip, int port, int intent, SessionListener listener) throws UnknownHostException, IOException {
		this.intent = intent;
		this.listener = listener;
		socket = new Socket();
		socket.connect(new InetSocketAddress(ip, port), 500);
		dis = new DataInputStream(socket.getInputStream());
		dos = new DataOutputStream(socket.getOutputStream());
	}

	public void beginSession() throws IOException {
		dos.write(intent);
		if (intent == INTENT_REBUILD) {
			return;
		}
		else {
			setHost(dis.readUTF());
			setOS(dis.readUTF());
			setVersion(dis.readUTF());
		}
		if (intent == INTENT_CONNECT) {
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
					final JFrame sframe = new JFrame("Inspexi");
					sframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					sframe.setAlwaysOnTop(true);
					panel.setPreferredSize(new Dimension(300, 300));
					sframe.getContentPane().add(panel);
					sframe.pack();
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
							// imageReceived(image);
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
						// JOptionPane.showMessageDialog(frame, "No connection", "Inspexi", JOptionPane.WARNING_MESSAGE);
					}
				}
			}).start();
		}
		else {
			socket.close();
		}
	}

	@Override
	public void sessionEnded(String reason) {
		listener.sessionEnded(reason);
	}
}
