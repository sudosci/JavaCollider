/*
 * LogTextArea.java
 * (JavaCollider)
 * Copyright (c) 2004-2015 Hanns Holger Rutz. All rights reserved.
 * This software is published under the GNU Lesser General Public License v2.1+
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.jcollider.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.AbstractAction;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

/**
 * A <code>JTextArea</code> encompassing a <code>PrintWriter</code> that can be
 * used as an alternative to the standard <code>System.out</code> or
 * <code>System.err</code> objects. Writing to this <code>PrintWriter</code>
 * will append the text to the text area.
 * <p>
 * This code is based on an idea by Real Gagnon published at:<br>
 * <A HREF=
 * "http://tanksoftware.com/juk/developer/src/com/tanksoftware/util/RedirectedFrame.java">
 * tanksoftware.com/juk/developer/src/com/tanksoftware/util/RedirectedFrame.java</A>
 *
 * @author Hanns Holger Rutz
 * @version 0.32, 25-Feb-08
 *
 * @see java.io.PrintStream
 * @see java.lang.System#setOut( PrintStream )
 * @see java.lang.System#setErr( PrintStream )
 */
public class LogTextArea extends JTextArea {
	protected final boolean useLogFile;
	protected final File logFile;
	private final PrintStream outStream;
	protected FileWriter logFileWriter = null;
	// private final JTextArea textArea = this;
	private int totalLength = 0;
	// XXX JavaCollider
	// private MenuAction actionClear = null;
	private AbstractAction actionClear = null;

	/**
	 * Constructs a new text area for logging messages. The area is readonly and
	 * wraps lines as they exceed the right margin. Alternatively messages can be
	 * logged in a text file.
	 *
	 * @param rows
	 *            same as in JTextArea()
	 * @param columns
	 *            same as in JTextArea()
	 * @param useLogFile
	 *            <code>true</code> to have a copy of the output logged into a file
	 * @param logFile
	 *            if <code>useLogFile</code> is <code>true</code>, this is the file
	 *            into which the log is written. if <code>useLogFile</code> is
	 *            <code>false</code>, you can pass <code>null</code> here.
	 */
	public LogTextArea(int rows, int columns, boolean useLogFile, File logFile) {
		super(rows, columns);

		this.useLogFile = useLogFile;
		this.logFile = logFile;
		outStream = new PrintStream(new RedirectedStream());

		setEditable(false);
		setLineWrap(true);
	}

	public LogTextArea() {
		this(6, 40, false, null);
	}

	/*
	 * Returns the stream used by this gadget to write data to.
	 * 
	 * @return the <code>PrintStream</code>, useful for redirecting system output
	 * to.
	 * 
	 * @see java.lang.System#setOut( PrintStream )
	 * 
	 * @see java.lang.System#setErr( PrintStream )
	 * 
	 * @warning Theoretically if you use this stream for <code>System.setErr</code>,
	 * you will create a recursion deadlock if an exception is thrown within the
	 * <code>write</code> method of the stream. This case has never been experienced
	 * however.
	 */
	public PrintStream getLogStream() {
		return outStream;
	}

	/**
	 * This method is public because of the superclass method. Appending text using
	 * this method directly will not use the internal print stream and thus not
	 * appear in a log file.
	 */
	public void append(String str) {
		super.append(str);
		totalLength += str.length();
		updateCaret();
	}

	private void updateCaret() {
		try {
			setCaretPosition(Math.max(0, totalLength - 1));
		} catch (IllegalArgumentException e1) {
			/* ignored */ }
	}

	/**
	 * Replaces the gadget's text. This is useful for clearing the gadget. This
	 * doesn't affect the <code>PrintStream</code> or the log file.
	 *
	 * @param str
	 *            the new text to replace the gadgets content or <code>null</code>
	 *            to clear the gadget.
	 */
	public void setText(String str) {
		super.setText(str);
		totalLength = str == null ? 0 : str.length();
	}

	// public void laterInvocation( Object o )
	// {
	// append( (String) o );
	// }

	// XXX JavaCollider
	// public MenuAction getClearAction()
	public AbstractAction getClearAction() {
		if (actionClear == null) {
			actionClear = new ActionClear();
		}
		return actionClear;
	}

	public JScrollPane placeMeInAPane() {
		return (new JScrollPane(this, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, // aqua hin aqua her.
																						// VERTICAL_SCROLLBAR_ALWAYS
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
	}

	public void makeSystemOutput() {
		System.setOut(getLogStream());
		System.setErr(getLogStream());
	}

	// ---------------- internal classes ----------------

	private class RedirectedStream extends OutputStream {
		private byte[] cheesy = new byte[1];
		// private int totalLength = 0;

		protected RedirectedStream() {
			super();
		}

		public void write(byte b[]) throws IOException {
			this.write(b, 0, b.length);
		}

		public void write(byte b[], int off, int len) throws IOException {
			String str = new String(b, off, len);
			append(str);
			// lim.queue( str );

			if (useLogFile) {
				if (logFileWriter == null) {
					logFileWriter = new FileWriter(logFile);
				}
				logFileWriter.write(str);
			}
		}

		public void flush() throws IOException {
			if (logFileWriter != null) {
				logFileWriter.flush();
			}
			super.flush();
		}

		public void close() throws IOException {
			if (logFileWriter != null) {
				logFileWriter.close();
				logFileWriter = null;
			}
			super.close();
		}

		public void write(int b) throws IOException {
			cheesy[0] = (byte) b;
			this.write(cheesy);
		}
	}

	private class ActionClear
			// XXX JavaCollider
			// extends MenuAction
			extends AbstractAction {
		protected ActionClear() {
			/* empty */ }

		public void actionPerformed(ActionEvent e) {
			setText(null);
		}
	}
}