/*
 * ControlDesc.java
 * (JavaCollider)
 * Copyright (c) 2004-2015 Hanns Holger Rutz. All rights reserved.
 * This software is published under the GNU Lesser General Public License v2.1+
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.jcollider;

import java.io.PrintStream;

/**
 * A descriptor class for a control UGen, similar to SClang's ControlName class.
 * Note that the <code>lag</code> parameter is currently unused.
 *
 * @author Hanns Holger Rutz
 * @version 0.31, 08-Oct-07
 */
public class ControlDesc {
	private final String name;
	private final Object rate;
	private final float defaultValue;
	private final float lag;

	public ControlDesc(String name, Object rate, float defaultValue, float lag) {
		this.name = name;
		// this.index = index;
		this.rate = rate;
		this.defaultValue = defaultValue;
		this.lag = lag;
	}

	public ControlDesc(String name, Object rate, float defaultValue) {
		this(name, rate, defaultValue, 0.0f);
	}

	public Object getRate() {
		return rate;
	}

	public String getName() {
		return name;
	}

	public float getDefaultValue() {
		return defaultValue;
	}

	public float getLag() {
		return lag;
	}

	public void printOn(PrintStream out) {
		out.print("\"" + (name == null ? "??? " : name) + "\" @ " + rate);
		out.println(", default = " + defaultValue);
	}
}
