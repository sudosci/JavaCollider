/*
 * OSCResponderNode.java
 * (JavaCollider)
 * Copyright (c) 2004-2015 Hanns Holger Rutz. All rights reserved.
 * This software is published under the GNU Lesser General Public License v2.1+
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.jcollider;

import java.io.IOException;
import java.net.SocketAddress;

import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;

/**
 * Similar operation as the SClang class of the same name, but slightly
 * different implementation, based on the new <code>OSCMultiResponder</code>
 * class and <code>NetUtil</code>.
 * <P>
 * <B>As of v0.29</B> the creator signature had to be changed to use a
 * <code>Server</code> instead of a network address, unfortunately (a side
 * effect of using <code>OSCClient</code> in <code>Server</code> and
 * <code>OSCMultiResponder</code>). So you may need to update old code.
 *
 * @author Hanns Holger Rutz
 * @version 0.36, 08-Oct-09
 */
public class OSCResponderNode implements OSCListener {
	private final String cmdName;
	private final Action action;
	private volatile boolean removeWhenDone = false;
	private volatile boolean listening = false;

	private final Object sync;
	private final OSCMultiResponder multi;

	/**
	 * Creates a new responder node to listen for messages coming in from the given
	 * server. Filters out messages different from the specified command name. Upon
	 * reception, the provided OSCListener is invoked.
	 * <p>
	 * After creating the responder, the <code>add</code> method has to be called
	 * separately to actually start the listening process.
	 *
	 * @param s
	 *            server of incoming messages
	 * @param cmdName
	 *            name of the OSC command at whose arrival the action is invoked
	 * @param action
	 *            the action's <code>messageReceived</code> method is called upon
	 *            message reception. Note that just as specified in the
	 *            <code>OSCListener</code> interface, the action should not assume
	 *            to be in any particular thread. The current implementation calls
	 *            the action in the OSC listening thread, but this is not
	 *            guaranteed. Calls to Swing components should be deferred
	 *            appropriately.
	 *
	 * @see Server
	 * @see #add()
	 */
	public OSCResponderNode(final Server s, String cmdName, final OSCListener action) {
		this(s, cmdName, new Action() {
			@Override
			public void respond(OSCResponderNode node, OSCMessage msg, long time) {
				action.messageReceived(msg, s.getAddr(), time);
			}
		});
	}

	/**
	 * Creates a new responder node to listen for messages coming in from the given
	 * server. Filters out messages different from the specified command name. Upon
	 * reception, the provided action is invoked.
	 * <p>
	 * After creating the responder, the <code>add</code> method has to be called
	 * separately to actually start the listening process.
	 *
	 * @param s
	 *            server of incoming messages
	 * @param cmdName
	 *            name of the OSC command at whose arrival the action is invoked
	 * @param action
	 *            the action's <code>respond</code> method is called upon message
	 *            reception. Note that just as specified in the
	 *            <code>OSCListener</code> interface, the action should not assume
	 *            to be in any particular thread. The current implementation calls
	 *            the action in the OSC listening thread, but this is not
	 *            guaranteed. Calls to Swing components should be deferred
	 *            appropriately.
	 *
	 * @see Server
	 * @see #add()
	 */
	public OSCResponderNode(Server s, String cmdName, Action action) {
		this.cmdName = cmdName;
		this.action = action;
		multi = s.getMultiResponder();
		sync = multi.getSync();
	}

	/**
	 * Queries the name which is used as the message filter
	 *
	 * @return the name of the OSC command to which this responder listens
	 */
	public String getCommandName() {
		return cmdName;
	}

	/**
	 * Adds the node to the list of actively listening nodes. If you are uncertain
	 * about the node's state, check <code>isListening</code> first, since this
	 * method will throw an <code>IllegalStateException</code> if you try to add it
	 * twice.
	 *
	 * @return the responder node (for convenience)
	 *
	 * @see #remove()
	 * @see #isListening()
	 *
	 * @throws IllegalStateException
	 *             if the node has already been added
	 */
	public OSCResponderNode add() throws IOException {
		synchronized (sync) {
			if (listening)
				throw new IllegalStateException("OSCResponderNode.add() : duplicate call");
			multi.addNode(this);
			listening = true;
			return this;
		}
	}

	/**
	 * Queries the node's state.
	 *
	 * @return <code>true</code> if the node is active (was added),
	 *         <code>false</code> otherwise (newly created node or removed)
	 */
	public boolean isListening() {
		synchronized (sync) {
			return (listening);
		}
	}

	/**
	 * Tags the node to remove itself after the next unfiltered message arrived. If
	 * the node shall receive exactly one message, a clean code must call this
	 * method before calling the <code>add</code> method.
	 *
	 * @return the responder node (for convenience)
	 */
	public OSCResponderNode removeWhenDone() {
		removeWhenDone = true;
		return this;
	}

	/**
	 * This method is called as part of the implementation of the
	 * <code>OSCListener</code> interface. It dispatches the message to the action.
	 * If <code>removeWhenDone</code> was called, it will remove the node after the
	 * action returns.
	 *
	 * @see #removeWhenDone()
	 */
	@Override
	public void messageReceived(OSCMessage msg, SocketAddress sender, long time) {
		if (listening) {
			try {
				action.respond(this, msg, time);
			} catch (Exception e1) {
				e1.printStackTrace(Server.getPrintStream());
			}
			if (removeWhenDone) {
				remove(); // OSCMultiResponder will take care of thread issues
			}
		}
	}

	/**
	 * Removes the node from the list of actively listening nodes. If the node was
	 * already removed, this method does nothing.
	 *
	 * @return the responder node (for convenience)
	 * @throws IOException
	 *             if there was a problem
	 *
	 * @see #add()
	 */
	public OSCResponderNode remove()
	// throws IOException
	{
		synchronized (sync) {
			listening = false;
			multi.removeNode(this);
			return this;
		}
	}

	// ----------- internal classes -----------

	public interface Action {
		public void respond(OSCResponderNode node, OSCMessage msg, long time);
	}

}