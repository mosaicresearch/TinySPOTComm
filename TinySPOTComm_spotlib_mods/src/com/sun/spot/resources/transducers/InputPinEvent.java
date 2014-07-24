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
 * Capture the state of a input pin event so it can be reported to a callback.
 */
public class InputPinEvent extends SensorEvent {

    protected boolean high;

    /**
     * Constructor
     *
     * @param sensor the ISwitch sensor
     */
    public InputPinEvent(ITransducer sensor) {
        super(sensor);
    }

    /**
     * Return the sensor as an object of type IInputPin.
     * 
     * @return the sensor cast to IInputPin
     */
    public IInputPin getInputPin() {
        return (IInputPin) sensor;
    }

    /**
     * Record the pin's state.
     *
     * @param high the current pin state
     */
    public void setHigh(boolean high) {
        this.high = high;
        setTime();
    }

    /**
     * Get the pin's event state.
     *
     * @return true if the pin was high.
     */
    public boolean isHigh() {
        return high;
    }

    /**
     * Get the pin's event state.
     *
     * @return true if the pin was low.
     */
    public boolean isLow() {
        return !high;
    }

}
