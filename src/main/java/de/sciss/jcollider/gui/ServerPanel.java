/*
 * ServerWindow.java
 * (JavaCollider)
 * Copyright (c) 2004-2015 Hanns Holger Rutz. All rights reserved.
 * This software is published under the GNU Lesser General Public License v2.1+
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.jcollider.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import de.sciss.jcollider.Constants;
import de.sciss.jcollider.JavaCollider;
import de.sciss.jcollider.Server;
import de.sciss.jcollider.ServerEvent;
import de.sciss.jcollider.ServerListener;

/**
 * A quick hack to provide a GUI element looking similar to SCLang's server
 * window. Additionally, you can opt to create a text console for scsynth text
 * output when booting the local server.
 *
 * @author Hanns Holger Rutz
 * @version 0.32, 25-Feb-08
 */
public class ServerPanel extends JPanel implements ServerListener, Constants {
	/**
	 * <code>Font</code> used for the text console. You can set this field prior to
	 * instantiating the <code>ServerPanel</code> to use a different font.
	 */
	public static Font fntConsole;
	/**
	 * <code>Font</code> used for the GUI elements (buttons). You can set this field
	 * prior to instantiating the <code>ServerPanel</code> to use a different font.
	 */
	public static Font fntGUI = new Font("Helvetica", Font.PLAIN, 12);
	/**
	 * <code>Font</code> used for server name box. You can set this field prior to
	 * instantiating the <code>ServerPanel</code> to use a different font.
	 */
	public static Font fntBigName = new Font("Helvetica", Font.BOLD, 16);

	/**
	 * Constructor flag: create a console panel.
	 */
	public static final int CONSOLE = 0x01;
	/**
	 * Constructor flag: create a boot/quit button
	 */
	public static final int BOOTQUIT = 0x02;
	/**
	 * Constructor flag: create a server name box
	 */
	public static final int BIGNAME = 0x08;
	/**
	 * Constructor flag: create a status panel for server CPU usage and number of
	 * nodes.
	 */
	public static final int COUNTS = 0x40;
	/**
	 * Constructor flag: create a button to turning OSC dumping on/off.
	 */
	public static final int DUMP = 0x80;

	/**
	 * Constructor flag: shortcut for creating those GUI elements that SClang
	 * provides.
	 */
	public static final int MIMIC = 0x7E;

	private static final int BUTTONS = 0xBE;

	protected final Server server;
	protected final ServerPanel enc_this = this;

	private JTextField lbBigName = null;
	private ActionBoot actionBoot = null;

	private JFrame ourFrame;
	private JScrollPane ggScroll;
	private boolean showHide;

	private JLabel lbCntAvgCPU, lbCntPeakCPU, lbCntUGens, lbCntSynths, lbCntGroups, lbCntSynthDefs;

	private static final String COUNT_NA = "?";
	private static final MessageFormat msgCntPercent = new MessageFormat("{0,number,#.#} %", Locale.US);

	private static final Color colrRunning = new Color(0x58, 0xB0, 0x8D);
	private static final Color colrBooting = new Color(0xCC, 0xFF, 0x33);
	private static final Color colrStopped = new Color(0x60, 0x60, 0x60);

	static {
		fntConsole = JavaCollider.isMacOS ? new Font("Monaco", Font.PLAIN, 10) : new Font("Monospaced", Font.PLAIN, 11);
	}

	/**
	 * Creates a new <code>ServerPanel</code> for the given server and with GUI
	 * elements specified by the flags.
	 *
	 * @param server
	 *            the server to which this panel is connected
	 * @param flags
	 *            a mask of flags that define which GUI elements should be created,
	 *            e.g. <code>CONSOLE</code>, <code>BOOTQUIT</code> etc.
	 */
	public ServerPanel(Server server, int flags) {
		super();

		this.server = server;

		final BoxLayout lay = new BoxLayout(this, BoxLayout.Y_AXIS);

		setLayout(lay);

		if ((flags & CONSOLE) != 0) {
			add(createConsole());
		}
		if ((flags & BUTTONS) != 0) {
			add(createButtons(flags));
		}
		if ((flags & COUNTS) != 0) {
			add(createCountsPanel());
		}

		server.addListener(this);
	}

	/**
	 * Call this method if the server isn't used any more or the panel's parent
	 * window is disposed. This will free any resources occupied by the panel and
	 * remove listeners.
	 */
	public void dispose() {
		server.removeListener(this);
	}

