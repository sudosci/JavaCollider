/*
 * Buffer.java
 * (JavaCollider)
 * Copyright (c) 2004-2015 Hanns Holger Rutz. All rights reserved.
 * This software is published under the GNU Lesser General Public License v2.1+
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.jcollider;

import java.io.IOException;
import java.io.PrintStream;

import de.sciss.net.OSCBundle;
import de.sciss.net.OSCMessage;

/**
 * Mimics SCLang's Buffer class, that is, it's a client side representation of
 * an audio buffer
 *
 * @warning this is a quick direct translation from SClang which is largely
 *          untested. before all methods have been thoroughly verified, excepted
 *          some of them to be wrong or behave different than expected. what
 *          certainly works is alloc-, read-, free-, zero- and close-messages
 *
 * @author Hanns Holger Rutz
 * @version 0.32, 25-Feb-08
 *
 * @todo custom fill commands (sine1 etc.)
 */
public class Buffer implements Constants {
	private final Server server;
	private final int bufNum;

	private int numFrames;
	private int numChannels;
	private double sampleRate;

	private String path = null;
	private CompletionAction doOnInfo = null;

	/**
	 * Creates a new Buffer with given number of frames and channels. This method
	 * uses the Server's allocators but does not send an <code>allocMsg</code> to
	 * the server.
	 *
	 * @param server
	 *            the server to which the buffer belongs
	 * @param numFrames
	 *            the number of frames (samples per channel) that buffer occupies
	 * @param numChannels
	 *            the number of channels the buffer occupies
	 *
	 * @see #alloc()
	 *
	 * @warning when the allocators are exhausted, the <code>bufNum</code> of the
	 *          returned <code>Buffer</code> instance is <code>-1</code>. The caller
	 *          is responsible for dealing with this possible failure.
	 */
	public Buffer(Server server, int numFrames, int numChannels) {
		this(server, numFrames, numChannels, server.getBufferAllocator().alloc(1));
	}

	/**
	 * Creates a new Buffer with given number of frames and channels. This method
	 * requires an explicit buffer index and does not use the Server's allocators.
	 * This method does not send an <code>allocMsg</code> to the server.
	 *
	 * @param server
	 *            the server to which the buffer belongs
	 * @param numFrames
	 *            the number of frames (samples per channel) that buffer occupies
	 * @param numChannels
	 *            the number of channels the buffer occupies
	 * @param bufNum
	 *            the index of the buffer
	 */
	public Buffer(Server server, int numFrames, int numChannels, int bufNum) {
		this.server = server;
		this.bufNum = bufNum;
		this.numFrames = numFrames;
		this.numChannels = numChannels;
		this.sampleRate = server.getSampleRate();
	}

	private Buffer(Server server, int bufNum) {
		this.server = server;
		this.bufNum = bufNum;
	}

	/**
	 * Queries the server at which the buffer resides
	 *
	 * @return the buffer's <code>Server</code>
	 */
	public Server getServer() {
		return server;
	}

	/**
	 * Queries the buffer data's sample rate. This rate is updated when an
	 * asynchronous read command is completed and buffer info has been received from
	 * the server.
	 *
	 * @return the buffer content's sample rate
	 */
	public double getSampleRate() {
		return sampleRate;
	}

	protected void setSampleRate(double sampleRate) {
		this.sampleRate = sampleRate;
	}

	/**
	 * Queries the buffer's index.
	 *
	 * @return the buffer index
	 */
	public int getBufNum() {
		return bufNum;
	}

	/**
	 * Queries the buffer data's number of frames (samples per channel). In
	 * ansynchronous allocation by read this value is filled in when the read is
	 * completed and buffer info has been received from the server.
	 *
	 * @return the number of frames in the buffer
	 */
	public int getNumFrames() {
		return numFrames;
	}

	protected void setNumFrames(int numFrames) {
		this.numFrames = numFrames;
	}

	/**
	 * Queries the buffer's number of channels. In ansynchronous allocation by read
	 * this value is filled in when the read is completed and buffer info has been
	 * received from the server.
	 *
	 * @return the number of channels in the buffer
	 */
	public int getNumChannels() {
		return numChannels;
	}

	protected void setNumChannels(int numChannels) {
		this.numChannels = numChannels;
	}

	/**
	 * Queries the buffer's sound file path. In ansynchronous allocation by read
	 * this value is instantely filled in.
	 *
	 * @return the sound file path of the buffer or <code>null</code>
	 */
	public String getPath() {
		return path;
	}

	private void setPath(String path) {
		this.path = path;
	}

	/**
	 * Queries the buffer's duration in seconds. This is only valid after the buffer
	 * info (such as sampleRate) has been retrieved.
	 * 
	 * @return the total duration of the buffer (when played back at normal rate) in
	 *         seconds
	 */
	public double getDuration() {
		return getNumFrames() / getSampleRate();
	}

	@Override
	public String toString() {
		return ("Buffer(" + getBufNum() + ", " + getNumFrames() + ", " + getNumChannels() + ", " + getSampleRate()
				+ ", " + getPath() + ")");
	}

	/**
	 * Allocates and returns a new mono Buffer with given number of frames.
	 *
	 * @param server
	 *            the server to which the buffer belongs
	 * @param numFrames
	 *            the number of frames (samples per channel) that buffer occupies
	 * @return the newly created Buffer or <code>null</code> if the server's buffer
	 *         allocator is exhausted
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 */
	public static Buffer alloc(Server server, int numFrames) throws IOException {
		return Buffer.alloc(server, numFrames, 1);
	}

	/**
	 * Allocates and returns a new Buffer with given number of channels and frames.
	 *
	 * @param server
	 *            the server to which the buffer belongs
	 * @param numFrames
	 *            the number of frames (samples per channel) that buffer occupies
	 * @param numChannels
	 *            the number of channels the buffer occupies
	 * @return the newly created Buffer or <code>null</code> if the server's buffer
	 *         allocator is exhausted
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 */
	public static Buffer alloc(Server server, int numFrames, int numChannels) throws IOException {
		return Buffer.alloc(server, numFrames, numChannels, null);
	}

	/**
	 * Allocates and returns a new Buffer with given number of channels and frames.
	 *
	 * @param server
	 *            the server to which the buffer belongs
	 * @param numFrames
	 *            the number of frames (samples per channel) that buffer occupies
	 * @param numChannels
	 *            the number of channels the buffer occupies
	 * @param completionFunc
	 *            a function that returns an <code>OSCMessage</code> which is
	 *            processed by the server when the reading is complete. can be
	 *            <code>null</code>.
	 * @return the newly created Buffer or <code>null</code> if the server's buffer
	 *         allocator is exhausted
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 */
	public static Buffer alloc(Server server, int numFrames, int numChannels, CompletionFunction completionFunc)
			throws IOException {
		final int bufNum = server.getBufferAllocator().alloc(1);

		if (bufNum == -1) {
			Server.getPrintStream()
					.println("Buffer.alloc: failed to get a buffer allocated. " + "; server: " + server.getName());
			return null;
		} else {
			return Buffer.alloc(server, numFrames, numChannels, completionFunc, bufNum);
		}
	}

	/**
	 * Allocates and returns a new Buffer with given number of channels and frames.
	 * This uses an explicitly provided buffer index and not the server's allocator.
	 *
	 * @param server
	 *            the server to which the buffer belongs
	 * @param numFrames
	 *            the number of frames (samples per channel) that buffer occupies
	 * @param numChannels
	 *            the number of channels the buffer occupies
	 * @param completionFunc
	 *            a function that returns an <code>OSCMessage</code> which is
	 *            processed by the server when the reading is complete. can be
	 *            <code>null</code>.
	 * @param bufNum
	 *            the index by which the buffer is known on the server
	 * @return the newly created Buffer
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 */
	public static Buffer alloc(Server server, int numFrames, int numChannels, CompletionFunction completionFunc,
			int bufNum) throws IOException {
		final Buffer buf = new Buffer(server, numFrames, numChannels, bufNum); // sets sample rate

		buf.alloc(completionFunc == null ? null : completionFunc.completion(buf));
		return buf;
	}

	/**
	 * Allocates and returns an array of neighbouring single channel
	 * <code>Buffer</code> objects with given number of frames. Neighbouring means,
	 * that <code>buf[ n ].getBufNum() == buf[ n-1 ].getBufNum() + 1</code>.
	 *
	 * @param numBufs
	 *            the number of buffers to allocate
	 * @param server
	 *            the server to which the buffer belongs
	 * @param numFrames
	 *            the number of frames (samples per channel) that buffer occupies
	 * @return the newly created Buffers
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 */
	public static Buffer[] allocConsecutive(int numBufs, Server server, int numFrames) throws IOException {
		return Buffer.allocConsecutive(numBufs, server, numFrames, 1);
	}

	/**
	 * Allocates and returns an array of neighbouring <code>Buffer</code> objects
	 * with given number of channels and frames. Neighbouring means, that
	 * <code>buf[ n ].getBufNum() == buf[ n-1 ].getBufNum() + 1</code>.
	 *
	 * @param numBufs
	 *            the number of buffers to allocate
	 * @param server
	 *            the server to which the buffer belongs
	 * @param numFrames
	 *            the number of frames (samples per channel) that buffer occupies
	 * @param numChannels
	 *            the number of channels the buffer occupies
	 * @return the newly created Buffers
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 */
	public static Buffer[] allocConsecutive(int numBufs, Server server, int numFrames, int numChannels)
			throws IOException {
		return Buffer.allocConsecutive(numBufs, server, numFrames, numChannels, null);
	}

