package org.whired.nspex.tools;

import java.util.HashSet;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import com.sun.jna.platform.win32.WinUser.MSG;

/**
 * @author l3eta
 * @author Whired
 */
public class KeyboardMonitor implements KeyProducer {

	private final HHOOK hhk;
	private final LowLevelKeyboardProc keyboardHook;
	private final User32 lib;
	private final HashSet<KeyProducer> listeners = new HashSet<KeyProducer>();

	public static void main(final String[] args) {
		new KeyboardMonitor();
	}

	public KeyboardMonitor() {
		lib = User32.INSTANCE;
		final HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
		keyboardHook = new LowLevelKeyboardProc() {
			@Override
			public LRESULT callback(final int nCode, final WPARAM wParam, final KBDLLHOOKSTRUCT info) {
				final int keyCode = info.vkCode;
				if (nCode >= 0) {
					switch (wParam.intValue()) {
						case WinUser.WM_SYSKEYUP:
						case WinUser.WM_KEYUP:
							Keys.delKeyDown(keyCode);
						break;
						case WinUser.WM_KEYDOWN:
						case WinUser.WM_SYSKEYDOWN:
							Keys.addKeyDown(keyCode);
							keyPressed((short) keyCode);
						break;
					}
				}
				return lib.CallNextHookEx(hhk, nCode, wParam, info.getPointer());
			}
		};
		hhk = lib.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, keyboardHook, hMod, 0);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				lib.UnhookWindowsHookEx(hhk);
			}
		}));
		monitor();
	}

	public void monitor() {
		while (lib.GetMessage(new MSG(), null, 0, 0) != 0) {
			;
		}
		lib.UnhookWindowsHookEx(hhk);
	}

	public synchronized void addListener(final KeyProducer listener) {
		listeners.add(listener);
	}

	public synchronized void removeListener(final KeyProducer listener) {
		listeners.remove(listener);
	}

	@Override
	public final synchronized void keyPressed(final short wKeyCode) {
		System.out.print(KeyDef.forCode(wKeyCode).toString());
		for (final KeyProducer l : listeners) {
			l.keyPressed(wKeyCode);
		}
	}

	public static class Keys {

		private static int[] keysDown = { -1, -1, -1, -1 };

		private static void addKeyDown(final int keyCode) {
			for (int i = 0; i < keysDown.length; i++) {
				if (keysDown[i] == -1) {
					keysDown[i] = keyCode;
					return;
				}
				else if (keysDown[i] == keyCode) {
					return;
				}
			}
		}

		private static void delKeyDown(final int keyCode) {
			for (int i = 0; i < keysDown.length; i++) {
				if (keysDown[i] == keyCode) {
					keysDown[i] = -1;
					return;
				}
			}
		}
	}

}