package org.whired.inspexi.master;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Position.Bias;

public class JConsole extends JTextArea {
	private static final String CMDC = "[-> ";
	private final Caret caret;
	private final HashSet<CommandListener> listeners = new HashSet<CommandListener>();

	public JConsole() {
		caret = new DefaultCaret() {

			@Override
			public void setDot(int dot) {
				super.setDot(getValidPos(dot));
			}

			@Override
			public void setDot(int paramInt, Bias paramBias) {
				super.setDot(getValidPos(paramInt), paramBias);
			}

			private int getValidPos(int orig) {
				int lastcmd = getText().lastIndexOf(CMDC) + CMDC.length();
				if (orig < lastcmd) {
					orig = lastcmd;
				}
				return orig;
			}
		};
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent k) {
				String st = getSelectedText();
				int c = k.getKeyCode();
				if (st != null && c != KeyEvent.VK_LEFT && c != KeyEvent.VK_RIGHT && c != KeyEvent.VK_UP && c != KeyEvent.VK_DOWN && c != KeyEvent.VK_SHIFT) {
					if (st.contains(CMDC)) {
						setCaretPosition(getText().lastIndexOf(CMDC) + CMDC.length());
						moveCaretPosition(getDocument().getLength());
					}
				}
				if (c == KeyEvent.VK_BACK_SPACE) {
					try {
						if (getDocument().getText(Math.max(caret.getDot(), caret.getMark()) - CMDC.length(), CMDC.length()).equals(CMDC)) {
							k.consume();
						}
					}
					catch (BadLocationException e) {
					}
				}
				else if (c == KeyEvent.VK_ENTER) {
					String command = getText().substring(getText().lastIndexOf(CMDC) + CMDC.length());
					append("\n" + CMDC);
					k.consume();
					fireCommand(command);
				}
			}
		});
		setCaret(caret);
		setBackground(Color.BLACK);
		setForeground(Color.GREEN);
		setSelectionColor(Color.GREEN);
		setSelectedTextColor(Color.BLACK);
		setFont(new Font("Monospaced", Font.PLAIN, 12));
		append(CMDC);
	}

	private void fireCommand(String cmd) {
		synchronized (this) {
			for (CommandListener l : listeners) {
				l.doCommand(cmd);
			}
		}
	}

	public void addCommandListener(CommandListener l) {
		synchronized (this) {
			listeners.add(l);
		}
	}

	public void removeCommandListener(CommandListener l) {
		synchronized (this) {
			listeners.remove(l);
		}
	}
}