	/**
	 * Allocates and returns an array of neighbouring <code>Buffer</code> objects
	 * with given number of channels and frames. Neighbouring means, that
	 * <code>buf[ n ].getBufNum() == buf[ n-1 ].getBufNum() + 1</code>.
	 *
	 * @param numBufs
	 *            the number of buffers to allocate
	 * @param server
	 *            the server to which the buffer belongs
	 * @param numFrames
	 *            the number of frames (samples per channel) that buffer occupies
	 * @param numChannels
	 *            the number of channels the buffer occupies
	 * @param completionFunc
	 *            a function that returns an <code>OSCMessage</code> which is
	 *            processed by the server when the reading is complete. can be
	 *            <code>null</code>.
	 * @return the newly created Buffers
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 */
	public static Buffer[] allocConsecutive(int numBufs, Server server, int numFrames, int numChannels,
			CompletionFunction completionFunc) throws IOException {
		final int bufNum = server.getBufferAllocator().alloc(numBufs);

		if (bufNum == -1) {
			Server.getPrintStream().println("Buffer.alloc: failed to get " + numBufs + " buffers allocated. "
					+ "; server: " + server.getName());
			return null;
		} else {
			return Buffer.allocConsecutive(numBufs, server, numFrames, numChannels, completionFunc, bufNum);
		}
	}

	/**
	 * Allocates and returns an array of neighbouring <code>Buffer</code> objects
	 * with given number of channels and frames. This uses an explicitly provided
	 * buffer index and not the server's allocator. The first element of the
	 * returned array has a buffer index equal to the provided <code>bufNum</code>,
	 * the next element has a buffer index of <code>bufNum + 1</code>, the next
	 * element an index of <code>bufNum + 2</code> etc.
	 *
	 * @param numBufs
	 *            the number of buffers to allocate
	 * @param server
	 *            the server to which the buffer belongs
	 * @param numFrames
	 *            the number of frames (samples per channel) that buffer occupies
	 * @param numChannels
	 *            the number of channels the buffer occupies
	 * @param completionFunc
	 *            a function that returns an <code>OSCMessage</code> which is
	 *            processed by the server when the reading is complete. can be
	 *            <code>null</code>.
	 * @param bufNum
	 *            the index by which the first buffer is known on the server. the
	 *            consecutive buffers have indices of
	 *            <code>bufNum + (1...numBufs-1)</code>
	 * @return the newly created Buffers
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 */
	public static Buffer[] allocConsecutive(int numBufs, Server server, int numFrames, int numChannels,
			CompletionFunction completionFunc, int bufNum) throws IOException {
		final Buffer[] bufs = new Buffer[numBufs];
		Buffer buf;
		boolean success = false;
		try {
			for (int i = 0; i < numBufs; i++) {
				buf = new Buffer(server, numFrames, numChannels, bufNum + i);
				buf.alloc(completionFunc == null ? null : completionFunc.completion(buf));
				bufs[i] = buf;
			}
			success = true;
			return bufs;
		} finally {
			if (!success) {
				for (int i = 0; i < numBufs; i++) {
					if (bufs[i] != null) {
						try {
							bufs[i].free();
						} catch (IOException e1) {
							/* ignored */ }
					}
				}
			}
		}
	}

	/**
	 * Allocates the buffer created with the basic <code>new</code> constructor.
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see #Buffer( Server, int, int )
	 */
	public void alloc() throws IOException {
		alloc(null);
	}

	/**
	 * Allocates the buffer created with the basic <code>new</code> constructor.
	 *
	 * @param completionMsg
	 *            an <code>OSCMessage</code> which is processed by the server when
	 *            the allocation is complete. can be <code>null</code>.
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see #Buffer( Server, int, int )
	 */
	public void alloc(OSCMessage completionMsg) throws IOException {
		getServer().sendMsg(allocMsg(completionMsg));
	}

	/**
	 * Creates an OSC <code>/b_alloc</code> message to allocate the buffer created
	 * with the basic <code>new</code> constructor.
	 *
	 * @return the message to be sent to the server
	 *
	 * @see #Buffer( Server, int, int )
	 */
	public OSCMessage allocMsg() throws IOException {
		return allocMsg(null);
	}

	/**
	 * Creates an OSC <code>/b_alloc</code> message to allocate the buffer created
	 * with the basic <code>new</code> constructor.
	 *
	 * @param completionMsg
	 *            an <code>OSCMessage</code> which is processed by the server when
	 *            the allocation is complete. can be <code>null</code>.
	 * @return the message to be sent to the server
	 *
	 * @see #Buffer( Server, int, int )
	 */
	public OSCMessage allocMsg(OSCMessage completionMsg) throws IOException {
		addToServerArray();

		final Object[] args = completionMsg == null
				? new Object[] { new Integer(getBufNum()), new Integer(getNumFrames()), new Integer(getNumChannels()) }
				: new Object[] { new Integer(getBufNum()), new Integer(getNumFrames()), new Integer(getNumChannels()),
						completionMsg };

		return (new OSCMessage("/b_alloc", args));
	}

	/**
	 * Allocates the buffer created with the basic <code>new</code> constructor, by
	 * reading in a sound file.
	 *
	 * @param aPath
	 *            the path to the sound file
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see #Buffer( Server, int, int )
	 */
	public void allocRead(String aPath) throws IOException {
		allocRead(aPath, 0);
	}

	/**
	 * Allocates the buffer created with the basic <code>new</code> constructor, by
	 * reading in a sound file.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param startFrame
	 *            starting frame in the sound file
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see #Buffer( Server, int, int )
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void allocRead(String aPath, long startFrame) throws IOException {
		allocRead(aPath, startFrame, -1);
	}

	/**
	 * Allocates the buffer created with the basic <code>new</code> constructor, by
	 * reading in a sound file.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param startFrame
	 *            starting frame in the sound file
	 * @param nFrames
	 *            the number of frames to read, which equals the number of frames
	 *            allocated for the buffer
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see #Buffer( Server, int, int )
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void allocRead(String aPath, long startFrame, int nFrames) throws IOException {
		allocRead(aPath, startFrame, nFrames, null);
	}

	/**
	 * Allocates the buffer created with the basic <code>new</code> constructor, by
	 * reading in a sound file.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param startFrame
	 *            starting frame in the sound file
	 * @param nFrames
	 *            the number of frames to read, which equals the number of frames
	 *            allocated for the buffer
	 * @param completionMsg
	 *            an <code>OSCMessage</code> which is processed by the server when
	 *            the allocation and reading is complete. can be <code>null</code>.
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see #Buffer( Server, int, int )
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void allocRead(String aPath, long startFrame, int nFrames, OSCMessage completionMsg) throws IOException {
		getServer().sendMsg(allocReadMsg(aPath, startFrame, nFrames, completionMsg));
	}

	/**
	 * Allocates the buffer created with the basic <code>new</code> constructor, by
	 * reading in selected channels of a sound file.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param startFrame
	 *            starting frame in the sound file
	 * @param nFrames
	 *            the number of frames to read, which equals the number of frames
	 *            allocated for the buffer
	 * @param channels
	 *            an array of channel indices to read (starting from <code>0</code>)
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void allocReadChannel(String aPath, long startFrame, int nFrames, int[] channels) throws IOException {
		allocReadChannel(aPath, startFrame, nFrames, channels, null);
	}

	/**
	 * Allocates the buffer created with the basic <code>new</code> constructor, by
	 * reading in selected channels of a sound file.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param startFrame
	 *            starting frame in the sound file
	 * @param nFrames
	 *            the number of frames to read, which equals the number of frames
	 *            allocated for the buffer
	 * @param channels
	 *            an array of channel indices to read (starting from <code>0</code>)
	 * @param completionMsg
	 *            an <code>OSCMessage</code> which is processed by the server when
	 *            the allocation and reading is complete. can be <code>null</code>.
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void allocReadChannel(String aPath, long startFrame, int nFrames, int[] channels, OSCMessage completionMsg)
			throws IOException {
		getServer().sendMsg(allocReadChannelMsg(aPath, startFrame, nFrames, channels, completionMsg));
	}

	/**
	 * Creates an OSC <code>/b_allocRead</code> message to allocate the buffer
	 * created with the basic <code>new</code> constructor, by reading in a sound
	 * file.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @return the message to be sent to the server
	 *
	 * @see #Buffer( Server, int, int )
	 */
	public OSCMessage allocReadMsg(String aPath) throws IOException {
		return allocReadMsg(aPath, 0);
	}

