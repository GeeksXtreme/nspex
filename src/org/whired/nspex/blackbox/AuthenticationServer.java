package org.whired.nspex.blackbox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.logging.Level;

import org.whired.nspex.blackbox.sql.SQLiteDatabase;
import org.whired.nspex.net.BufferUtil;
import org.whired.nspex.net.Communicable;
import org.whired.nspex.net.NioCommunicable;
import org.whired.nspex.net.NioServer;
import org.whired.nspex.tools.logging.Log;

/**
 * The authentication server
 * @author Whired
 */
public class AuthenticationServer {
	private final static long IP_CHANGE_TIMEOUT = 259200000; // 3 days
	private final DecimalFormat decFormat = new DecimalFormat("#.#");
	private NioServer server;

	/**
	 * Starts an authentication server on the specified port
	 * @param port the port to listen for connections on
	 * @throws IOException
	 * @throws GeneralSecurityException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public AuthenticationServer(final int port) throws GeneralSecurityException, SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
		Log.l.setLevel(Level.ALL);
		// Set up rsa session
		Log.l.info("Generating RSA keys..");
		final RSAKeySet rsaKeys = new RSAKeySet();

		Log.l.info("Initializing database..");
		final SQLiteDatabase database = new SQLiteDatabase("", "ispx_db");

		server = new NioServer(port) {

			@Override
			protected Communicable getCommunicable(final SelectionKey key) {
				return new NioCommunicable(key, server) {
					private RSASession rsaSess;
					{
						// These guys need to be quick
						setReadTimeout(2000);
					}

					@Override
					public void handle(final int id, final ByteBuffer payload) {
						if (rsaSess == null && id != 0) {
							// We don't care why, but we aren't getting the key spec
							// TODO send error code?
							disconnect();
						}
						switch (id) {
							case Opcodes.RSA_KEY_REQUEST:
								try {
									// Get remote public key spec
									rsaSess = new RSASession(rsaKeys, payload);

									// Send local public key
									send(0, rsaKeys.getPublicKeySpec());
								}
								catch (final Throwable t) {
									t.printStackTrace();
									disconnect();
								}
							break;
							case Opcodes.LOGIN:

								try {
									final ByteBuffer decPay = rsaSess.decrypt(payload);

									// Get client details
									final MessageDigest md5 = MessageDigest.getInstance("MD5");
									String userid;
									String pass;

									byte[] array = md5.digest((BufferUtil.getJTF(decPay) + "dIliHw308f@(,3s09fu~").getBytes("UTF-8"));
									StringBuffer sb = new StringBuffer();
									for (int i = 0; i < array.length; ++i) {
										sb.append(Integer.toHexString(array[i] & 0xFF | 0x100).substring(1, 3));
									}
									userid = sb.toString();

									md5.reset();
									sb = new StringBuffer();
									array = md5.digest((BufferUtil.getJTF(decPay) + "do%c.a19*'XmwUlm)~xr").getBytes("UTF-8"));
									for (int i = 0; i < array.length; ++i) {
										sb.append(Integer.toHexString(array[i] & 0xFF | 0x100).substring(1, 3));
									}
									pass = sb.toString();
									Log.l.config("[" + this + "] userid: " + userid + " userpass: " + pass);

									// Okay, so we have the basics..
									// Now we can use the details
									try {
										ResultSet rs = database.executeQuery("SELECT user_password,user_ip,last_ip_change FROM user WHERE user_id = '" + userid + "'");
										if (rs.next() && rs.getString(1).equals(pass)) {
											// Password is right, let's compare the ip
											final String lastHost = rs.getString(2);
											if (!isSameIsp(lastHost, this.toString())) {
												final long lastChange = rs.getLong(3);
												Log.l.config("IP has changed too drastically (" + this + "!=" + lastHost + ") lastChange=" + lastChange);
												// Check if client is allowed a mulligan
												if (System.currentTimeMillis() - IP_CHANGE_TIMEOUT > lastChange) {
													// Warn client that he can change, but at a cost
													// Set timeout to ~20s
													System.out.println("IP is allowed to change");
													System.out.println(System.currentTimeMillis());

													// If user desn't elect to change
													// discoonect();
													// return;

												}
												else {
													// Hasn't been long enough
													// Let's give them a rough idea so they don't spam us
													final long remaining = lastChange - System.currentTimeMillis() + IP_CHANGE_TIMEOUT;
													System.out.println("IP changed too recently, try again in " + decFormat.format(remaining / 1000d / 60d / 60d / 24d) + " days");
													// Send this message

													// Bounce them
													disconnect();
													return;
												}
											}
											// Everything is sorted, let's get this guy his slaves
											rs = database.executeQuery("SELECT slave_ip FROM slave INNER JOIN owned_slave ON slave.slave_id = owned_slave.slave_id WHERE user_id = '" + userid + "'");
											while (rs.next()) {
												Log.l.config(userid + " owns slave " + rs.getString(1));
											}
											rs.close();
										}
										else {
											// No matches found/password mismatch
											Log.l.info("[" + this + "]: Invalid login");
											// Send response code
											disconnect();
											return;
										}
										rs.close();
									}
									catch (final SQLException e) {
										e.printStackTrace();
									}
								}
								catch (final Exception e) {
									e.printStackTrace();
									disconnect();
								}
							break;
						}
					}

					@Override
					public void handle(final int id) {

					}

					@Override
					protected void disconnected() {
						// We don't really care
						Log.l.fine("[" + this + "] disconnected");
					}

					@Override
					public void log(Level level, String message) {
						// TODO Auto-generated method stub

					}

				};
			}
		};
		server.startListening();
	}

	private static boolean isSameIsp(final String ip1, final String ip2) {
		// TODO ..this has some obvious flaws..
		return get512(ip1).equals(get512(ip2));
	}

	private static String get512(final String ip) {
		if (ip.contains(".")) {
			final String[] parts = ip.split("\\.");
			return parts[0] + "." + parts[1];
		}
		else {
			return ip;
		}
	}

	public static void main(final String[] args) throws Throwable {
		new AuthenticationServer(43597);
	}
}