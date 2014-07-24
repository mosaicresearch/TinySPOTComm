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
 * Capture the state of a switch event so it can be reported to a callback.
 */
public class SwitchEvent extends SensorEvent {

    protected boolean pressed;

    /**
     * Constructor
     *
     * @param sensor the ISwitch sensor
     */
    public SwitchEvent(ITransducer sensor) {
        super(sensor);
    }

    /**
     * Return the sensor as an object of type ITemperatureInput.
     * 
     * @return the sensor cast to ITemperatureInput
     */
    public ISwitch getSwitch() {
        return (ISwitch) sensor;
    }

    /**
     * Record the switch state.
     *
     * @param pressed the current switch state
     */
    public void setPressed(boolean pressed) {
        this.pressed = pressed;
        setTime();
    }

    /**
     * Get the switch event state.
     *
     * @return true if the switch was pressed.
     */
    public boolean isPressed() {
        return pressed;
    }

}