	/**
	 * Creates an OSC <code>/b_allocRead</code> message to allocate the buffer
	 * created with the basic <code>new</code> constructor, by reading in a sound
	 * file.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param startFrame
	 *            starting frame in the sound file
	 * @return the message to be sent to the server
	 *
	 * @see #Buffer( Server, int, int )
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public OSCMessage allocReadMsg(String aPath, long startFrame) throws IOException {
		return allocReadMsg(aPath, startFrame, -1);
	}

	/**
	 * Creates an OSC <code>/b_allocRead</code> message to allocate the buffer
	 * created with the basic <code>new</code> constructor, by reading in a sound
	 * file.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param startFrame
	 *            starting frame in the sound file
	 * @param nFrames
	 *            the number of frames to read, which equals the number of frames
	 *            allocated for the buffer
	 * @return the message to be sent to the server
	 *
	 * @see #Buffer( Server, int, int )
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public OSCMessage allocReadMsg(String aPath, long startFrame, int nFrames) throws IOException {
		return allocReadMsg(aPath, startFrame, nFrames, null);
	}

	/**
	 * Creates an OSC <code>/b_allocRead</code> message to allocate the buffer
	 * created with the basic <code>new</code> constructor, by reading in a sound
	 * file.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param startFrame
	 *            starting frame in the sound file
	 * @param nFrames
	 *            the number of frames to read, which equals the number of frames
	 *            allocated for the buffer
	 * @param completionMsg
	 *            an <code>OSCMessage</code> which is processed by the server when
	 *            the allocation and reading is complete. can be <code>null</code>.
	 * @return the message to be sent to the server
	 *
	 * @see #Buffer( Server, int, int )
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public OSCMessage allocReadMsg(String aPath, long startFrame, int nFrames, OSCMessage completionMsg)
			throws IOException {
		addToServerArray();

		if (startFrame > 0x7FFFFFFFL) {
			Server.getPrintStream().println("Buffer.allocReadMsg : startFrame (" + startFrame + ") exceeds 32bit int");
		}

		setPath(aPath);

		final Object[] args = completionMsg == null
				? new Object[] { new Integer(getBufNum()), aPath, new Long(startFrame), new Integer(nFrames) }
				: new Object[] { new Integer(getBufNum()), aPath, new Long(startFrame), new Integer(nFrames),
						completionMsg };

		return (new OSCMessage("/b_allocRead", args));
	}

	/**
	 * Creates an OSC <code>/b_allocReadChannel</code> message to allocate the
	 * buffer created with the basic <code>new</code> constructor, by reading in
	 * selected channels from a sound file.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param startFrame
	 *            starting frame in the sound file
	 * @param nFrames
	 *            the number of frames to read, which equals the number of frames
	 *            allocated for the buffer
	 * @param channels
	 *            an array of channel indices to read (starting from <code>0</code>)
	 * @return the message to be sent to the server
	 *
	 * @see #Buffer( Server, int, int )
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public OSCMessage allocReadChannelMsg(String aPath, long startFrame, int nFrames, int[] channels)
			throws IOException {
		return allocReadChannelMsg(aPath, startFrame, nFrames, channels, null);
	}

	/**
	 * Creates an OSC <code>/b_allocReadChannel</code> message to allocate the
	 * buffer created with the basic <code>new</code> constructor, by reading in
	 * selected channels from a sound file.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param startFrame
	 *            starting frame in the sound file
	 * @param nFrames
	 *            the number of frames to read, which equals the number of frames
	 *            allocated for the buffer
	 * @param channels
	 *            an array of channel indices to read (starting from <code>0</code>)
	 * @param completionMsg
	 *            an <code>OSCMessage</code> which is processed by the server when
	 *            the allocation and reading is complete. can be <code>null</code>.
	 * @return the message to be sent to the server
	 *
	 * @see #Buffer( Server, int, int )
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public OSCMessage allocReadChannelMsg(String aPath, long startFrame, int nFrames, int[] channels,
			OSCMessage completionMsg) throws IOException {
		addToServerArray();

		if (startFrame > 0x7FFFFFFFL) {
			Server.getPrintStream()
					.println("Buffer.allocReadChannelMsg : startFrame (" + startFrame + ") exceeds 32bit int");
		}

		setPath(aPath);

		final Object[] args = new Object[4 + channels.length + (completionMsg == null ? 0 : 1)];
		args[0] = new Integer(getBufNum());
		args[1] = aPath;
		args[2] = new Long(startFrame);
		args[3] = new Integer(nFrames);
		for (int i = 4, j = 0; j < channels.length;) {
			args[i++] = new Integer(channels[j++]);
		}
		if (completionMsg != null)
			args[args.length - 1] = completionMsg;
		return (new OSCMessage("/b_allocReadChannel", args));
	}

	/**
	 * Reads a whole file into memory for PlayBuf etc. adds a query as a completion
	 * message.
	 *
	 * @param server
	 *            the server on which the buffer is allocated
	 * @param path
	 *            the path to the sound file
	 *
	 * @return the newly created Buffer or <code>null</code> if the server's buffer
	 *         allocator is exhausted
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 */
	public static Buffer read(Server server, String path) throws IOException {
		return Buffer.read(server, path, 0);
	}

	/**
	 * Reads a whole file into memory for PlayBuf etc., starting at a given frame.
	 * adds a query as a completion message.
	 *
	 * @param server
	 *            the server on which the buffer is allocated
	 * @param path
	 *            the path to the sound file
	 * @param startFrame
	 *            the frame index in the sound file to start reading from
	 *
	 * @return the newly created Buffer or <code>null</code> if the server's buffer
	 *         allocator is exhausted
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public static Buffer read(Server server, String path, long startFrame) throws IOException {
		return Buffer.read(server, path, startFrame, -1);
	}

	/**
	 * Reads a section of a file into memory for PlayBuf etc. adds a query as a
	 * completion message.
	 *
	 * @param server
	 *            the server on which the buffer is allocated
	 * @param path
	 *            the path to the sound file
	 * @param startFrame
	 *            the frame index in the sound file to start reading from
	 * @param numFrames
	 *            the number of frames to read. this is equal to the number of
	 *            frames allocated for the buffer
	 *
	 * @return the newly created Buffer or <code>null</code> if the server's buffer
	 *         allocator is exhausted
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public static Buffer read(Server server, String path, long startFrame, int numFrames) throws IOException {
		return Buffer.read(server, path, startFrame, numFrames, null);
	}

	/**
	 * Reads a section of a file into memory for PlayBuf etc. adds a query as a
	 * completion message.
	 *
	 * @param server
	 *            the server on which the buffer is allocated
	 * @param path
	 *            the path to the sound file
	 * @param startFrame
	 *            the frame index in the sound file to start reading from
	 * @param numFrames
	 *            the number of frames to read. this is equal to the number of
	 *            frames allocated for the buffer
	 * @param action
	 *            an action to be executed when the <code>/b_info</code> message
	 *            comes back from the server. at this time the buffer has been
	 *            allocated and filled. <code>action</code> can be <code>null</code>
	 *
	 * @return the newly created Buffer or <code>null</code> if the server's buffer
	 *         allocator is exhausted
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public static Buffer read(Server server, String path, long startFrame, int numFrames, CompletionAction action)
			throws IOException {
		final int bufNum = server.getBufferAllocator().alloc(1);

		if (bufNum == -1) {
			Server.getPrintStream()
					.println("Buffer.read: failed to get a buffer allocated. " + "; server: " + server.getName());
			return null;
		} else {
			return Buffer.read(server, path, startFrame, numFrames, action, bufNum);
		}
	}

	/**
	 * Reads a section of a file into memory for PlayBuf etc. adds a query as a
	 * completion message. An explicit buffer index is provided.
	 *
	 * @param server
	 *            the server on which the buffer is allocated
	 * @param path
	 *            the path to the sound file
	 * @param startFrame
	 *            the frame index in the sound file to start reading from
	 * @param numFrames
	 *            the number of frames to read. this is equal to the number of
	 *            frames allocated for the buffer
	 * @param action
	 *            an action to be executed when the <code>/b_info</code> message
	 *            comes back from the server. at this time the buffer has been
	 *            allocated and filled. <code>action</code> can be <code>null</code>
	 * @param bufNum
	 *            the index to use for the buffer
	 *
	 * @return the newly created buffer
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public static Buffer read(Server server, String path, long startFrame, int numFrames, CompletionAction action,
			int bufNum) throws IOException {
		final Buffer buf = new Buffer(server, bufNum);

		buf.setDoOnInfo(action);
		buf.waitForBufInfo();
		buf.allocRead(path, startFrame, numFrames, buf.queryMsg());
		return buf;
	}

	/**
	 * Reads a section of a given set of channels from a file into memory for
	 * PlayBuf etc. adds a query as a completion message.
	 *
	 * @param server
	 *            the server on which the buffer is allocated
	 * @param path
	 *            the path to the sound file
	 * @param startFrame
	 *            the frame index in the sound file to start reading from
	 * @param numFrames
	 *            the number of frames to read. this is equal to the number of
	 *            frames allocated for the buffer. Use <code>-1</code> to read the
	 *            whole file.
	 * @param channels
	 *            an array of channel indices to read (starting from <code>0</code>)
	 *
	 * @return the newly created buffer
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public static Buffer readChannel(Server server, String path, long startFrame, int numFrames, int[] channels)
			throws IOException {
		return Buffer.readChannel(server, path, startFrame, numFrames, channels, null);
	}

	/**
	 * Reads a section of a given set of channels from a file into memory for
	 * PlayBuf etc. adds a query as a completion message.
	 *
	 * @param server
	 *            the server on which the buffer is allocated
	 * @param path
	 *            the path to the sound file
	 * @param startFrame
	 *            the frame index in the sound file to start reading from
	 * @param numFrames
	 *            the number of frames to read. this is equal to the number of
	 *            frames allocated for the buffer
	 * @param channels
	 *            an array of channel indices to read (starting from <code>0</code>)
	 * @param action
	 *            an action to be executed when the <code>/b_info</code> message
	 *            comes back from the server. at this time the buffer has been
	 *            allocated and filled. <code>action</code> can be <code>null</code>
	 *
	 * @return the newly created buffer
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public static Buffer readChannel(Server server, String path, long startFrame, int numFrames, int[] channels,
			CompletionAction action) throws IOException {
		final int bufNum = server.getBufferAllocator().alloc(1);

		if (bufNum == -1) {
			Server.getPrintStream().println(
					"Buffer.readChannel: failed to get a buffer allocated. " + "; server: " + server.getName());
			return null;
		} else {
			return Buffer.readChannel(server, path, startFrame, numFrames, channels, action, bufNum);
		}
	}

	/**
	 * Reads a section of a given set of channels from a file into memory for
	 * PlayBuf etc. adds a query as a completion message. An explicit buffer index
	 * is provided.
	 *
	 * @param server
	 *            the server on which the buffer is allocated
	 * @param path
	 *            the path to the sound file
	 * @param startFrame
	 *            the frame index in the sound file to start reading from
	 * @param numFrames
	 *            the number of frames to read. this is equal to the number of
	 *            frames allocated for the buffer
	 * @param channels
	 *            an array of channel indices to read (starting from <code>0</code>)
	 * @param action
	 *            an action to be executed when the <code>/b_info</code> message
	 *            comes back from the server. at this time the buffer has been
	 *            allocated and filled. <code>action</code> can be <code>null</code>
	 * @param bufNum
	 *            the index to use for the buffer
	 *
	 * @return the newly created buffer
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public static Buffer readChannel(Server server, String path, long startFrame, int numFrames, int[] channels,
			CompletionAction action, int bufNum) throws IOException {
		final Buffer buf = new Buffer(server, bufNum);

		buf.setDoOnInfo(action);
		buf.waitForBufInfo();
		buf.allocReadChannel(path, startFrame, numFrames, channels, buf.queryMsg());
		return buf;
	}

	/**
	 * Reads in as many frames from a sound file as fit into the buffer, starting at
	 * the beginning of the file. Closes the file after reading.
	 *
	 * @param aPath
	 *            the path to the sound file
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 */
	public void read(String aPath) throws IOException {
		read(aPath, 0);
	}

