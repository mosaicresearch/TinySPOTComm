/*
 * Copyright 2006-2010 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.spot.resources.transducers;

/**
 * Interface to a basic GPIO pin.
 */
public interface IIOPin extends IOutputPin, IInputPin, ITransducer {

	/**
	 * Ask if this pin is an output.
	 * Pins are initially inputs.
	 * Note that it is possible to query the current pin setting of an output pin using isHigh() and isLow().
	 * @return true if this pin is currently set to be an output
	 */
	boolean isOutput();

	/**
     * Set whether the pin should be an input or an output.
     * 
	 * @param b if b is true the pin becomes an output, if false it becomes an input
	 */
	void setAsOutput(boolean b);

}
