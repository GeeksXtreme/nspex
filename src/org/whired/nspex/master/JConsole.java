package org.whired.nspex.master;

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
	private final String CMDC;// = "[" + System.getProperty("user.name") + "> ";
	private final Caret caret;
	private final HashSet<CommandListener> listeners = new HashSet<CommandListener>();

	BufferQueue<String> savedCommands = new BufferQueue<String>(256);
	private int pos = 0;

	public JConsole(final String username) {
		CMDC = "[" + username + "> ";
		caret = new DefaultCaret() {

			@Override
			public void setDot(final int dot) {
				super.setDot(getValidPos(dot));
			}

			@Override
			public void setDot(final int paramInt, final Bias paramBias) {
				super.setDot(getValidPos(paramInt), paramBias);
			}

			private int getValidPos(int orig) {
				final int lastcmd = getText().lastIndexOf(CMDC) + CMDC.length();
				if (orig < lastcmd) {
					orig = lastcmd;
				}
				return orig;
			}
		};
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent k) {
				final String st = getSelectedText();
				final int c = k.getKeyCode();
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
					catch (final BadLocationException e) {
					}
				}
				else if (c == KeyEvent.VK_ENTER) {
					final String command = getText().substring(getText().lastIndexOf(CMDC) + CMDC.length());
					append("\n" + CMDC);
					k.consume();
					fireCommand(command);
					savedCommands.offer(command);
					pos = savedCommands.size() - 1;
				}
				else if (c == KeyEvent.VK_UP) {
					if (k.isShiftDown()) {
						// Do selection stuff
						k.consume();
						moveCaretPosition(getText().lastIndexOf(CMDC) + CMDC.length());
					}
					else {
						// Do command history stuff
						k.consume();
						replaceCurrent(savedCommands.get(pos));
						if (pos > 0) {
							pos--;
						}
					}
				}
				else if (c == KeyEvent.VK_DOWN) {
					if (k.isShiftDown()) {
						// Do selection stuff
						k.consume();
						moveCaretPosition(getDocument().getLength());
					}
					else {
						// Do command history stuff
						k.consume();
						if (pos < savedCommands.size() - 1) {
							pos++;
							replaceCurrent(savedCommands.get(pos));
						}
						else {
							replaceCurrent("");
						}
					}
				}
				else if (c == KeyEvent.VK_HOME && k.isShiftDown()) {
					k.consume();
					moveCaretPosition(getText().lastIndexOf(CMDC) + CMDC.length());
				}
				else if (c == KeyEvent.VK_END && k.isShiftDown()) {
					k.consume();
					moveCaretPosition(getDocument().getLength());
				}
			}
		});
		setCaret(caret);
		setBackground(Color.BLACK);
		final Color nimBlue = new Color(57, 105, 138);
		setForeground(nimBlue);
		setSelectionColor(nimBlue);
		setSelectedTextColor(Color.BLACK);
		setFont(new Font("Monospaced", Font.PLAIN, 12));
		append(CMDC);
	}

	private void replaceCurrent(final String newCommand) {
		try {
			final int goodIdx = getText().lastIndexOf(CMDC) + CMDC.length();
			getDocument().remove(goodIdx, getDocument().getLength() - goodIdx);
			getDocument().insertString(goodIdx, newCommand, null);
			setCaretPosition(getDocument().getLength());
			moveCaretPosition(getDocument().getLength());
		}
		catch (final BadLocationException e) {
			e.printStackTrace();
		}
	}

	private void fireCommand(final String cmd) {
		synchronized (this) {
			for (final CommandListener l : listeners) {
				l.doCommand(cmd);
			}
		}
	}

	public void addCommandListener(final CommandListener l) {
		synchronized (this) {
			listeners.add(l);
		}
	}

	public void removeCommandListener(final CommandListener l) {
		synchronized (this) {
			listeners.remove(l);
		}
	}
}