	/**
	 * Reads in as many frames from a sound file as fit into the buffer, starting at
	 * a given frame in the file. Closes the file after reading.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void read(String aPath, long fileStartFrame) throws IOException {
		read(aPath, fileStartFrame, -1);
	}

	/**
	 * Reads in frames from a sound file into the buffer. Closes the file after
	 * reading.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void read(String aPath, long fileStartFrame, int nFrames) throws IOException {
		read(aPath, fileStartFrame, nFrames, 0);
	}

	/**
	 * Reads in frames from a sound file into the buffer, beginning a given offset
	 * in the buffer. Closes the file after reading.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 * @param bufStartFrame
	 *            the offset (in frames) in the buffer at which the filling begins
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void read(String aPath, long fileStartFrame, int nFrames, int bufStartFrame) throws IOException {
		read(aPath, fileStartFrame, nFrames, bufStartFrame, false);
	}

	/**
	 * Reads in frames from a sound file into the buffer, beginning a given offset
	 * in the buffer.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 * @param bufStartFrame
	 *            the offset (in frames) in the buffer at which the filling begins
	 * @param leaveOpen
	 *            <code>false</code> to close the sound file after reading,
	 *            <code>true</code> to leave it open (as required for a
	 *            <code>DiskIn</code> UGen). If you leave the file open, don't
	 *            forget to call <code>close</code> on the buffer eventually.
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void read(String aPath, long fileStartFrame, int nFrames, int bufStartFrame, boolean leaveOpen)
			throws IOException {
		read(aPath, fileStartFrame, nFrames, bufStartFrame, leaveOpen, null);
	}

	/**
	 * Reads in frames from a sound file into the buffer, beginning a given offset
	 * in the buffer.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 * @param bufStartFrame
	 *            the offset (in frames) in the buffer at which the filling begins
	 * @param leaveOpen
	 *            <code>false</code> to close the sound file after reading,
	 *            <code>true</code> to leave it open (as required for a
	 *            <code>DiskIn</code> UGen). If you leave the file open, don't
	 *            forget to call <code>close</code> on the buffer eventually.
	 * @param action
	 *            an action to be executed when the <code>/b_info</code> comes back
	 *            from the server. at this moment, the read operation is completed.
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void read(String aPath, long fileStartFrame, int nFrames, int bufStartFrame, boolean leaveOpen,
			CompletionAction action) throws IOException {
		addToServerArray();
		setDoOnInfo(action);
		waitForBufInfo();
		getServer().sendMsg(readMsg(aPath, fileStartFrame, nFrames, bufStartFrame, leaveOpen, queryMsg()));
	}

	/**
	 * Reads in frames from selected channels of a sound file into the buffer,
	 * beginning a given offset in the buffer.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 * @param bufStartFrame
	 *            the offset (in frames) in the buffer at which the filling begins
	 * @param leaveOpen
	 *            <code>false</code> to close the sound file after reading,
	 *            <code>true</code> to leave it open (as required for a
	 *            <code>DiskIn</code> UGen). If you leave the file open, don't
	 *            forget to call <code>close</code> on the buffer eventually.
	 * @param channels
	 *            an array of channel indices to read (starting from <code>0</code>)
	 * @param action
	 *            an action to be executed when the <code>/b_info</code> comes back
	 *            from the server. at this moment, the read operation is completed.
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void readChannel(String aPath, long fileStartFrame, int nFrames, int bufStartFrame, boolean leaveOpen,
			int[] channels, CompletionAction action) throws IOException {
		addToServerArray();
		setDoOnInfo(action);
		waitForBufInfo();
		getServer().sendMsg(
				readChannelMsg(aPath, fileStartFrame, nFrames, bufStartFrame, leaveOpen, channels, queryMsg()));
	}

	/**
	 * Creates an OSC <code>/b_read</code> message to read in as many frames from a
	 * sound file as fit into the buffer, starting at the beginning of the file,
	 * closing the file after reading.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @return the message to be sent to the server
	 */
	public OSCMessage readMsg(String aPath) {
		return readMsg(aPath, 0);
	}

	/**
	 * Creates an OSC <code>/b_read</code> message to read in as many frames from a
	 * sound file as fit into the buffer, starting at a given frame in the file,
	 * closing the file after reading.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @return the message to be sent to the server
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public OSCMessage readMsg(String aPath, long fileStartFrame) {
		return readMsg(aPath, fileStartFrame, -1);
	}

	/**
	 * Creates an OSC <code>/b_read</code> message to read in frames from a sound
	 * file into the buffer, closing the file after reading.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 * @return the message to be sent to the server
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public OSCMessage readMsg(String aPath, long fileStartFrame, int nFrames) {
		return readMsg(aPath, fileStartFrame, nFrames, 0);
	}

	/**
	 * Creates an OSC <code>/b_read</code> message to read in frames from a sound
	 * file into the buffer, beginning a given offset in the buffer, closing the
	 * file after reading.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 * @param bufStartFrame
	 *            the offset (in frames) in the buffer at which the filling begins
	 * @return the message to be sent to the server
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public OSCMessage readMsg(String aPath, long fileStartFrame, int nFrames, int bufStartFrame) {
		return readMsg(aPath, fileStartFrame, nFrames, bufStartFrame, false);
	}

	/**
	 * Creates an OSC <code>/b_read</code> message to read in frames from a sound
	 * file into the buffer, beginning a given offset in the buffer.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 * @param bufStartFrame
	 *            the offset (in frames) in the buffer at which the filling begins
	 * @param leaveOpen
	 *            <code>false</code> to close the sound file after reading,
	 *            <code>true</code> to leave it open (as required for a
	 *            <code>DiskIn</code> UGen). If you leave the file open, don't
	 *            forget to call <code>close</code> on the buffer eventually.
	 * @return the message to be sent to the server
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public OSCMessage readMsg(String aPath, long fileStartFrame, int nFrames, int bufStartFrame, boolean leaveOpen) {
		return readMsg(aPath, fileStartFrame, nFrames, bufStartFrame, leaveOpen, null);
	}

	/**
	 * Creates an OSC <code>/b_read</code> message to read in frames from a sound
	 * file into the buffer, beginning a given offset in the buffer.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 * @param bufStartFrame
	 *            the offset (in frames) in the buffer at which the filling begins
	 * @param leaveOpen
	 *            <code>false</code> to close the sound file after reading,
	 *            <code>true</code> to leave it open (as required for a
	 *            <code>DiskIn</code> UGen). If you leave the file open, don't
	 *            forget to call <code>close</code> on the buffer eventually.
	 * @param completionMsg
	 *            an <code>OSCMessage</code> which is processed by the server when
	 *            the reading is complete. can be <code>null</code>.
	 * @return the message to be sent to the server
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public OSCMessage readMsg(String aPath, long fileStartFrame, int nFrames, int bufStartFrame, boolean leaveOpen,
			OSCMessage completionMsg) {
		if (fileStartFrame > 0x7FFFFFFFL) {
			Server.getPrintStream()
					.println("Buffer.readMsg : fileStartFrame (" + fileStartFrame + ") exceeds 32bit int");
		}

		setPath(aPath);

		final Object[] args = completionMsg == null
				? new Object[] { new Integer(getBufNum()), aPath, new Long(fileStartFrame), new Integer(nFrames),
						new Integer(bufStartFrame), new Integer(leaveOpen ? 1 : 0) }
				: new Object[] { new Integer(getBufNum()), aPath, new Long(fileStartFrame), new Integer(nFrames),
						new Integer(bufStartFrame), new Integer(leaveOpen ? 1 : 0), completionMsg };

		return (new OSCMessage("/b_read", args));
		// doesn't set my numChannels etc.
	}

	/**
	 * Creates an OSC <code>/b_read</code> message to read in frames from selected
	 * channels of a sound file into the buffer, beginning a given offset in the
	 * buffer.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 * @param bufStartFrame
	 *            the offset (in frames) in the buffer at which the filling begins
	 * @param leaveOpen
	 *            <code>false</code> to close the sound file after reading,
	 *            <code>true</code> to leave it open (as required for a
	 *            <code>DiskIn</code> UGen). If you leave the file open, don't
	 *            forget to call <code>close</code> on the buffer eventually.
	 * @param channels
	 *            an array of channel indices to read (starting from <code>0</code>)
	 * @return the message to be sent to the server
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public OSCMessage readChannelMsg(String aPath, long fileStartFrame, int nFrames, int bufStartFrame,
			boolean leaveOpen, int[] channels) {
		return readChannelMsg(aPath, fileStartFrame, nFrames, bufStartFrame, leaveOpen, channels, null);
	}

	/**
	 * Creates an OSC <code>/b_read</code> message to read in frames from selected
	 * channels of a sound file into the buffer, beginning a given offset in the
	 * buffer.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 * @param bufStartFrame
	 *            the offset (in frames) in the buffer at which the filling begins
	 * @param leaveOpen
	 *            <code>false</code> to close the sound file after reading,
	 *            <code>true</code> to leave it open (as required for a
	 *            <code>DiskIn</code> UGen). If you leave the file open, don't
	 *            forget to call <code>close</code> on the buffer eventually.
	 * @param channels
	 *            an array of channel indices to read (starting from <code>0</code>)
	 * @param completionMsg
	 *            an <code>OSCMessage</code> which is processed by the server when
	 *            the reading is complete. can be <code>null</code>.
	 * @return the message to be sent to the server
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public OSCMessage readChannelMsg(String aPath, long fileStartFrame, int nFrames, int bufStartFrame,
			boolean leaveOpen, int[] channels, OSCMessage completionMsg) {
		if (fileStartFrame > 0x7FFFFFFFL) {
			Server.getPrintStream()
					.println("Buffer.readChannelMsg : fileStartFrame (" + fileStartFrame + ") exceeds 32bit int");
		}

		setPath(aPath);

		final Object[] args = new Object[6 + channels.length + (completionMsg == null ? 0 : 1)];
		args[0] = new Integer(getBufNum());
		args[1] = aPath;
		args[2] = new Long(fileStartFrame);
		args[3] = new Integer(nFrames);
		args[4] = new Integer(bufStartFrame);
		args[5] = new Integer(leaveOpen ? 1 : 0);
		for (int i = 6, j = 0; j < channels.length;) {
			args[i++] = new Integer(channels[j++]);
		}
		if (completionMsg != null)
			args[args.length - 1] = completionMsg;
		return (new OSCMessage("/b_readChannel", args));
		// doesn't set my numChannels etc.
	}

	/**
	 * Writes the buffer contents to a sound file, using AIFF integer 24 bit format.
	 *
	 * @param aPath
	 *            the path name of the file to write to
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 */
	public void write(String aPath) throws IOException {
		write(aPath, "aiff", "int24");
	}

