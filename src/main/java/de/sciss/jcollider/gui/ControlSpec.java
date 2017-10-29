/*
 * ControlSpec.java
 * (JavaCollider)
 * Copyright (c) 2004-2015 Hanns Holger Rutz. All rights reserved.
 * This software is published under the GNU Lesser General Public License v2.1+
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.jcollider.gui;

/**
 * Specification for a control input such as a synth control. This is a quite
 * direct translation from the SClang class. Specs are immutable though (minVal
 * and maxVal can't be changed).
 *
 * @version 0.28, 29-Jul-06
 * @author Hanns Holger Rutz
 */
public class ControlSpec {
	private final double minVal, maxVal, clipLo, clipHi, step, defaultVal;
	private final String units;
	private final Warp warp;

	public static final ControlSpec defaultSpec = new ControlSpec();

	public ControlSpec() {
		this(0.0, 1.0);
	}

	public ControlSpec(double minVal, double maxVal) {
		this(minVal, maxVal, Warp.lin);
	}

	public ControlSpec(double minVal, double maxVal, Warp warp) {
		this(minVal, maxVal, warp, 0.0);
	}

	public ControlSpec(double minVal, double maxVal, Warp warp, double step) {
		this(minVal, maxVal, warp, step, minVal);
	}

	public ControlSpec(double minVal, double maxVal, Warp warp, double step, double defaultVal) {
		this(minVal, maxVal, warp, step, defaultVal, null);
	}

	public ControlSpec(double minVal, double maxVal, Warp warp, double step, double defaultVal, String units) {
		this.minVal = minVal;
		this.maxVal = maxVal;
		this.warp = warp;
		this.step = step;
		this.defaultVal = defaultVal;
		this.units = units;

		clipLo = Math.min(minVal, maxVal);
		clipHi = Math.max(minVal, maxVal);
	}

	public double getMinVal() {
		return minVal;
	}

	public double getMaxVal() {
		return maxVal;
	}

	public double getClipLo() {
		return clipLo;
	}

	public double getClipHi() {
		return clipHi;
	}

	public double getDefaultVal() {
		return defaultVal;
	}

	public double getStep() {
		return step;
	}

	public Warp getWarp() {
		return warp;
	}

	public String getUnits() {
		return units;
	}

	public double constrain(double value) {
		value = Math.max(clipLo, Math.min(clipHi, value));

		if (step == 0.0) {
			return value;
		} else {
			return Math.round(value / step) * step;
		}
	}

	public double getRange() {
		return (maxVal - minVal);
	}

	public double getRatio() {
		return (maxVal / minVal);
	}

	// maps a value from [0..1] to spec range
	public double map(double value) {
		value = warp.map(Math.max(0.0, Math.min(1.0, value)), this);

		if (step == 0.0) {
			return value;
		} else {
			return Math.round(value / step) * step;
		}
	}

	// maps a value from spec range to [0..1]
	public double unmap(double value) {
		if (step != 0.0) {
			value = Math.round(value / step) * step;
		}
		return warp.unmap(Math.max(clipLo, Math.min(clipHi, value)), this);
	}

}
