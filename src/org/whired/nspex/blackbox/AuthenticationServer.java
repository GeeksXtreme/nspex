package org.whired.nspex.blackbox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.security.GeneralSecurityException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.whired.nspex.blackbox.sql.SQLiteDatabase;
import org.whired.nspex.net.BufferUtil;
import org.whired.nspex.net.Communicable;
import org.whired.nspex.net.ExpandableByteBuffer;
import org.whired.nspex.net.NioCommunicable;
import org.whired.nspex.net.NioServer;
import org.whired.nspex.tools.IpUtil;
import org.whired.nspex.tools.RSAKeySet;
import org.whired.nspex.tools.RSASession;
import org.whired.nspex.tools.logging.Log;

/**
 * The authentication server
 * @author Whired
 */
public class AuthenticationServer {
	private final static long IP_CHANGE_TIMEOUT = 259200000; // 3 days
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

		// Set up RSA session
		final RSAKeySet rsaKeys = RSAKeySet.generateKeys(1024);

		Log.l.info("Initializing database..");
		final SQLiteDatabase database = new SQLiteDatabase("", "ispx_db");

		server = new NioServer(port) {

			@Override
			protected Communicable getCommunicable(final SelectionKey key) {
				return new NioCommunicable(key, server) {

					private String userid;

					private RSASession rsaSess;
					{
						// These guys need to be quick
						setReadTimeout(2000);
					}

					@Override
					public void handle(final int id, final ByteBuffer payload) {
						if (rsaSess == null && id != Opcodes.RSA_KEY_REQUEST) {
							// We don't care why, but we aren't getting the keyspec
							remoteLog(Level.SEVERE, "RSA keyspec expected");
							disconnect();
						}
						switch (id) {
							case Opcodes.RSA_KEY_REQUEST:
								try {
									// Get remote public key spec
									rsaSess = new RSASession(rsaKeys, payload);

									// Send local public key
									send(Opcodes.RSA_KEY_REQUEST, rsaKeys.getPublicKeySpec());
								}
								catch (final Throwable t) {
									t.printStackTrace();
									disconnect();
								}
							break;
							case Opcodes.LOGIN:

								try {
									final ByteBuffer decPay = rsaSess.decrypt(payload);

									// Get client's details
									String pass;

									// Bake the cake
									userid = org.apache.commons.codec.digest.DigestUtils.sha512Hex("do%c.a19*'XmwUlm)~xr" + BufferUtil.getJTF(decPay) + "dIliHw308f@(,3s09fu~");
									pass = org.apache.commons.codec.digest.DigestUtils.sha512Hex("dIliHw308f@(,3s09fu~" + BufferUtil.getJTF(decPay) + "do%c.a19*'XmwUlm)~xr");

									Log.l.config("[" + this + "] userid: " + userid + " userpass: " + pass);

									// Now we can use the details
									try {
										ResultSet rs = database.executeQuery("SELECT user_password,user_ip,last_ip_change FROM user WHERE user_id = '" + userid + "'");
										if (rs.next() && rs.getString(1).equals(pass)) {

											// Password is right, grab the ISP from WHOIS db
											String lastHost = rs.getString(2);

											Log.l.config("[" + this + "] last host: " + lastHost);
											WhoisInfo wii = WhoisInfo.fromARINJSON(WhoisInfoDownloader.getArinWhoisJson(lastHost));
											Log.l.info("wii: " + wii.getName() + " start: " + wii.getStartAddress() + " end: " + wii.getEndAddress() + " +/-: " + (wii.getEndAddress() - wii.getStartAddress()));

											if (!wii.withinRange(IpUtil.ip4ToInt(this.toString()))) {

												// ISP has changed
												long lastChange = rs.getLong(3);

												Log.l.config("ISP region has changed. lastChange=" + lastChange);

												// Check if client is allowed a mulligan
												if (System.currentTimeMillis() - IP_CHANGE_TIMEOUT > lastChange) {

													// Warn client that he can change, but at a cost
													send(Opcodes.CONFIRM_ISP_CHANGE, ByteBuffer.allocate(8).putLong(IP_CHANGE_TIMEOUT));

													// Set timeout to 20s while we wait for a response
													setReadTimeout(20000);
													return;
												}
												else {
													// Hasn't been long enough
													// Let's give them a rough idea so they don't spam us (Y U NO WORK??)
													final long remaining = lastChange - System.currentTimeMillis() + IP_CHANGE_TIMEOUT;
													remoteLog(Level.SEVERE, String.format("ISP region changed too recently, try again in %.1f days", remaining / 1000d / 60d / 60d / 24d));

													// Bounce them
													disconnect();
													return;
												}

											}

											// Everything is sorted, let's get this guy his slaves
											sendSlaves();
										}
										else {
											// No matches found/password mismatch
											Log.l.info("[" + this + "] Invalid login");
											remoteLog(Level.SEVERE, "Invalid login");
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
							case Opcodes.CONFIRM_ISP_CHANGE:
								boolean allow = payload.get() == 1;
								if (allow) {
									// Update database
									try {
										Log.l.info("Updating IP: " + this);
										database.executeStatement("UPDATE user SET last_ip_change='" + System.currentTimeMillis() + "', user_ip='" + this + "' WHERE user_id='" + userid + "'");
									}
									catch (SQLException e) {
										Log.l.log(Level.SEVERE, "Error while updating user info: ", e);
										// Sucks for them, but we can't risk it
										disconnect();
									}
									try {
										sendSlaves();
									}
									catch (SQLException e) {
										// Doesn't really affect us
										Log.l.log(Level.WARNING, "Error while fetching slaves: ", e);
									}
								}
								else {
									// Cya!
									remoteLog(Level.INFO, "Sorry for any inconvenience");
									disconnect();
								}
							break;
						}
					}

					private void sendSlaves() throws SQLException {
						ResultSet rs = database.executeQuery("SELECT slave_ip FROM slave INNER JOIN owned_slave ON slave.slave_id = owned_slave.slave_id WHERE user_id = '" + userid + "'");

						int ct = 0;

						ExpandableByteBuffer buf = new ExpandableByteBuffer();

						while (rs.next()) {
							ct++;
							buf.putJTF(rs.getString(1));
						}
						rs.close();

						// Add count to front (SQLite supports forward_only)
						ByteBuffer sbuf = buf.asByteBuffer();

						ByteBuffer fbuf = ByteBuffer.allocate(4 + sbuf.capacity());
						sbuf.flip();
						fbuf.putInt(ct).put(sbuf);

						send(Opcodes.SLAVES_RECEIVED, fbuf);
						Log.l.info("[" + this + "] Logged in successfully");
						disconnect();
					}

					@Override
					public void handle(final int id) {

					}

					@Override
					protected void disconnected() {
						// We don't REALLY care..
						Log.l.fine("[" + this + "] disconnected");
					}
				};
			}
		};
		server.startListening();
	}

	public static void main(final String[] args) throws Throwable {
		Log.l.setLevel(Level.ALL);
		new AuthenticationServer(43597);
	}
}