	/**
	 * Writes the buffer contents to a sound file.
	 *
	 * @param aPath
	 *            the path name of the file to write to
	 * @param headerFormat
	 *            one of <code>kHeaderAIFF</code> etc.
	 * @param sampleFormat
	 *            one of <code>kSampleInt24</code> etc.
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see Constants#kHeaderAIFF
	 * @see Constants#kSampleInt24
	 */
	public void write(String aPath, String headerFormat, String sampleFormat) throws IOException {
		write(aPath, headerFormat, sampleFormat, -1);
	}

	/**
	 * Writes the buffer contents to a sound file.
	 *
	 * @param aPath
	 *            the path name of the file to write to
	 * @param headerFormat
	 *            one of <code>kHeaderAIFF</code> etc.
	 * @param sampleFormat
	 *            one of <code>kSampleInt24</code> etc.
	 * @param nFrames
	 *            to number of frames to write, or <code>-1</code> to write the
	 *            whole buffer
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see Constants#kHeaderAIFF
	 * @see Constants#kSampleInt24
	 */
	public void write(String aPath, String headerFormat, String sampleFormat, int nFrames) throws IOException {
		write(aPath, headerFormat, sampleFormat, nFrames, 0);
	}

	/**
	 * Writes a section of the buffer to a sound file.
	 *
	 * @param aPath
	 *            the path name of the file to write to
	 * @param headerFormat
	 *            one of <code>kHeaderAIFF</code> etc.
	 * @param sampleFormat
	 *            one of <code>kSampleInt24</code> etc.
	 * @param nFrames
	 *            to number of frames to write, or <code>-1</code> to write the
	 *            whole buffer
	 * @param bufStartFrame
	 *            the start frame in the buffer from which to write
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see Constants#kHeaderAIFF
	 * @see Constants#kSampleInt24
	 */
	public void write(String aPath, String headerFormat, String sampleFormat, int nFrames, int bufStartFrame)
			throws IOException {
		write(aPath, headerFormat, sampleFormat, nFrames, bufStartFrame, false);
	}

	/**
	 * Writes a section of the buffer to a sound file.
	 *
	 * @param aPath
	 *            the path name of the file to write to
	 * @param headerFormat
	 *            one of <code>kHeaderAIFF</code> etc.
	 * @param sampleFormat
	 *            one of <code>kSampleInt24</code> etc.
	 * @param nFrames
	 *            to number of frames to write, or <code>-1</code> to write the
	 *            whole buffer
	 * @param bufStartFrame
	 *            the start frame in the buffer from which to write
	 * @param leaveOpen
	 *            <code>false</code> to close the sound file after writing,
	 *            <code>true</code> to leave it open (as required for a
	 *            <code>DiskOut</code> UGen). If you leave the file open, don't
	 *            forget to call <code>close</code> on the buffer eventually.
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see Constants#kHeaderAIFF
	 * @see Constants#kSampleInt24
	 */
	public void write(String aPath, String headerFormat, String sampleFormat, int nFrames, int bufStartFrame,
			boolean leaveOpen) throws IOException {
		write(aPath, headerFormat, sampleFormat, nFrames, bufStartFrame, leaveOpen, null);
	}

	/**
	 * Writes a section of the buffer to a sound file.
	 *
	 * @param aPath
	 *            the path name of the file to write to
	 * @param headerFormat
	 *            one of <code>kHeaderAIFF</code> etc.
	 * @param sampleFormat
	 *            one of <code>kSampleInt24</code> etc.
	 * @param nFrames
	 *            to number of frames to write, or <code>-1</code> to write the
	 *            whole buffer
	 * @param bufStartFrame
	 *            the start frame in the buffer from which to write
	 * @param leaveOpen
	 *            <code>false</code> to close the sound file after writing,
	 *            <code>true</code> to leave it open (as required for a
	 *            <code>DiskOut</code> UGen). If you leave the file open, don't
	 *            forget to call <code>close</code> on the buffer eventually.
	 * @param completionMsg
	 *            an <code>OSCMessage</code> which is processed by the server when
	 *            the writing is complete. can be <code>null</code>.
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see Constants#kHeaderAIFF
	 * @see Constants#kSampleInt24
	 */
	public void write(String aPath, String headerFormat, String sampleFormat, int nFrames, int bufStartFrame,
			boolean leaveOpen, OSCMessage completionMsg) throws IOException {
		getServer().sendMsg(
				writeMsg(aPath, headerFormat, sampleFormat, nFrames, bufStartFrame, leaveOpen, completionMsg));
	}

	/**
	 * Creates an OSC <code>/b_write</code> message to write the buffer contents to
	 * a sound file, using AIFF integer 24 bit format.
	 *
	 * @param aPath
	 *            the path name of the file to write to
	 * @return the message to send to the server
	 */
	public OSCMessage writeMsg(String aPath) {
		return writeMsg(aPath, "aiff", "int24");
	}

	/**
	 * Creates an OSC <code>/b_write</code> message to write the buffer contents to
	 * a sound file.
	 *
	 * @param aPath
	 *            the path name of the file to write to
	 * @param headerFormat
	 *            one of <code>kHeaderAIFF</code> etc.
	 * @param sampleFormat
	 *            one of <code>kSampleInt24</code> etc.
	 * @return the message to send to the server
	 *
	 * @see Constants#kHeaderAIFF
	 * @see Constants#kSampleInt24
	 */
	public OSCMessage writeMsg(String aPath, String headerFormat, String sampleFormat) {
		return writeMsg(aPath, headerFormat, sampleFormat, -1);
	}

	/**
	 * Creates an OSC <code>/b_write</code> message to write the buffer contents to
	 * a sound file.
	 *
	 * @param aPath
	 *            the path name of the file to write to
	 * @param headerFormat
	 *            one of <code>kHeaderAIFF</code> etc.
	 * @param sampleFormat
	 *            one of <code>kSampleInt24</code> etc.
	 * @param nFrames
	 *            to number of frames to write, or <code>-1</code> to write the
	 *            whole buffer
	 * @return the message to send to the server
	 *
	 * @see Constants#kHeaderAIFF
	 * @see Constants#kSampleInt24
	 */
	public OSCMessage writeMsg(String aPath, String headerFormat, String sampleFormat, int nFrames) {
		return writeMsg(aPath, headerFormat, sampleFormat, nFrames, 0);
	}

