package org.whired.nspex.updater;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;

import org.whired.nspex.tools.HttpClient;

/**
 * Updates packages
 * @author Whired
 */
public class Updater implements Runnable {
	/** An OS independent path to the Java executable */
	private static final String JAVA_EXE = System.getProperty("java.home") + Package.FS + "bin" + Package.FS + "java";

	/** The package this updater is working with */
	private final Package pkg;

	/**
	 * Creates a new updater for the specified package
	 * @param pkg the package to update
	 */
	public Updater(final Package pkg) {
		this.pkg = pkg;
	}

	/** Runs this updater */
	@Override
	public void run() {
		try {
			checkAndLaunch();
		}
		catch (final IOException e1) {
			e1.printStackTrace();
			launch();
		}
	}

	/**
	 * Checks for a new version and downloads, ensuring that the download succeeded
	 * @throws IOException if the file can not be downloaded
	 * @throws UpdateNotFoundException when no update is found
	 */
	private void checkAndLaunch() throws IOException {
		final String destFile = pkg.getLocalCodebase() + pkg.getName();
		final String remoteUrl = pkg.getRemoteCodebase() + pkg.getName();
		final String remoteHash = getRemoteHash(remoteUrl);
		String localHash = getLocalHash(destFile);
		boolean match = localHash != null && remoteHash.toLowerCase().equals(localHash.toLowerCase());
		while (!match) {
			System.out.println("Hash check fail, download");
			HttpClient.saveToDisk(destFile, remoteUrl);
			localHash = getLocalHash(destFile);
			match = remoteHash.toLowerCase().equals(localHash.toLowerCase());
		}
		System.out.println("Hash check pass, launch");
		launch();
	}

	/**
	 * Gets the hash from the file at the specified url
	 * @param url the url of the file to get the hash of
	 * @return the hash that was read
	 */
	private String getRemoteHash(final String url) throws IOException {
		System.out.println(url);
		final BufferedReader br = new BufferedReader(new InputStreamReader(HttpClient.getStream(url + ".MD5")));
		final StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}

	/**
	 * Gets the hash from the file at the given path
	 * @param path the path to the file
	 * @return the hash of the file
	 */
	private String getLocalHash(final String path) {
		BufferedInputStream bis = null;
		try {
			final File file = new File(path);
			final MessageDigest md = MessageDigest.getInstance("MD5");
			int bytesRead;
			final byte[] buffer = new byte[1024];
			bis = new BufferedInputStream(new FileInputStream(file));
			while ((bytesRead = bis.read(buffer)) != -1) {
				md.update(buffer, 0, bytesRead);
			}
			bis.close();
			return toHexString(md.digest());
		}
		catch (final Exception ex) {
		} // Swallow irrelevant exceptions
		finally {
			try {
				if (bis != null) {
					bis.close();
				}
			}
			catch (final IOException ex) {
			}
		}
		return null;
	}

	/**
	 * Converts an array of bytes to a hexadecimal string
	 * @param bytes the bytes to convert
	 * @return the resulting string
	 */
	private static String toHexString(final byte[] bytes) {
		final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		final char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v / 16];
			hexChars[j * 2 + 1] = hexArray[v % 16];
		}
		return new String(hexChars);
	}

	/**
	 * Launches the package for this updater and exits the current program
	 */
	private void launch() {
		try {
			System.out.println("Exec: " + JAVA_EXE + " -classpath " + pkg.getLocalCodebase() + pkg.getName() + " " + pkg.getMainClass());
			new ProcessBuilder(JAVA_EXE, "-classpath", pkg.getLocalCodebase() + pkg.getName(), pkg.getMainClass()).start();
		}
		catch (final IOException e) {
			e.printStackTrace();
		}

		System.exit(0);
	}
}
