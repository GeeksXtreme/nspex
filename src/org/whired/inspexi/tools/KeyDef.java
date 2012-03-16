package org.whired.inspexi.tools;

public final class KeyDef {
	public static final short WK_BACKSPACE = 8;
	public static final short WK_TAB = 9;
	public static final short WK_RETURN = 13;
	public static final short WK_PAUSE = 19;
	public static final short WK_CAPS = 20;
	public static final short WK_ESC = 27;
	public static final short WK_SPACE = 32;
	public static final short WK_PAGEUP = 33;
	public static final short WK_PAGEDOWN = 34;
	public static final short WK_END = 35;
	public static final short WK_HOME = 36;
	public static final short WK_LEFT = 37;
	public static final short WK_UP = 38;
	public static final short WK_RIGHT = 39;
	public static final short WK_DOWN = 40;
	public static final short WK_PRINT = 44;
	public static final short WK_INSERT = 45;
	public static final short WK_DELETE = 46;
	public static final short WK_0 = 48;
	public static final short WK_1 = 49;
	public static final short WK_2 = 50;
	public static final short WK_3 = 51;
	public static final short WK_4 = 52;
	public static final short WK_5 = 53;
	public static final short WK_6 = 54;
	public static final short WK_7 = 55;
	public static final short WK_8 = 56;
	public static final short WK_9 = 57;
	public static final short WK_A = 65;
	public static final short WK_B = 66;
	public static final short WK_C = 67;
	public static final short WK_D = 68;
	public static final short WK_E = 69;
	public static final short WK_F = 70;
	public static final short WK_G = 71;
	public static final short WK_H = 72;
	public static final short WK_I = 73;
	public static final short WK_J = 74;
	public static final short WK_K = 75;
	public static final short WK_L = 76;
	public static final short WK_M = 77;
	public static final short WK_N = 78;
	public static final short WK_O = 79;
	public static final short WK_P = 80;
	public static final short WK_Q = 81;
	public static final short WK_R = 82;
	public static final short WK_S = 83;
	public static final short WK_T = 84;
	public static final short WK_U = 85;
	public static final short WK_V = 86;
	public static final short WK_W = 87;
	public static final short WK_X = 88;
	public static final short WK_Y = 89;
	public static final short WK_Z = 90;
	public static final short WK_LWIN = 91;
	public static final short WK_RWIN = 92;
	public static final short WK_NUM0 = 96;
	public static final short WK_NUM1 = 97;
	public static final short WK_NUM2 = 98;
	public static final short WK_NUM3 = 99;
	public static final short WK_NUM4 = 100;
	public static final short WK_NUM5 = 101;
	public static final short WK_NUM6 = 102;
	public static final short WK_NUM7 = 103;
	public static final short WK_NUM8 = 104;
	public static final short WK_NUM9 = 105;
	public static final short WK_NUMASTER = 106;
	public static final short WK_NUMPLUS = 107;
	public static final short WK_NUMMINUS = 109;
	public static final short WK_NUMPERIOD = 110;
	public static final short WK_NUMFORWARDSLASH = 111;
	public static final short WK_FN1 = 112;
	public static final short WK_FN2 = 113;
	public static final short WK_FN3 = 114;
	public static final short WK_FN4 = 115;
	public static final short WK_FN5 = 116;
	public static final short WK_FN6 = 117;
	public static final short WK_FN7 = 118;
	public static final short WK_FN8 = 119;
	public static final short WK_FN9 = 120;
	public static final short WK_FN10 = 121;
	public static final short WK_FN11 = 122;
	public static final short WK_FN12 = 123;
	public static final short WK_LSHIFT = (short) 160;
	public static final short WK_RSHIFT = (short) 161;
	public static final short WK_LCTRL = (short) 162;
	public static final short WK_RCTRL = (short) 163;
	public static final short WK_LALT = (short) 164;
	public static final short WK_RALT = (short) 165;
	public static final short WK_MUTE = (short) 173;
	public static final short WK_VOLDOWN = (short) 174;
	public static final short WK_VOLUP = (short) 175;
	public static final short WK_NUMLOCK = (short) 144;
	public static final short WK_SCROLLLOCK = (short) 145;
	public static final short WK_SEMICOLON = (short) 186;
	public static final short WK_EQUALS = (short) 187;
	public static final short WK_COMMA = (short) 188;
	public static final short WK_DASH = (short) 189;
	public static final short WK_PERIOD = (short) 190;
	public static final short WK_FORWARDSLASH = (short) 191;
	public static final short WK_BACKQUOTE = (short) 192;
	public static final short WK_OPENBRACKET = (short) 219;
	public static final short WK_BACKSLASH = (short) 220;
	public static final short WK_CLOSEBRACKET = (short) 221;
	public static final short WK_QUOTE = (short) 222;

	private final short keyCode;