	/**
	 * Creates an OSC <code>/b_write</code> message to write a section of the buffer
	 * to a sound file.
	 *
	 * @param aPath
	 *            the path name of the file to write to
	 * @param headerFormat
	 *            one of <code>kHeaderAIFF</code> etc.
	 * @param sampleFormat
	 *            one of <code>kSampleInt24</code> etc.
	 * @param nFrames
	 *            to number of frames to write, or <code>-1</code> to write the
	 *            whole buffer
	 * @param bufStartFrame
	 *            the start frame in the buffer from which to write
	 * @return the message to send to the server
	 *
	 * @see Constants#kHeaderAIFF
	 * @see Constants#kSampleInt24
	 */
	public OSCMessage writeMsg(String aPath, String headerFormat, String sampleFormat, int nFrames,
			int bufStartFrame) {
		return writeMsg(aPath, headerFormat, sampleFormat, nFrames, bufStartFrame, false);
	}

	/**
	 * Creates an OSC <code>/b_write</code> message to write a section of the buffer
	 * to a sound file.
	 *
	 * @param aPath
	 *            the path name of the file to write to
	 * @param headerFormat
	 *            one of <code>kHeaderAIFF</code> etc.
	 * @param sampleFormat
	 *            one of <code>kSampleInt24</code> etc.
	 * @param nFrames
	 *            to number of frames to write, or <code>-1</code> to write the
	 *            whole buffer
	 * @param bufStartFrame
	 *            the start frame in the buffer from which to write
	 * @param leaveOpen
	 *            <code>false</code> to close the sound file after writing,
	 *            <code>true</code> to leave it open (as required for a
	 *            <code>DiskOut</code> UGen). If you leave the file open, don't
	 *            forget to call <code>close</code> on the buffer eventually.
	 * @return the message to send to the server
	 *
	 * @see Constants#kHeaderAIFF
	 * @see Constants#kSampleInt24
	 */
	public OSCMessage writeMsg(String aPath, String headerFormat, String sampleFormat, int nFrames, int bufStartFrame,
			boolean leaveOpen) {
		return writeMsg(aPath, headerFormat, sampleFormat, nFrames, bufStartFrame, leaveOpen, null);
	}

	/**
	 * Creates an OSC <code>/b_write</code> message to write a section of the buffer
	 * to a sound file.
	 *
	 * @param aPath
	 *            the path name of the file to write to
	 * @param headerFormat
	 *            one of <code>kHeaderAIFF</code> etc.
	 * @param sampleFormat
	 *            one of <code>kSampleInt24</code> etc.
	 * @param nFrames
	 *            to number of frames to write, or <code>-1</code> to write the
	 *            whole buffer
	 * @param bufStartFrame
	 *            the start frame in the buffer from which to write
	 * @param leaveOpen
	 *            <code>false</code> to close the sound file after writing,
	 *            <code>true</code> to leave it open (as required for a
	 *            <code>DiskOut</code> UGen). If you leave the file open, don't
	 *            forget to call <code>close</code> on the buffer eventually.
	 * @param completionMsg
	 *            an <code>OSCMessage</code> which is processed by the server when
	 *            the writing is complete. can be <code>null</code>.
	 * @return the message to send to the server
	 *
	 * @see Constants#kHeaderAIFF
	 * @see Constants#kSampleInt24
	 */
	public OSCMessage writeMsg(String aPath, String headerFormat, String sampleFormat, int nFrames, int bufStartFrame,
			boolean leaveOpen, OSCMessage completionMsg) {
		final Object[] args = completionMsg == null
				? new Object[] { new Integer(getBufNum()), aPath, headerFormat, sampleFormat, new Integer(nFrames),
						new Integer(bufStartFrame), new Integer(leaveOpen ? 1 : 0) }
				: new Object[] { new Integer(getBufNum()), aPath, headerFormat, sampleFormat, new Integer(nFrames),
						new Integer(bufStartFrame), new Integer(leaveOpen ? 1 : 0), completionMsg };

		return (new OSCMessage("/b_write", args));
	}

	private void setDoOnInfo(CompletionAction action) {
		doOnInfo = action;
	}

	private CompletionAction getDoOnInfo() {
		return doOnInfo;
	}

	/**
	 * Reads a whole file into memory for PlayBuf etc. Just like
	 * <code>read( Server, String )</code> but without sending a
	 * <code>/b_query</code>. Hence, the internal fields are not updated unless you
	 * explicitly call <code>query()</code>
	 *
	 * @param server
	 *            the server on which the buffer is allocated
	 * @param path
	 *            the path to the sound file
	 *
	 * @return the newly created Buffer or <code>null</code> if the server's buffer
	 *         allocator is exhausted
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see #read( Server, String )
	 */
	public static Buffer readNoUpdate(Server server, String path) throws IOException {
		return Buffer.readNoUpdate(server, path, 0);
	}

	/**
	 * Reads a whole file into memory for PlayBuf etc. Just like
	 * <code>read( Server, String, long )</code> but without sending a
	 * <code>/b_query</code>. Hence, the internal fields are not updated unless you
	 * explicitly call <code>query()</code>
	 *
	 * @param server
	 *            the server on which the buffer is allocated
	 * @param path
	 *            the path to the sound file
	 * @param startFrame
	 *            the frame index in the sound file to start reading from
	 *
	 * @return the newly created Buffer or <code>null</code> if the server's buffer
	 *         allocator is exhausted
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see #read( Server, String, long )
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public static Buffer readNoUpdate(Server server, String path, long startFrame) throws IOException {
		return Buffer.readNoUpdate(server, path, startFrame, -1);
	}

	/**
	 * Reads a section of a file into memory for PlayBuf etc. Just like
	 * <code>read( Server, String, long, int )</code> but without sending a
	 * <code>/b_query</code>. Hence, the internal fields are not updated unless you
	 * explicitly call <code>query()</code>
	 *
	 * @param server
	 *            the server on which the buffer is allocated
	 * @param path
	 *            the path to the sound file
	 * @param startFrame
	 *            the frame index in the sound file to start reading from
	 * @param numFrames
	 *            the number of frames to read. this is equal to the number of
	 *            frames allocated for the buffer
	 *
	 * @return the newly created Buffer or <code>null</code> if the server's buffer
	 *         allocator is exhausted
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @see #read( Server, String, long, int )
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public static Buffer readNoUpdate(Server server, String path, long startFrame, int numFrames) throws IOException {
		return Buffer.readNoUpdate(server, path, startFrame, numFrames, null);
	}

	/**
	 * Reads a section of a file into memory for PlayBuf etc. Just like
	 * <code>read( Server, String, long, int, CompletionFunction )</code> but
	 * without sending a <code>/b_query</code>. Hence, the internal fields are not
	 * updated unless you explicitly call <code>query()</code>.
	 *
	 * @param server
	 *            the server on which the buffer is allocated
	 * @param path
	 *            the path to the sound file
	 * @param startFrame
	 *            the frame index in the sound file to start reading from
	 * @param numFrames
	 *            the number of frames to read. this is equal to the number of
	 *            frames allocated for the buffer
	 * @param completionFunc
	 *            an action to be executed when the <code>/b_info</code> message
	 *            comes back from the server. at this time the buffer has been
	 *            allocated and filled. <code>action</code> can be <code>null</code>
	 *
	 * @return the newly created Buffer or <code>null</code> if the server's buffer
	 *         allocator is exhausted
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 * 
	 * @see #read( Server, String, long, int, CompletionAction )
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public static Buffer readNoUpdate(Server server, String path, long startFrame, int numFrames,
			CompletionFunction completionFunc) throws IOException {
		final int bufNum = server.getBufferAllocator().alloc(1);

		if (bufNum == -1) {
			Server.getPrintStream().println(
					"Buffer.readNoUpdate: failed to get a buffer allocated. " + "; server: " + server.getName());
			return null;
		} else {
			return Buffer.readNoUpdate(server, path, startFrame, numFrames, completionFunc, bufNum);
		}
	}

	/**
	 * Reads a section of a file into memory for PlayBuf etc. Just like
	 * <code>read( Server, String, long, int, CompletionFunction, int )</code> but
	 * without sending a <code>/b_query</code>. Hence, the internal fields are not
	 * updated unless you explicitly call <code>query()</code>. An explicit buffer
	 * index is provided.
	 *
	 * @param server
	 *            the server on which the buffer is allocated
	 * @param path
	 *            the path to the sound file
	 * @param startFrame
	 *            the frame index in the sound file to start reading from
	 * @param numFrames
	 *            the number of frames to read. this is equal to the number of
	 *            frames allocated for the buffer
	 * @param completionFunc
	 *            an action to be executed when the <code>/b_info</code> message
	 *            comes back from the server. at this time the buffer has been
	 *            allocated and filled. <code>action</code> can be <code>null</code>
	 * @param bufNum
	 *            the index to use for the buffer
	 *
	 * @return the newly created buffer
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 * 
	 * @see #read( Server, String, long, int, CompletionAction, int )
	 *
	 * @warning <code>long startFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public static Buffer readNoUpdate(Server server, String path, long startFrame, int numFrames,
			CompletionFunction completionFunc, int bufNum) throws IOException {
		final Buffer buf = new Buffer(server, bufNum);
		buf.allocRead(path, startFrame, numFrames, completionFunc == null ? null : completionFunc.completion(buf));
		return buf;
	}

	/**
	 * Reads in as many frames from a sound file as fit into the buffer, starting at
	 * the beginning of the file. Closes the file after reading. Just like
	 * <code>read( String )</code> but without sending a <code>/b_query</code>.
	 * Hence, the internal fields are not updated unless you explicitly call
	 * <code>query()</code>.
	 *
	 * @param aPath
	 *            the path to the sound file
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 */
	public void readNoUpdate(String aPath) throws IOException {
		readNoUpdate(aPath, 0);
	}

