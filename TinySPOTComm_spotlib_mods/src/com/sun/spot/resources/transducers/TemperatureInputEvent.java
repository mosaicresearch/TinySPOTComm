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
 * Capture the state of a temperature input event so it can be reported to a callback.
 */
public class TemperatureInputEvent extends SensorEvent {

    protected double value;   // in Celsius

    /**
     * Constructor
     *
     * @param sensor the ITemperatureInput sensor
     */
    public TemperatureInputEvent(ITransducer sensor) {
        super(sensor);
    }

    /**
     * Return the sensor as an object of type ITemperatureInput.
     * 
     * @return the sensor cast to ITemperatureInput
     */
    public ITemperatureInput getTemperatureInput() {
        return (ITemperatureInput) sensor;
    }

    /**
     * Record the temperature from the sensor.
     *
     * @param val the current temperature in Celsius
     */
    public void setCelsius(double val) {
        value = val;
        setTime();
    }

    /**
     * Record the temperature from the sensor.
     *
     * @param val the current temperature in Fahrenheit
     */
    public void setFahrenheit(double val) {
        setCelsius(((ITemperatureInput)sensor).convertF2C(val));
    }

    /**
     * Get the event temperature in Celsius.
     *
     * @return the event temperature in Celsius.
     */
    public double getCelsius() {
        return value;
    }

    /**
     * Get the event temperature in Fahrenheit.
     *
     * @return the event temperature in Fahrenheit.
     */
    public double getFahrenheit() {
        return ((ITemperatureInput)sensor).convertC2F(getCelsius());
    }
    
}