	private JComponent createConsole() {
		final LogTextArea lta = new LogTextArea(12, 40, false, null);
		final InputMap imap = lta.getInputMap();
		final ActionMap amap = lta.getActionMap();

		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "clear");
		amap.put("clear", lta.getClearAction());
		lta.setFont(fntConsole);

		Server.setPrintStream(lta.getLogStream());

		ggScroll = lta.placeMeInAPane();

		return ggScroll;
	}

	private JComponent createCountsPanel() {
		final JPanel p = new JPanel(new GridLayout(3, 4, 0, 4));

		lbCntAvgCPU = new JLabel();
		lbCntPeakCPU = new JLabel();
		lbCntUGens = new JLabel();
		lbCntSynths = new JLabel();
		lbCntGroups = new JLabel();
		lbCntSynthDefs = new JLabel();

		p.add(new JLabel(JavaCollider.getResourceString("countsAvgCPU") + " : ", SwingConstants.RIGHT));
		p.add(lbCntAvgCPU);
		p.add(new JLabel(JavaCollider.getResourceString("countsPeakCPU") + " : ", SwingConstants.RIGHT));
		p.add(lbCntPeakCPU);
		p.add(new JLabel(JavaCollider.getResourceString("countsUGens") + " : ", SwingConstants.RIGHT));
		p.add(lbCntUGens);
		p.add(new JLabel(JavaCollider.getResourceString("countsSynths") + " : ", SwingConstants.RIGHT));
		p.add(lbCntSynths);
		p.add(new JLabel(JavaCollider.getResourceString("countsGroups") + " : ", SwingConstants.RIGHT));
		p.add(lbCntGroups);
		p.add(new JLabel(JavaCollider.getResourceString("countsSynthDefs") + " : ", SwingConstants.RIGHT));
		p.add(lbCntSynthDefs);

		p.setBorder(BorderFactory.createEmptyBorder(2, 8, 8, 8));
		JavaCollider.setDeepFont(p, fntGUI);

		p.setMaximumSize(p.getPreferredSize());
		updateCounts();
		return p;
	}

	private JComponent createButtons(int flags) {
		final JToolBar tb = new JToolBar();
		AbstractButton but;
		Insets insets;

		tb.setBorderPainted(false);
		tb.setFloatable(false);

		if ((flags & BOOTQUIT) != 0) {
			actionBoot = new ActionBoot();
			but = new JButton(actionBoot);
			but.setFont(fntGUI);
			insets = but.getMargin();
			but.setMargin(new Insets(insets.top + 2, insets.left + 4, insets.bottom + 2, insets.right + 4));
			tb.add(but);
		}
		if ((flags & BIGNAME) != 0) {
			lbBigName = new JTextField(8);
			lbBigName.setFont(fntBigName);
			lbBigName.setMaximumSize(lbBigName.getPreferredSize());
			lbBigName.setText(server.getName());
			lbBigName.setEditable(false);
			lbBigName.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
			lbBigName.setBackground(Color.black);
			lbBigName.setHorizontalAlignment(SwingConstants.CENTER);
			updateBigName();
			tb.add(lbBigName);
		}
		
		if ((flags & DUMP) != 0) {
			but = new JToggleButton(new ActionDump());
			but.setFont(fntGUI);
			insets = but.getMargin();
			but.setMargin(new Insets(insets.top + 2, insets.left + 4, insets.bottom + 2, insets.right + 4));
			tb.add(but);
		}
		return tb;
	}

	protected void updateBigName() {
		if (lbBigName == null)
			return;

		if (server.isRunning()) {
			lbBigName.setForeground(colrRunning);
		} else if (server.isBooting()) {
			lbBigName.setForeground(colrBooting);
		} else {
			lbBigName.setForeground(colrStopped);
		}
	}

	private void updateCounts() {
		if (lbCntAvgCPU == null)
			return;

		if (server.isRunning()) {
			final Server.Status status = server.getStatus();
			final Float[] cntArgs = new Float[1];
			cntArgs[0] = new Float(status.avgCPU);
			lbCntAvgCPU.setText(msgCntPercent.format(cntArgs));
			cntArgs[0] = new Float(status.peakCPU);
			lbCntPeakCPU.setText(msgCntPercent.format(cntArgs));
			lbCntUGens.setText(String.valueOf(status.numUGens));
			lbCntSynths.setText(String.valueOf(status.numSynths));
			lbCntGroups.setText(String.valueOf(status.numGroups));
			lbCntSynthDefs.setText(String.valueOf(status.numSynthDefs));
		} else {
			lbCntAvgCPU.setText(COUNT_NA);
			lbCntPeakCPU.setText(COUNT_NA);
			lbCntUGens.setText(COUNT_NA);
			lbCntSynths.setText(COUNT_NA);
			lbCntGroups.setText(COUNT_NA);
			lbCntSynthDefs.setText(COUNT_NA);
		}
	}

	protected void showHideConsole(boolean show) {
		if ((ourFrame == null) || (this.showHide == show))
			return;

		this.showHide = show;
		ggScroll.setVisible(show);
		ourFrame.pack();

	}

	/**
	 * Creates a window containing a <code>ServerPanel</code> for the given server.
	 * Uses default flags (<code>MIMIC</code>).
	 *
	 * @param server
	 *            the server to which the panel shall be connected
	 *
	 * @return a frame containing the panel. This frame is already made visible. The
	 *         default close operation is <code>DO_NOTHING_ON_CLOSE</code>, so you
	 *         will want to attach a <code>WindowListener</code> which deals with
	 *         closing and cleanup.
	 */
	public static JFrame makeWindow(Server server) {
		return makeWindow(server, MIMIC);
	}

	/**
	 * Creates a window with custom flags. See the one argument method for details.
	 *
	 * @param server
	 *            the server to which the panel shall be connected
	 * @param flags
	 *            a mask of flags that define which GUI elements should be created,
	 *            e.g. <code>CONSOLE</code>, <code>BOOTQUIT</code> etc.
	 * 
	 * @see #makeWindow( Server )
	 */
	public static JFrame makeWindow(Server server, int flags) {
		final ServerPanel sp = new ServerPanel(server, flags);
		final JFrame f = new JFrame(server.getName() + " server");
		final Container cp = f.getContentPane();

		sp.ourFrame = f;
		if (sp.ggScroll != null)
			sp.ggScroll.setVisible(false);

		f.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		cp.setLayout(new BorderLayout());
		cp.add(sp, BorderLayout.CENTER);
		f.pack();
		f.setVisible(true);
		f.toFront();

		return f;
	}

	// -------------- ServerListener interface --------------

	/**
	 * This class implements the <code>ServerListener</code> interface to be
	 * notified about the server booting and quitting, and for tracking the status.
	 */
	@Override
	public void serverAction(ServerEvent e) {
		switch (e.getID()) {
		case ServerEvent.RUNNING:
			if (actionBoot != null) {
				actionBoot.booted();
			}
			updateBigName();
			break;

		case ServerEvent.STOPPED:
			if (actionBoot != null) {
				actionBoot.terminated();
			}
			updateBigName();
			updateCounts();
			break;

		case ServerEvent.COUNTS:
			updateCounts();
			break;

		default:
			break;
		}
	}

	// -------------- internal classes --------------

	private class ActionBoot extends AbstractAction {
		private boolean booted;

		protected ActionBoot() {
			super(JavaCollider.getResourceString("buttonBoot"));

			booted = server.isRunning() || server.isBooting();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if (booted) {
					server.quit();
				} else {
					server.boot();
					updateBigName();
					showHideConsole(true);
				}
			} catch (IOException e1) {
				JavaCollider.displayError(enc_this, e1, getValue(NAME).toString());
			}
		}

		protected void terminated() {
			booted = false;
			putValue(NAME, JavaCollider.getResourceString("buttonBoot"));
			showHideConsole(false);
		}

		protected void booted() {
			booted = true;
			putValue(NAME, JavaCollider.getResourceString("buttonQuit"));
		}
	} // class actionBootClass

	private class ActionDump extends AbstractAction {
		private boolean dumping;

		protected ActionDump() {
			super(JavaCollider.getResourceString("buttonDumpOSC"));

			dumping = server.getDumpMode() != kDumpOff;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			final AbstractButton b = (AbstractButton) e.getSource();

			dumping = b.isSelected();
			try {
				server.dumpOSC(dumping ? kDumpText : kDumpOff);
			} catch (IOException e1) {
				JavaCollider.displayError(enc_this, e1, getValue(NAME).toString());
			}
		}
	} // class actionDumpClass
}