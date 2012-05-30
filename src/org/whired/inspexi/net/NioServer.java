package org.whired.inspexi.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.whired.inspexi.tools.logging.Log;

/**
 * Listens for and manages incoming connections and data
 * @author Whired
 */
public abstract class NioServer {
	/** The server socket channel to use */
	private final ServerSocketChannel serverChannel;
	/** The selector to use */
	private final Selector selector;
	/** A map of keys to their respective communicables */
	private final HashMap<SelectionKey, Communicable> connections = new HashMap<SelectionKey, Communicable>();
	/** A set of all the currently connected ips */
	private final HashSet<String> connectedIps = new HashSet<String>();
	private long lastIdleCheckMS;

	/**
	 * Creates and starts a new server listening on the specified port
	 * @param port the port to listen on
	 * @throws IOException if the server cannot be started
	 */
	public NioServer(int port) throws IOException {
		// Prepare the NIO facilities
		selector = Selector.open();
		serverChannel = ServerSocketChannel.open();

		// 100% async
		serverChannel.configureBlocking(false);
		// Make this channel report accept()s to selector
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		// Start it up
		serverChannel.socket().bind(new InetSocketAddress(port));

		Log.l.info("Server is bound to port " + port);
	}

	public final void startListening() throws IOException {
		while (true) {

			//If a suitable amount of time passed, check for idle channels
			if (System.currentTimeMillis() - lastIdleCheckMS > 1500) {
				Iterator<SelectionKey> keys = selector.keys().iterator();
				while (keys.hasNext()) {
					SelectionKey key = keys.next();
					if (key.isValid()) {
						Communicable comm = connections.get(key);
						// Not all keys are communicables
						if (comm != null) {
							int to = comm.getReadTimeout();
							if (to > 0 && (System.currentTimeMillis() - comm.getLastReadTime()) >= to) {
								removeKey(key);
							}
						}
					}
				}
				lastIdleCheckMS = System.currentTimeMillis();
			}

			// Block until we get a key or 2s elapse
			int count = selector.select(2000);

			if (count > 0) {
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey currentKey = it.next();
					if (currentKey.isValid()) {
						if (currentKey.isAcceptable()) {
							ServerSocketChannel ssc = (ServerSocketChannel) currentKey.channel();

							// We'll accept no matter what to show we're online
							SocketChannel sc = ssc.accept();

							// But we aren't going to add another reader for this host
							String ip = ((InetSocketAddress) sc.socket().getRemoteSocketAddress()).getHostName();
							sc.configureBlocking(false);
							currentKey = sc.register(selector, SelectionKey.OP_READ);
							if (!connectedIps.contains(ip)) {
								Communicable c = getCommunicable(currentKey);
								connections.put(currentKey, c);
								connectedIps.add(ip);
								Log.l.info("Accepted [" + ip + "], size=" + connections.size());
							}
							else {
								Log.l.info("Rejected [" + ip + "], size=" + connections.size());
								currentKey.cancel();
							}
						}
						else if (currentKey.isReadable()) {
							try {
								Communicable comm = connections.get(currentKey);
								if (comm.read() > -1) {
									comm.setLastReadTime(System.currentTimeMillis());
								}
								else {
									currentKey.cancel();
								}
							}
							catch (IOException ioe) {
								currentKey.cancel();
							}
						}
					}
					// If anything at all went wrong remove the key
					if (!currentKey.isValid()) {
						removeKey(currentKey);
					}
				}
				it.remove();
			}
		}
	}

	/**
	 * Removes, invalidates, and closes a key's channel
	 * @param key the key to remove
	 */
	private void removeKey(SelectionKey key) {
		Communicable comm;
		try {
			key.channel().close();
		}
		catch (IOException e) {
		}
		if ((comm = connections.remove(key)) != null) {
			String ip = ((InetSocketAddress) ((SocketChannel) key.channel()).socket().getRemoteSocketAddress()).getHostName();
			Log.l.info("Removing [" + ip + "], size=" + connections.size());
			connectedIps.remove(ip);
			comm.disconnected();
		}
	}

	/**
	 * Invoked when a channel has opened for a connecting client
	 * @param key the key that was registered
	 * @return the communicable that will handle information transfers
	 */
	protected abstract Communicable getCommunicable(SelectionKey key);
}
