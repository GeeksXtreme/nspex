package org.whired.inspexi.blackbox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;

import org.whired.inspexi.blackbox.sql.SQLiteDatabase;
import org.whired.inspexi.tools.NetTask;
import org.whired.inspexi.tools.NetTaskQueue;
import org.whired.inspexi.tools.ReactServer;
import org.whired.inspexi.tools.SessionListener;

/**
 * The authentication server
 * @author Whired
 */
public class AuthenticationServer {
	private final static long IP_CHANGE_TIMEOUT = 864000000; // 10 days
	private final DecimalFormat decFormat = new DecimalFormat("#.#");
	/** The queue that handles work for this server */
	private final NetTaskQueue queue = new NetTaskQueue(new SessionListener() {
		@Override
		public void sessionEnded(String reason, Throwable t) {
			t.printStackTrace();
		}
	});

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
	public AuthenticationServer(final int port) throws IOException, GeneralSecurityException, SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		// Set up rsa session
		System.out.print("Generating RSA keys..");
		final RSASession rsa = new RSASession();

		System.out.println("Initializing database..");
		final SQLiteDatabase database = new SQLiteDatabase("", "ispx_db");

		System.out.print("Starting server..");
		final ReactServer server = new ReactServer(new ServerSocket(port), queue) {
			@Override
			public NetTask getOnConnectTask(final Socket sock) {
				return new NetTask("auth_connect", sock) {
					@Override
					public void run(final DataInputStream dis, final DataOutputStream dos) throws IOException, GeneralSecurityException {
						// First things first, these guys need to do their business QUICK, we're not going to wait
						sock.setSoTimeout(2000);

						String remoteIp = ((InetSocketAddress) sock.getRemoteSocketAddress()).getAddress().getHostAddress();

						// Exchange keys with the client
						rsa.exchangeKeys(dis, dos);

						// Set up a secure input stream
						RSAInputStream ris = new RSAInputStream(dis, rsa);

						// Get the client's details
						MessageDigest md5 = MessageDigest.getInstance("MD5");
						String userid;
						String pass;

						byte[] array = md5.digest(ris.readUTF().getBytes());
						StringBuffer sb = new StringBuffer();
						for (int i = 0; i < array.length; ++i) {
							sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
						}
						userid = sb.toString();

						md5.reset();
						sb = new StringBuffer();
						array = md5.digest(ris.readUTF().getBytes());
						for (int i = 0; i < array.length; ++i) {
							sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
						}
						pass = sb.toString();

						System.out.println("id: " + userid + " pass: " + pass);

						// Now we can use the details
						try {
							ResultSet rs = database.executeQuery("SELECT user_password,user_ip,last_ip_change FROM user WHERE user_id = '" + userid + "'");
							if (rs.next() && rs.getString(1).equals(pass)) {
								// Password is right, make sure this user hasn't expired

								// Not expired, let's compare the ip
								if (!isSameIsp(rs.getString(2), remoteIp)) {
									System.out.println("IP has changed too drastically");
									long lastChange = rs.getLong(3);
									System.out.println(lastChange);
									// Check if client is allowed a mulligan
									if (System.currentTimeMillis() - IP_CHANGE_TIMEOUT > lastChange) {
										// Warn client that he can change, but at a cost
										// Set timeout to ~20s
										System.out.println("IP is allowed to change");
										System.out.println(System.currentTimeMillis());

										// If user desn't elect to change
										//return;

									}
									else {
										// Hasn't been long enough
										// Let's give them a rough idea so they don't spam us
										long remaining = lastChange - System.currentTimeMillis() + IP_CHANGE_TIMEOUT;
										System.out.println("IP changed too recently, try again in " + decFormat.format((remaining / 1000d / 60d / 60d / 24d)) + " days");
										// Wait for some result code to send
										sock.close();
										return;
									}
								}
								// Okay everything is sorted, let's get this guy his slaves
								rs = database.executeQuery("SELECT user_ip FROM slave INNER JOIN owned_slave ON slave.slave_id = owned_slave.slave_id WHERE user_id = '" + userid + "'");
								while (rs.next()) {
									System.out.println(userid + " owns slave " + rs.getString(1));
								}
							}
							else {
								// No matches found/password mismatch
								System.out.println("Invalid login");
							}
						}
						catch (SQLException e) {
							e.printStackTrace();
						}

						// Hang
						dos.close();
						dis.read();
					}
				};
			}
		};
		System.out.println("started.");
		server.startAccepting();
	}

	public static boolean isSameIsp(String ip1, String ip2) {
		return get512(ip1).equals(get512(ip2));
	}

	private static String get512(String ip) {
		String[] parts = ip.split("\\.");
		return parts[0] + "." + parts[1];
	}

	public static void main(final String[] args) throws IOException, GeneralSecurityException, SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		new AuthenticationServer(43597);
	}
}
