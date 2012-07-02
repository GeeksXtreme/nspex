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
	/** The end of line command, used to prevent deletion of output */
	private final String CMDC = "\r\n";
	/** A custom caret that prevents insertion at points of output */
	private final Caret caret = new DefaultCaret() {

		@Override
		public void setDot(final int dot) {
			super.setDot(getValidPos(dot));
		}

		@Override
		public void setDot(final int paramInt, final Bias paramBias) {
			super.setDot(getValidPos(paramInt), paramBias);
		}

		/**
		 * Gets the closest valid position to the specified point
		 * @param orig the original position point
		 * @return the closest valid position
		 */
		private int getValidPos(int orig) {
			final int lastcmd = getText().lastIndexOf(CMDC) + CMDC.length();
			if (orig < lastcmd) {
				orig = lastcmd;
			}
			return orig;
		}
	};
	/** The set of listeners subscribed to this console */
	private final HashSet<CommandListener> listeners = new HashSet<CommandListener>();
	/** The buffer of saved commands */
	BufferQueue<String> savedCommands = new BufferQueue<String>(256);
	/** The current scroll index of {@link #savedCommands} */
	private int savedCommandsIdx = 0;

	/**
	 * Creates a new console
	 */
	public JConsole() {

		// Add listeners that provide custom behavior
		addListeners();

		// Set the custom caret that provides custom behavior
		setCaret(caret);

		// The nimbus blue color
		final Color nimBlue = new Color(57, 105, 138);

		// Set some LaF
		setBackground(Color.BLACK);
		setForeground(nimBlue);
		setSelectionColor(nimBlue);
		setSelectedTextColor(Color.BLACK);
		setFont(new Font("Monospaced", Font.PLAIN, 12));

		//?
		//append(CMDC);
	}

	/**
	 * Adds listeners to this console
	 */
	private final void addListeners() {
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
					int cmdcIdx = getText().lastIndexOf(CMDC);
					// cmdc might not be present
					cmdcIdx = cmdcIdx < 0 ? 0 : cmdcIdx + CMDC.length();
					final String command = getText().substring(cmdcIdx);
					// Remove from text because it will be echoed
					try {
						getDocument().remove(cmdcIdx, getText().length() - cmdcIdx);
					}
					catch (BadLocationException e) {
						e.printStackTrace();
					}
					k.consume();
					fireCommand(command);
					savedCommands.offer(command);
					savedCommandsIdx = savedCommands.size() - 1;
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
						replaceCurrent(savedCommands.get(savedCommandsIdx));
						if (savedCommandsIdx > 0) {
							savedCommandsIdx--;
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
						if (savedCommandsIdx < savedCommands.size() - 1) {
							savedCommandsIdx++;
							replaceCurrent(savedCommands.get(savedCommandsIdx));
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
	}

	@Override
	public void append(String str) {
		super.append(str);

		// Auto scroll
		setCaretPosition(getDocument().getLength());
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
		// Check for clear commands
		final String lwc = cmd.toLowerCase();
		if (lwc.equals("cls") || lwc.equals("clear")) {
			try {
				getDocument().remove(0, getDocument().getLength());
			}
			catch (BadLocationException e) {
				// Seriously, why is this even checked..
			}
		}
		else {
			synchronized (this) {
				for (final CommandListener l : listeners) {
					l.doCommand(cmd);
				}
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
