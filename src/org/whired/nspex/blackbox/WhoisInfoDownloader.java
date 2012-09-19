package org.whired.nspex.blackbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.whired.nspex.tools.HttpClient;

public class WhoisInfoDownloader {

	/**
	 * Gets WHOIS information for the specified ip from ARIN in JSON
	 * @param ip the ip to check
	 * @return the whois information in json
	 * @throws IOException when the ARIN server cannot be reached
	 */
	public static String getArinWhoisJson(String ip) throws IOException {
		InputStream is = HttpClient.getStream("http://whois.arin.net/rest/ip/" + ip + ".json");
		BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
		StringBuilder b = new StringBuilder();
		String s;
		while ((s = rdr.readLine()) != null) {
			b.append(s);
		}
		is.close();
		return b.toString();
	}
}
