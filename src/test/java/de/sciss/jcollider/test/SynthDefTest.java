/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (c) 2017 Matthew MacLeod
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this work; if not, see http://www.gnu.org/licenses/
 */
package de.sciss.jcollider.test;

import java.io.IOException;
import java.util.Random;

import de.sciss.jcollider.Control;
import de.sciss.jcollider.GraphElem;
import de.sciss.jcollider.Server;
import de.sciss.jcollider.Synth;
import de.sciss.jcollider.SynthDef;
import de.sciss.jcollider.UGen;
import de.sciss.jcollider.UGenChannel;
import de.sciss.jcollider.UGenInfo;
import de.sciss.net.OSCBundle;

/**
 *
 * 
 */
public class SynthDefTest {

	public static void main(String[] args) {
		Server.setProgram("/usr/bin/scsynth");

		Server myServer = null;
		try {
			myServer = new Server("localhost");

			myServer.start();
			myServer.startAliveThread();
			myServer.boot();
			
			UGenInfo.readBinaryDefinitions();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		

		GraphElem f = null;
		GraphElem g, h;
		Control c = Control.kr(new String[] { "resinv" }, new float[] { 0.5f });
		UGenChannel reso = c.getChannel(0);
		Synth synth;
		Random r = new Random(System.currentTimeMillis());
		String defName = "JNoiseBusiness1b";
		OSCBundle bndl;
		SynthDef def;
		long time;

		for (int i = 0; i < 4; i++) {
			g = UGen.ar("*", UGen.ar("LFSaw", UGen.kr("midicps",
					UGen.kr("MulAdd", UGen.kr("LFPulse", UGen.ir(0.06f), UGen.ir(0), UGen.ir(0.5f)), UGen.ir(2),
							UGen.array(UGen.ir(34 + r.nextFloat() * 0.2f), UGen.ir(34 + r.nextFloat() * 0.2f))))),
					UGen.ir(0.01f));
			f = (f == null) ? g : UGen.ar("+", f, g);
		}
		h = UGen.kr("LinExp", UGen.kr("SinOsc", UGen.ir(0.07f)), UGen.ir(-1), UGen.ir(1), UGen.ir(300), UGen.ir(5000));
		f = UGen.ar("softclip", UGen.ar("RLPF", f, h, reso));
		f = UGen.ar("softclip", UGen.ar("RLPF", f, h, reso));
		def = new SynthDef(defName, UGen.ar("Out", UGen.ir(0), f));

		synth = Synth.basicNew(defName, myServer);
		try {
			def.send(myServer, synth.newMsg(myServer.asTarget(), new String[] { "resinv" }, new float[] { 0.98f }));
			time = System.currentTimeMillis();
			for (int i = 500; i < 5000; i += 250) {
				bndl = new OSCBundle(time + i);
				bndl.addPacket(synth.setMsg("resinv", r.nextFloat() * 0.8f + 0.015f));
				myServer.sendBundle(bndl);
			}
			bndl = new OSCBundle(time + 5500);
			bndl.addPacket(synth.freeMsg());
			myServer.sendBundle(bndl);
		} catch (IOException e1) {
			System.err.println(e1);
		}
	}

}
