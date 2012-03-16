package org.whired.inspexi;

import java.awt.AWTException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.whired.inspexi.net.Master;
import org.whired.inspexi.net.Slave;

public class MainTest {
	public static void main(String[] args) throws AWTException, IOException, InterruptedException, InvocationTargetException {
		String opt = args[0].toLowerCase();
		if (opt.equals("server"))
			new Slave();
		else if (opt.equals("client"))
			new Master();
		else
			System.out.println("Usage: gabriel server|client");
	}
}
