package org.whired.inspexi.blackbox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.whired.inspexi.blackbox.sql.Database;
import org.whired.inspexi.tools.NetTask;
import org.whired.inspexi.tools.NetTaskQueue;
import org.whired.inspexi.tools.ReactServer;
import org.whired.inspexi.tools.SessionListener;

/**
 * The authentication server
 * @author Whired
 */
public class AuthenticationServer {
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
		final Database database = new Database("", "ipdb.sqlite");

		System.out.print("Starting server..");
		final ReactServer server = new ReactServer(new ServerSocket(port), queue) {
			@Override
			public NetTask getOnConnectTask(final Socket sock) {
				return new NetTask("auth_connect", sock) {
					@Override
					public void run(final DataInputStream dis, final DataOutputStream dos) throws IOException, GeneralSecurityException {
						// First things first, these guys need to do their business QUICK
						sock.setSoTimeout(2000);

						String remoteIp = ((InetSocketAddress) sock.getRemoteSocketAddress()).getAddress().getHostAddress();
						try {
							ResultSet rs = database.executeQuery("SELECT data FROM ip_group_city WHERE ip_start < '" + ipToLong(remoteIp) + "' ORDER BY ip_start DESC LIMIT 1");
							while (rs.next()) {
								System.out.println(rs.getString(1));
							}
						}
						catch (SQLException e) {
							e.printStackTrace();
						}

						System.out.println("exch");
						rsa.exchangeKeys(dis, dos);

						byte[] payload = new byte[dis.readInt()];
						dis.readFully(payload);
						System.out.println("before: " + payload[0] + " len: " + payload.length);
						payload = rsa.decrypt(payload);
						System.out.println("after: " + payload[0] + " len: " + payload.length);
						System.out.println(new String(payload, "utf8"));

						// Hang
						dis.read();

						// TODO
						// Read hwid, name, and password

						// Compare ips/hwid/salt -- limit of 3 (_hashed_) per user!

						// Database work

						// Pick a slave

						// Tell slave to accept the calculated hash - master cannot replicate this

						// Write slave ip
					}
				};
			}
		};
		System.out.println("started.");
		server.startAccepting();
	}

	/**
	 * Converts a dotted ip to a long
	 * @param ip the ip to convert
	 * @return the converted ip
	 */
	private static long ipToLong(String ip) {
		String[] addrArray = ip.split("\\.");
		long num = 0;
		for (int i = 0; i < addrArray.length; i++) {
			int power = 3 - i;
			num += ((Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power)));
		}
		return num;
	}

	public static void main(final String[] args) throws IOException, GeneralSecurityException, SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		new AuthenticationServer(43597);
	}
}