	/**
	 * Reads in as many frames from a sound file as fit into the buffer, starting at
	 * a given frame in the file. Closes the file after reading. Just like
	 * <code>read( String, long )</code> but without sending a
	 * <code>/b_query</code>. Hence, the internal fields are not updated unless you
	 * explicitly call <code>query()</code>.
	 *
	 * 
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void readNoUpdate(String aPath, long fileStartFrame) throws IOException {
		readNoUpdate(aPath, fileStartFrame, -1);
	}

	/**
	 * Reads in frames from a sound file into the buffer. Closes the file after
	 * reading. Just like <code>read( String, long, int )</code> but without sending
	 * a <code>/b_query</code>. Hence, the internal fields are not updated unless
	 * you explicitly call <code>query()</code>.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void readNoUpdate(String aPath, long fileStartFrame, int nFrames) throws IOException {
		readNoUpdate(aPath, fileStartFrame, nFrames, 0);
	}

	/**
	 * Reads in frames from a sound file into the buffer, beginning a given offset
	 * in the buffer. Closes the file after reading. Just like
	 * <code>read( String, long, int, int )</code> but without sending a
	 * <code>/b_query</code>. Hence, the internal fields are not updated unless you
	 * explicitly call <code>query()</code>.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 * @param bufStartFrame
	 *            the offset (in frames) in the buffer at which the filling begins
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void readNoUpdate(String aPath, long fileStartFrame, int nFrames, int bufStartFrame) throws IOException {
		readNoUpdate(aPath, fileStartFrame, nFrames, bufStartFrame, false);
	}

	/**
	 * Reads in frames from a sound file into the buffer, beginning a given offset
	 * in the buffer. Just like <code>read( String, long, int, int, boolean )</code>
	 * but without sending a <code>/b_query</code>. Hence, the internal fields are
	 * not updated unless you explicitly call <code>query()</code>.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 * @param bufStartFrame
	 *            the offset (in frames) in the buffer at which the filling begins
	 * @param leaveOpen
	 *            <code>false</code> to close the sound file after reading,
	 *            <code>true</code> to leave it open (as required for a
	 *            <code>DiskIn</code> UGen). If you leave the file open, don't
	 *            forget to call <code>close</code> on the buffer eventually.
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void readNoUpdate(String aPath, long fileStartFrame, int nFrames, int bufStartFrame, boolean leaveOpen)
			throws IOException {
		readNoUpdate(aPath, fileStartFrame, nFrames, bufStartFrame, leaveOpen, null);
	}

	/**
	 * Reads in frames from a sound file into the buffer, beginning a given offset
	 * in the buffer. Just like
	 * <code>read( String, long, int, int, boolean, CompletionAction )</code> but
	 * without sending a <code>/b_query</code>. Hence, the internal fields are not
	 * updated unless you explicitly call <code>query()</code>.
	 *
	 * @param aPath
	 *            the path to the sound file
	 * @param fileStartFrame
	 *            the frame index in the sound file to start reading from
	 * @param nFrames
	 *            the number of frames to read a value of <code>-1</code> indicates
	 *            that as many frames as fit into the buffer should be read
	 * @param bufStartFrame
	 *            the offset (in frames) in the buffer at which the filling begins
	 * @param leaveOpen
	 *            <code>false</code> to close the sound file after reading,
	 *            <code>true</code> to leave it open (as required for a
	 *            <code>DiskIn</code> UGen). If you leave the file open, don't
	 *            forget to call <code>close</code> on the buffer eventually.
	 * @param completionMsg
	 *            an <code>OSCMessage</code> which is processed by the server when
	 *            the reading is complete. can be <code>null</code>.
	 *
	 * @throws IOException
	 *             if an error occurs while sending the OSC message
	 *
	 * @warning <code>long fileStartFrame</code> is truncated to 32bit by
	 *          <code>OSCMessage</code> for now
	 */
	public void readNoUpdate(String aPath, long fileStartFrame, int nFrames, int bufStartFrame, boolean leaveOpen,
			OSCMessage completionMsg) throws IOException {
		getServer().sendMsg(readMsg(aPath, fileStartFrame, nFrames, bufStartFrame, leaveOpen, completionMsg));
	}

	/**
	 * @warning numChannels defaults to 1 (not 2 as in sclang) !!
	 */
	public static Buffer cueSoundFile(Server server, String path) throws IOException {
		return Buffer.cueSoundFile(server, path, 0);
	}

	/**
	 * @warning numChannels defaults to 1 (not 2 as in sclang) !!
	 */
	public static Buffer cueSoundFile(Server server, String path, long startFrame) throws IOException {
		return Buffer.cueSoundFile(server, path, startFrame, 1);
	}

	public static Buffer cueSoundFile(Server server, String path, long startFrame, int numChannels) throws IOException {
		return Buffer.cueSoundFile(server, path, startFrame, numChannels, 32768);
	}

	public static Buffer cueSoundFile(Server server, String path, long startFrame, int numChannels, int bufferSize)
			throws IOException {
		return Buffer.cueSoundFile(server, path, startFrame, numChannels, bufferSize, null);
	}

	// preload a buffer for use with DiskIn
	public static Buffer cueSoundFile(Server server, final String path, final long startFrame, int numChannels,
			final int bufferSize, final CompletionFunction completionFunc) throws IOException {
		final Buffer buf = Buffer.alloc(server, bufferSize, numChannels, new CompletionFunction() {
			@Override
			public OSCMessage completion(Buffer buffer) {
				return buffer.readMsg(path, startFrame, bufferSize, 0, true,
						completionFunc == null ? null : completionFunc.completion(buffer));
			}
		});
		buf.addToServerArray();
		return buf;
	}

	public void cueSoundFile(String aPath) throws IOException {
		cueSoundFile(aPath, 0);
	}

	public void cueSoundFile(String aPath, long startFrame) throws IOException {
		cueSoundFile(aPath, startFrame, null);
	}

	public void cueSoundFile(String aPath, long startFrame, OSCMessage completionMsg) throws IOException {
		getServer().sendMsg(cueSoundFileMsg(aPath, startFrame, completionMsg));
	}

	public OSCMessage cueSoundFileMsg(String aPath, long startFrame, OSCMessage completionMsg) {
		return readMsg(aPath, startFrame, getNumFrames(), 0, true, completionMsg);
	}

	/**
	 * cache Buffers in an Array for easy info updating
	 * 
	 * @throws IOException
	 */
	private void addToServerArray() throws IOException {
		getServer().addBuf(this);
	}

	// tell the server to wait for a b_info
	private void waitForBufInfo() throws IOException {
		getServer().waitForBufInfo();
	}

	/**
	 * Called from Server when b_info is received. Do not call this method directly
	 */
	protected void queryDone() {
		if (getDoOnInfo() != null) {
			getDoOnInfo().completion(this);
			setDoOnInfo(null);
		}
	}

	public void fill(int startAt, int numSamples, float value) throws IOException {
		getServer().sendMsg(fillMsg(startAt, numSamples, value));
	}

	public OSCMessage fillMsg(int startAt, int numSamples, float value) {
		return (new OSCMessage("/b_fill", new Object[] { new Integer(getBufNum()), new Integer(startAt),
				new Integer(numSamples), new Float(value) }));
	}

	public void fill(int[] startAt, int[] numSamples, float[] value) throws IOException {
		getServer().sendMsg(fillMsg(startAt, numSamples, value));
	}

	public OSCMessage fillMsg(int[] startAt, int[] numSamples, float[] value) {
		final Object[] args = new Object[startAt.length * 3 + 1];
		args[0] = new Integer(getBufNum());

		for (int i = 0, j = 1; i < startAt.length; i++) {
			args[j++] = new Integer(startAt[i]);
			args[j++] = new Integer(numSamples[i]);
			args[j++] = new Float(value[i]);
		}

		return (new OSCMessage("/b_fill", args));
	}

	// close a file, write header, after DiskOut usage
	public void close() throws IOException {
		close(null);
	}

	public void close(OSCMessage completionMsg) throws IOException {
		getServer().sendMsg(closeMsg(completionMsg));
	}

	public OSCMessage closeMsg() {
		return closeMsg(null);
	}

	public OSCMessage closeMsg(OSCMessage completionMsg) {
		return simpleMsg("/b_close", completionMsg);
	}

	private OSCMessage simpleMsg(String cmdName, OSCMessage completionMsg) {
		final Object[] args = completionMsg == null ? new Object[] { new Integer(getBufNum()) }
				: new Object[] { new Integer(getBufNum()), completionMsg };

		return (new OSCMessage(cmdName, args));
	}

	public void free() throws IOException {
		free(null);
	}

	public void free(OSCMessage completionMsg) throws IOException {
		getServer().sendMsg(freeMsg(completionMsg));
	}

	public OSCMessage freeMsg() {
		return freeMsg(null);
	}

	public OSCMessage freeMsg(OSCMessage completionMsg) {
		getServer().freeBuf(getBufNum());
		getServer().getBufferAllocator().free(getBufNum());
		return simpleMsg("/b_free", completionMsg);
	}

	public void zero() throws IOException {
		zero(null);
	}

