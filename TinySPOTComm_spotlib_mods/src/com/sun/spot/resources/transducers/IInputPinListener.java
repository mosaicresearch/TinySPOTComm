/*
 * Copyright 2010 Sun Microsystems, Inc. All Rights Reserved.
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
 * Implemented by classes wanting a call back when the input pin state changes.
 * <p>
 * Note: that if pin changes happen too fast some transitions may be missed.
 * At least 3 milliseconds is required between pin changes.
 * <p>
 * Note: to compare the IInputPin argument passed to your callback with
 * an IIOPin you should use the equals() method, e.g.
 * <pre>
 * IIOPin d0 = (IIOPin)Resources.lookup(IIOPin.class, "D0");
 * ...
 *     d0.addIInputPinListener(this);
 * 
 * public void pinSetHigh(InputPinEvent evt) {
 *     if (d0.equals(evt.getInputPin())) {
 *         // handle pin D0 going high...
 *     }
 * }
 * </pre>
 * 
 */
public interface IInputPinListener {
    
    /**
     * Callback for when the input pin state changes from low to high.
     * 
     * @param evt the SensorEvent describing this event
     */
    public void pinSetHigh(InputPinEvent evt);

    /**
     * Callback for when the input pin state changes from high to low.
     *
     * @param evt the SensorEvent describing this event
     */
    public void pinSetLow(InputPinEvent evt);

}
