package org.whired.nspex.tools;

/**
 * Utilities for ip addresses
 * @author Whired
 */
public class IpUtil {

	/**
	 * The complement to {@link #ip4ToString(String)} - converts an ip4 address to an integer
	 * @param ip4 the ip address to convert, IE "12.126.43.92"
	 * @return the converted ip
	 */
	public static int ip4ToInt(String ip4) {
		String[] parts;

		if (ip4.contains(".") && (parts = ip4.split("\\.")).length == 4) {
			return Integer.parseInt(parts[0]) << 24 | Integer.parseInt(parts[1]) << 16 | Integer.parseInt(parts[2]) << 8 | Integer.parseInt(parts[3]);
		}
		else {
			// IP is invalid
			throw new NumberFormatException("for input string: " + ip4);
		}
	}

	/**
	 * The complement to {@link #ip4ToString(int)} - converts an ip4 integer to a string
	 * @param ip4 the integer value of an ip4 address
	 * @return the formatted string, IE "12.126.43.92"
	 */
	public static String ip4ToString(int ip4) {
		return ((ip4 >> 24) & 0xff) + "." + ((ip4 >> 16) & 0xff) + "." + ((ip4 >> 8) & 0xff) + "." + (ip4 & 0xff);
	}
}
