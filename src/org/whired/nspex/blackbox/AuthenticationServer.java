package org.whired.nspex.blackbox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.security.GeneralSecurityException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.whired.nspex.blackbox.sql.SQLiteDatabase;
import org.whired.nspex.net.BufferUtil;
import org.whired.nspex.net.Communicable;
import org.whired.nspex.net.ExpandableByteBuffer;
import org.whired.nspex.net.NioCommunicable;
import org.whired.nspex.net.NioServer;
import org.whired.nspex.tools.RSAKeySet;
import org.whired.nspex.tools.RSASession;
import org.whired.nspex.tools.logging.Log;

/**
 * The authentication server
 * @author Whired
 */
public class AuthenticationServer {
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

		// Set up session manager
		final SessionManager sessionManager = new SessionManager();

		Log.l.info("Initializing database..");
		final SQLiteDatabase database = new SQLiteDatabase("", "ispx_db");

		server = new NioServer(port) {

			@Override
			protected Communicable getCommunicable(final SelectionKey key) {
				return new NioCommunicable(key, server) {

					private String userid;
					private String sessionId;
					private RSASession rsaSess;
					{
						// These guys need to be quick
						setReadTimeout(TimeUnit.SECONDS.toMillis(2));
					}

					@Override
					public void handle(final int id, final ByteBuffer payload) {
						if (rsaSess == null && id != Opcodes.RSA_KEY_REQUEST) {
							// We don't care why, but we didn't get the keyspec
							remoteLog(Level.SEVERE, "RSA keyspec expected");
							disconnect();
						}
						switch (id) {
							case Opcodes.RSA_KEY_REQUEST:
								try {
									// Get remote public keyspec
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
											// Send the slave list
											sendSlaves(true);
										}
										else {
											// No matches found/password mismatch
											Log.l.info("[" + this + "] Invalid login");
											remoteLog(Level.SEVERE, "Invalid login");
											disconnect();
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
							case Opcodes.LOGIN_WITH_SESSION:
								try {
									ByteBuffer decPay = rsaSess.decrypt(payload);
									String v = BufferUtil.getJTF(decPay);
									Log.l.info("[" + this + "] Logging in with id: " + v);
									if (sessionManager.sessionValid(this.toString(), v)) {
										sendSlaves(false);
									}
									else {
										Log.l.warning("[" + this + "] Session invalid!");
										send(Opcodes.INVALIDATE_SESSION);
										disconnect();
									}
								}
								catch (Throwable e1) {
									e1.printStackTrace();
								}
							break;
						}
					}

					/**
					 * Send the remote their slaves
					 * @throws SQLException when a database error occurs
					 */
					private void sendSlaves(boolean sendSessionId) throws SQLException {
						sessionId = sessionManager.getSessionId(this.toString());

						// Send remote the session id if they need it
						if (sendSessionId) {
							try {
								send(Opcodes.LOGIN_WITH_SESSION, rsaSess.encrypt(new ExpandableByteBuffer().putJTF(sessionId).asByteBuffer()));
							}
							catch (GeneralSecurityException e) {
								Log.l.warning("[" + this + "] Unable to send session id");
							}
						}

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
						remoteLog(Level.INFO, "Logged in successfully");
						disconnect();
					}

					@Override
					public void handle(final int id) {

					}

					@Override
					protected void disconnected() {
						// We don't exactly care at this point
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