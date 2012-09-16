package org.whired.nspex.blackbox;

import java.io.IOException;

import org.whired.nspex.blackbox.json.JSONException;
import org.whired.nspex.blackbox.json.JSONObject;
import org.whired.nspex.tools.IpUtil;
import org.whired.nspex.tools.logging.Log;

/**
 * Whois information for an ipv4 address
 * @author Whired
 */
public class WhoisInfo {
	/** The name of the company */
	private final String name;
	/** The start (min) address for the company */
	private final int startAddress;
	/** The end (max) address for the company */
	private final int endAddress;

	/**
	 * Creates a new whois information based on the specified information
	 * @param name the name of the company
	 * @param startAddress the start (min) address for the company
	 * @param endAddress the end (max) address for the company
	 */
	WhoisInfo(final String name, final int startAddress, final int endAddress) {
		this.name = name;
		this.startAddress = startAddress;
		this.endAddress = endAddress;
	}

	/**
	 * Creates a new whois information by parsing the specified ARIN JSON
	 * @param json the raw JSON
	 * @return the whois information
	 * @throws JSONException if the JSON cannot be parsed
	 */
	public static WhoisInfo fromARINJSON(String json) throws JSONException {
		JSONObject jo = new JSONObject(json).getJSONObject("net");
		String lname = jo.getJSONObject("name").getString("$");
		String strStartAdr = jo.getJSONObject("startAddress").getString("$");
		String strEndAdr = jo.getJSONObject("endAddress").getString("$");

		return new WhoisInfo(lname, IpUtil.ip4ToInt(strStartAdr), IpUtil.ip4ToInt(strEndAdr));
	}

	/**
	 * Gets the name of the company
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the start (min) address in unsigned integer format
	 * @return the unsigned integer representing the start address
	 */
	public int getStartAddress() {
		return startAddress;
	}

	/**
	 * Gets the end (max) address in unsigned integer format
	 * @return the unsigned integer representing the end address
	 */
	public int getEndAddress() {
		return endAddress;
	}

	/**
	 * Specifies whether or not the given unsigned integer representing an ip falls between {@link #getStartAddress()} and {@link #getEndAddress()}
	 * @param ip the ipv4 to check
	 * @return {@code true} if it is between, otherwise {@code false}
	 */
	public boolean withinRange(int ip) {
		return ip >= startAddress && ip <= endAddress;
	}

	public static void main(String[] ar) throws IOException {
		try {
			WhoisInfo wii = fromARINJSON(WhoisInfoDownloader.getArinWhoisJson("12.126.43.92"));
			Log.l.info("wii: " + wii.getName() + " start: " + wii.getStartAddress() + " end: " + wii.getEndAddress() + " +/-: " + (wii.getEndAddress() - wii.getStartAddress()));
			int ip4 = IpUtil.ip4ToInt("12.126.43.92");
			Log.l.info("Test_2: " + ip4);
			//Log.l.info("Test_3: " + IpUtil.ip4ToInt("banana"));
			Log.l.info("Test_4: " + IpUtil.ip4ToString(ip4));
		}
		catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
