/*
 * OSCresponderNode.java
 * (JavaCollider)
 * Copyright (c) 2004-2015 Hanns Holger Rutz. All rights reserved.
 * This software is published under the GNU Lesser General Public License v2.1+
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.jcollider;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.sciss.net.OSCClient;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;

/**
 * Despite the name, the <code>OSCMultiResponder</code> mimics the SClang
 * counter part only superficially. It absorbs the whole
 * <code>OSCResponder</code> class and is based on the <code>NetUtil</code> OSC
 * library.
 * <p>
 * While the super class <code>OSCReceiver</code> allows only a coarse message
 * filtering, using the simple <code>OSCListener</code> interface, the
 * <code>OSCMultiResponder</code> maintains a map of OSC command names and
 * listeners (<code>OSCResponderNode</code>s) who wish to be informed about only
 * this particular type of messages.
 * <p>
 * When a new node is added using the <code>addNode</code> method, the static
 * list of all multi responders is searched for the given server address. If it
 * exists, the corresponding multi responder is used, otherwise a new multi
 * responder is created. Likewise, when <code>removeNode</code> is called, the
 * multi responder checks if all nodes have been removed, and if so will
 * terminate the OSC receiver.
 * <p>
 * To keep the responder permanently active, the server creates a multi
 * responder for its address upon instantiation.
 *
 * @author Hanns Holger Rutz
 * @version 0.34, 11-Jan-10
 */
public class OSCMultiResponder
		implements OSCListener {
	private final List<OSCResponderNode> allNodes = new ArrayList<>();
	private final Map<String, List<OSCResponderNode>> mapCmdToNodes = new HashMap<>();

	private static final boolean debug = false;

	private final OSCClient c;

	private OSCResponderNode[] resps = new OSCResponderNode[2];
	private final Object sync = new Object();

	/**
	 * Creates a new responder for the given <code>OSCClient</code>. This is done by
	 * the server to create a permanent listener. Users should use the
	 * <code>OSCResponderNode</code> class instead.
	 *
	 * @param c
	 *            the client to who's receiver we should listen
	 *            
	 * @throws IOException
	 *
	 * @see OSCResponderNode
	 */
	protected OSCMultiResponder(final OSCClient c) throws IOException {

		this.c = c;
		
		c.addOSCListener(this);
		
	}

	protected Object getSync() {
		return sync;
	}

	/**
	 * 
	 * @param node
	 * @throws IOException
	 */
	protected void addNode(OSCResponderNode node) throws IOException {
		List<OSCResponderNode> specialNodes;

		synchronized (sync) {
			allNodes.add(node);
			specialNodes = mapCmdToNodes.get(node.getCommandName());
			if (specialNodes == null) {
				specialNodes = new ArrayList<>(4);
				mapCmdToNodes.put(node.getCommandName(), specialNodes);
			}
			specialNodes.add(node);
		}
	}

	protected void removeNode(OSCResponderNode node)
	{
		final List<OSCResponderNode> specialNodes;

		synchronized (sync) {
			specialNodes = mapCmdToNodes.get(node.getCommandName());
			if (specialNodes != null) {
				specialNodes.remove(node);
				allNodes.remove(node);
				
				if (allNodes.isEmpty()) {
					mapCmdToNodes.clear();
					
				}
			}
		}
	}

	protected void dispose() {
		synchronized (sync) {
			c.removeOSCListener(this);
			allNodes.clear();
			mapCmdToNodes.clear();
			if (debug)
				System.err.println("OSCMultiResponder( client = " + c + "; hash = " + hashCode() + " ): dispose");
			c.dispose();
		}
	}

	// ------------ OSCListener interface ------------

	@Override
	public void messageReceived(OSCMessage msg, SocketAddress sender, long time) {
		final List<OSCResponderNode> specialNodes;
		final int numResps;
		final String cmdNameTmp = msg.getName();
		final String cmdName = (cmdNameTmp.charAt(0) == '/') ? cmdNameTmp : "/" + cmdNameTmp;

		synchronized (sync) {
			specialNodes = mapCmdToNodes.get(cmdName);
			if (specialNodes == null)
				return;
			numResps = specialNodes.size();
			resps = specialNodes.toArray(resps);
		}

		for (int i = 0; i < numResps; i++) {
			try {
				resps[i].messageReceived(msg, sender, time);
			} catch (Exception e) {
				e.printStackTrace(Server.getPrintStream());
			}
			resps[i] = null;
		}
	}
}