	public void zero(OSCMessage completionMsg) throws IOException {
		getServer().sendMsg(zeroMsg(completionMsg));
	}

	public OSCMessage zeroMsg() {
		return zeroMsg(null);
	}

	public OSCMessage zeroMsg(OSCMessage completionMsg) {
		return simpleMsg("/b_zero", completionMsg);
	}

	/**
	 * Frees all known buffers on a server. Known buffers are those which were
	 * allocated regularly using the server's buffer allocator.
	 *
	 * @param server
	 *            the server whose buffers should be freed
	 *
	 * @throws IOException
	 *             if an error occurs in OSC bundle sending
	 */
	@SuppressWarnings("rawtypes")
	public static void freeAll(Server server) throws IOException {
		final BlockAllocator bufferAllocator = server.getBufferAllocator();
		final java.util.List blocks = bufferAllocator.getAllocatedBlocks();
		BlockAllocator.Block block;
		final OSCBundle bndl = new OSCBundle(0.0);

		for (int i = 0; i < blocks.size(); i++) {
			block = (BlockAllocator.Block) blocks.get(i);
			bndl.addPacket(new OSCMessage("/b_free", new Object[] { new Integer(block.getAddress()) }));
			bufferAllocator.free(block.getAddress());
			server.freeBuf(block.getAddress()); // this was forgotten in sclang
		}
		if (bndl.getPacketCount() > 0)
			server.sendBundle(bndl);
	}

	/**
	 * A debugging method for querying the current buffer parameters and printing
	 * them into the console. This sends a query message to the server and upon
	 * reply prints the current buffer parameters into the console (as defined by
	 * <code>server.setPrintStream()</code>).
	 *
	 * @throws IOException
	 *             if a networking error occurs
	 *
	 * @todo this simply waits for the next /b_info message to come in without
	 *       checking the bufNum ; also there is no time out
	 */
	public void query() throws IOException {
		new OSCResponderNode(getServer(), "/b_info", new OSCResponderNode.Action() {
			@Override
			public void respond(OSCResponderNode r, OSCMessage msg, long time) {
				if (((Number) msg.getArg(0)).intValue() != getBufNum())
					return;
				Server.getPrintStream().println("bufNum      : " + msg.getArg(0) + "\nnumFrames   : " + msg.getArg(1)
						+ "\nnumChannels : " + msg.getArg(2) + "\nsampleRate  : " + msg.getArg(3) + "\n");
				r.remove();
			}
		}).add();

		getServer().sendMsg(queryMsg());
	}

	/**
	 * Constructs an OSC message <code>/b_query</code> for querying the buffer's
	 * parameters.
	 *
	 * @return the OSC message, ready to be send to the server
	 */
	public OSCMessage queryMsg() {
		return simpleMsg("/b_query", null);
	}

	/**
	 * Asynchronously updates the buffer parameters (such as number of frames or
	 * sample rate). It sends a query message to the server and upon reply, executes
	 * a given completion action.
	 *
	 * @param action
	 *            the action to be executed once the buffer info is updated
	 *
	 * @throws IOException
	 *             if a networking error occurs
	 */
	public void updateInfo(CompletionAction action) throws IOException {
		// add to the array here. That way, update will be accurate even if this buf
		// has been freed
		addToServerArray();
		setDoOnInfo(action);
		waitForBufInfo();

		getServer().sendMsg(queryMsg());
	}

	public void printOn(PrintStream stream) {
		stream.print(this.getClass().getName() + "(" + getBufNum() + "," + getNumFrames() + "," + getNumChannels() + ","
				+ getSampleRate() + "," + getPath() + ")");
	}

	/**
	 * Plays the buffer contents (oneshot) beginning on the first audio output.
	 *
	 * @return the buffer playing synth. the synth is automatically freed when the
	 *         buffer playback is complete.
	 *
	 * @throws IOException
	 *             if an error occurs in message sending
	 * @warning requires prior UGenInfo.readDefinitions
	 */
	public Synth play() throws IOException {
		return play(false);
	}

	/**
	 * Plays the buffer contents beginning on the first audio output.
	 *
	 * @param loop
	 *            whether the playback should be looped or not.
	 * @return the buffer playing synth. when not looping, the synth is
	 *         automatically freed when the buffer playback is complete.
	 *
	 * @throws IOException
	 *             if an error occurs in message sending
	 * @warning requires prior UGenInfo.readDefinitions
	 */
	public Synth play(boolean loop) throws IOException {
		return play(loop, 1f);
	}

	/**
	 * Plays the buffer contents beginning on the first audio output.
	 *
	 * @param loop
	 *            whether the playback should be looped or not.
	 * @param amp
	 *            the amplitude scaling
	 * @return the buffer playing synth. when not looping, the synth is
	 *         automatically freed when the buffer playback is complete.
	 *
	 * @throws IOException
	 *             if an error occurs in message sending
	 * @warning requires prior UGenInfo.readDefinitions
	 */
	public Synth play(boolean loop, float amp) throws IOException {
		return play(loop, amp, 0);
	}

	/**
	 * Plays the buffer contents.
	 *
	 * @param loop
	 *            whether the playback should be looped or not.
	 * @param amp
	 *            the amplitude scaling
	 * @param outBus
	 *            the index of the first bus to play on
	 * @return the buffer playing synth. when not looping, the synth is
	 *         automatically freed when the buffer playback is complete.
	 *
	 * @throws IOException
	 *             if an error occurs in message sending
	 * @warning requires prior UGenInfo.readDefinitions
	 */
	public Synth play(boolean loop, float amp, int outBus) throws IOException {
		return play(loop, amp, outBus, 0.02f);
	}

	/**
	 * Plays the buffer contents.
	 *
	 * @param loop
	 *            whether the playback should be looped or not.
	 * @param amp
	 *            the amplitude scaling
	 * @param outBus
	 *            the index of the first bus to play on
	 * @param fadeTime
	 *            the time in seconds for the synth to fade in and out (upon
	 *            release())
	 * @return the buffer playing synth. when not looping, the synth is
	 *         automatically freed when the buffer playback is complete.
	 *
	 * @throws IOException
	 *             if an error occurs in message sending
	 * @warning requires prior UGenInfo.readDefinitions
	 */
	public Synth play(boolean loop, float amp, int outBus, float fadeTime) throws IOException {
		return play(loop, amp, outBus, fadeTime, null, kAddToHead);
	}

	/**
	 * Plays the buffer contents.
	 *
	 * @param loop
	 *            whether the playback should be looped or not.
	 * @param amp
	 *            the amplitude scaling
	 * @param outBus
	 *            the index of the first bus to play on
	 * @param fadeTime
	 *            the time in seconds for the synth to fade in and out (upon
	 *            release())
	 * @param target
	 *            to node to add the new synth to
	 * @param addAction
	 *            the add action to use when adding the synth
	 * @return the buffer playing synth. when not looping, the synth is
	 *         automatically freed when the buffer playback is complete.
	 *
	 * @throws IOException
	 *             if an error occurs in message sending
	 * @warning requires prior UGenInfo.readDefinitions
	 */
	public Synth play(boolean loop, float amp, int outBus, float fadeTime, Node target, int addAction)
			throws IOException {
		final GraphElem bNum, out, ctrl, dt, gate;
		final OSCMessage newMsg;
		final String name;
		final Synth synth;
		final SynthDef def;
		GraphElem player, graph;

		out = Control.ir("i_out", 0f);
		bNum = UGen.ir(getBufNum());
		player = UGen.ar("PlayBuf", getNumChannels(), bNum, UGen.kr("BufRateScale", bNum), UGen.ir(1f), UGen.ir(0f),
				UGen.ir(loop ? 1f : 0f));

		if (fadeTime > 0f) {
			ctrl = Control.kr(new String[] { "fadeTime", "gate" }, new float[] { fadeTime, 1f });
			dt = ctrl.getOutput(0);
			gate = ctrl.getOutput(1);
			player = UGen.ar("*", player, UGen.kr("Linen", gate, dt, UGen.ir(1f), dt, UGen.ir(2f)));
		}

		graph = UGen.ar("Out", out, UGen.ar("*", player, UGen.ir(amp)));

		if (!loop) {
			// XXX should be replaced by a Line and BufDur
			graph = UGen.array(graph,
					UGen.kr("Line", UGen.ir(0f), UGen.ir(1f), UGen.kr("BufDur", bNum), UGen.ir(kDoneFree)));
		}

		name = "temp_" + String.valueOf(Math.abs(hashCode()));
		def = new SynthDef(name, graph);

		synth = Synth.basicNew(name, getServer());
		newMsg = synth.newMsg(target, new String[] { "i_out" }, new float[] { outBus }, addAction);

		def.send(getServer(), newMsg);
		return synth;
	}

	// ---------- internal classes and interfaces ----------

	/**
	 * Interface describing an action to take place after an asynchronous buffer
	 * command is completed.
	 */
	public static interface CompletionAction {
		/**
		 * Executes the completion action.
		 *
		 * @param buf
		 *            the buffer whose asynchronous action is completed.
		 */
		public void completion(Buffer buf);
	}

	/**
	 * Interface describing an function that creates an OSC message used as a
	 * completion message in asynchronous buffer commands.
	 */
	public static interface CompletionFunction {
		/**
		 * Queries the creation of the completion message.
		 *
		 * @param buf
		 *            the buffer for which the completion message is created
		 * @return the <code>OSCMessage</code> attached to as completion message to an
		 *         asynchronous command
		 */
		public OSCMessage completion(Buffer buf);
	}
}