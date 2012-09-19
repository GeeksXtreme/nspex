package org.whired.nspex.blackbox;

/**
 * Constants for communication operation codes
 * @author Whired
 */
public interface Opcodes {
	int RSA_KEY_REQUEST = 0;
	int LOGIN = 1;
	int SLAVES_RECEIVED = 2;
	int CONFIRM_ISP_CHANGE = 3;
}
