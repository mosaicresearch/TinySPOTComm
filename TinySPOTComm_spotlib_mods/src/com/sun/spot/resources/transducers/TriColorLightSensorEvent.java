/*
 * Copyright 2010 Oracle. All Rights Reserved.
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
 * Please contact Oracle, 16 Network Circle, Menlo Park, CA 94025
 * or visit www.oracle.com if you need additional information or have
 * any questions.
 */

package com.sun.spot.resources.transducers;

/**
 * Capture the state of a tri color light sensor event so it can be reported to a callback.
 */
public class TriColorLightSensorEvent extends LightSensorEvent {

    protected int red;
    protected int green;
    protected int blue;
    protected int clear;

    /**
     * Constructor
     *
     * @param sensor the ISwitch sensor
     */
    public TriColorLightSensorEvent(ITransducer sensor) {
        super(sensor);
    }

    /**
     * Return the sensor as an object of type ITriColorLightSensor.
     * 
     * @return the sensor cast to ITriColorLightSensor
     */
    public ITriColorLightSensor getTriColorLightSensor() {
        return (ITriColorLightSensor) sensor;
    }

    /**
     * Record the event state light sensor values.
     *
     * @param red   the light sensor red value.
     * @param green the light sensor green value.
     * @param blue  the light sensor blue value.
     * @param clear the light sensor clear value.
     */
    public void setValue(int red, int green, int blue, int clear) {
        this.red   = red;
        this.green = green;
        this.blue  = blue;
        this.clear = clear;
        this.value = clear;
        setTime();
    }

    /**
     * Get the event state light sensor red value.
     *
     * @return the event's light sensor red value
     */
    public int getRedValue() {
        return red;
    }

    /**
     * Get the event state light sensor green value.
     *
     * @return the event's light sensor green value
     */
    public int getGreenValue() {
        return green;
    }

    /**
     * Get the event state light sensor blue value.
     *
     * @return the event's light sensor blue value
     */
    public int getBlueValue() {
        return blue;
    }

    /**
     * Get the event state light sensor clear value.
     *
     * @return the event's light sensor clear value
     */
    public int getClearValue() {
        return clear;
    }

}