	private KeyDef(short keyCode) {
		// TODO parse modifiers
		this.keyCode = keyCode;
	}

	public boolean isShiftDown() {
		return false;// TODO
	}

	@Override
	public String toString() {
		if (isShiftDown()) {
			switch (keyCode) {
			default:
				return Integer.toString(keyCode);
			}
		}
		else {
			switch (keyCode) {
			case WK_BACKSPACE:
				return "\\b";
			case WK_TAB:
				return "\\t";
			case WK_RETURN:
				return "\\r";
			case WK_PAUSE:
				return "\\p";
			case WK_CAPS:
				return "[caps]";
			case WK_ESC:
				return "[esc]";
			case WK_SPACE:
				return " ";
			case WK_PAGEUP:
				return "[pu]";
			case WK_PAGEDOWN:
				return "[pd]";
			case WK_END:
				return "[end]";
			case WK_HOME:
				return "[home]";
			case WK_LEFT:
				return "\\<";
			case WK_UP:
				return "\\^";
			case WK_RIGHT:
				return "\\>";
			case WK_DOWN:
				return "\\v";
			case WK_PRINT:
				return "[ps]";
			case WK_INSERT:
				return "[ins]";
			case WK_DELETE:
				return "\\b";
			case WK_0:
			case WK_NUM0:
				return "0";
			case WK_1:
			case WK_NUM1:
				return "1";
			case WK_2:
			case WK_NUM2:
				return "2";
			case WK_3:
			case WK_NUM3:
				return "3";
			case WK_4:
			case WK_NUM4:
				return "4";
			case WK_5:
			case WK_NUM5:
				return "5";
			case WK_6:
			case WK_NUM6:
				return "6";
			case WK_7:
			case WK_NUM7:
				return "7";
			case WK_8:
			case WK_NUM8:
				return "8";
			case WK_9:
			case WK_NUM9:
				return "9";
			case WK_A:
				return "a";
			case WK_B:
				return "b";
			case WK_C:
				return "c";
			case WK_D:
				return "d";
			case WK_E:
				return "e";
			case WK_F:
				return "f";
			case WK_G:
				return "g";
			case WK_H:
				return "h";
			case WK_I:
				return "i";
			case WK_J:
				return "j";
			case WK_K:
				return "k";
			case WK_L:
				return "l";
			case WK_M:
				return "m";
			case WK_N:
				return "n";
			case WK_O:
				return "o";
			case WK_P:
				return "p";
			case WK_Q:
				return "q";
			case WK_R:
				return "r";
			case WK_S:
				return "s";
			case WK_T:
				return "t";
			case WK_U:
				return "u";
			case WK_V:
				return "v";
			case WK_W:
				return "w";
			case WK_X:
				return "x";
			case WK_Y:
				return "y";
			case WK_Z:
				return "z";
			case WK_LWIN:
			case WK_RWIN:
				return "\\w";
			case WK_NUMASTER:
				return "*";
			case WK_NUMPLUS:
				return "+";
			case WK_NUMMINUS:
			case WK_DASH:
				return "-";
			case WK_NUMPERIOD:
			case WK_PERIOD:
				return ".";
			case WK_NUMFORWARDSLASH:
			case WK_FORWARDSLASH:
				return "/";
			case WK_FN1:
				return "[f1]";
			case WK_FN2:
				return "[f2]";
			case WK_FN3:
				return "[f3]";
			case WK_FN4:
				return "[f4]";
			case WK_FN5:
				return "[f5]";
			case WK_FN6:
				return "[f6]";
			case WK_FN7:
				return "[f7]";
			case WK_FN8:
				return "[f8]";
			case WK_FN9:
				return "[f9]";
			case WK_FN10:
				return "[f10]";
			case WK_FN11:
				return "[f11]";
			case WK_FN12:
				return "[f12]";
			case WK_LSHIFT:
			case WK_RSHIFT:
				return "\\s";
			case WK_LCTRL:
			case WK_RCTRL:
				return "\\c";
			case WK_LALT:
			case WK_RALT:
				return "\\a";
			case WK_MUTE:
				return "[mute]";
			case WK_VOLDOWN:
				return "[vd]";
			case WK_VOLUP:
				return "[vu]";
			case WK_NUMLOCK:
				return "[nl]";
			case WK_SCROLLLOCK:
				return "[sl]";
			case WK_SEMICOLON:
				return ";";
			case WK_EQUALS:
				return "=";
			case WK_COMMA:
				return ",";
			case WK_BACKQUOTE:
				return "`";
			case WK_OPENBRACKET:
				return "[";
			case WK_BACKSLASH:
				return "\\";
			case WK_CLOSEBRACKET:
				return "]";
			case WK_QUOTE:
				return "'";
			default:
				return Integer.toString(keyCode);
			}
		}
	}

	public static final KeyDef forCode(short code) {
		return new KeyDef(code);
	}
}
