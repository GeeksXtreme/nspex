package org.whired.inspexi.net;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.Hashtable;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Master {
	private BufferedImage image = null;
	private JPanel panel;
	private JFrame frame;
	private Socket socket = null;

	public Master() throws InterruptedException, InvocationTargetException {
		final int PORT = 43596;
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				frame = new JFrame("Inspexi");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setAlwaysOnTop(true);
				panel = new JPanel() {
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
				panel.setPreferredSize(new Dimension(300, 300));
				frame.getContentPane().add(panel);
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});

		new Thread(new Runnable() {
			@Override
			public void run() {
				// Connect to server
				System.out.print("[Client] Connecting...");
				String ip;
				while ((ip = JOptionPane.showInputDialog(frame, "Enter new IP:", "Disconnected", JOptionPane.QUESTION_MESSAGE)) != null) {
					try {
						socket = new Socket(ip, PORT);
						frame.setTitle("Inspexi - " + ip);
						System.out.println("success.");
						frame.setVisible(true);
						// Set up streams
						final DataInputStream dis = new DataInputStream(socket.getInputStream());
						final DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
						final short width = dis.readShort();
						final short height = dis.readShort();
						System.out.println(dis.readUTF());
						panel.addMouseListener(new MouseAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								try {
									dos.write(0);
								}
								catch (IOException e1) {
									try {
										socket.close();
									}
									catch (IOException e2) {
									}
									e1.printStackTrace();
								}
							}
						});
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								panel.setPreferredSize(new Dimension(width, height));
								frame.pack();
								frame.setLocationRelativeTo(null);
							}
						});

						final byte[] clientPix = new byte[width * height];
						ColorModel cm = new DirectColorModel(8, 0xE0, 0x1C, 0x3);
						DataBufferByte dataBuffer = new DataBufferByte(clientPix, clientPix.length);
						image = new BufferedImage(cm, Raster.createWritableRaster(cm.createCompatibleSampleModel(width, height), dataBuffer, null), false, new Hashtable<Object, Object>());
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
								System.arraycopy(buf, 0, clientPix, 0, clientPix.length);
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
						t.printStackTrace();
						JOptionPane.showMessageDialog(frame, "No connection", "Inspexi", JOptionPane.WARNING_MESSAGE);
					}
				}
			}
		}).start();
	}

	public static void main(String[] args) throws InterruptedException, InvocationTargetException {
		new Master();
	}
}
