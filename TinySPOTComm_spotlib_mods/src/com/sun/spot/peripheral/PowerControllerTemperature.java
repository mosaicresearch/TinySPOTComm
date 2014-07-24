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
 * Please contact Oracle, 16 Network Circle, Menlo Park, CA 94025 or
 * visit www.oracle.com if you need additional information or have
 * any questions.
 */

package com.sun.spot.peripheral;

import com.sun.spot.resources.Resource;
import com.sun.spot.resources.transducers.ITemperatureInput;
import com.sun.spot.resources.transducers.IMeasurementInfo;
import com.sun.spot.resources.transducers.ITransducer;
import com.sun.spot.resources.transducers.SensorEvent;
import com.sun.spot.resources.transducers.TemperatureInputEvent;
import java.io.IOException;

class PowerControllerTemperature extends Resource implements ITransducer, ITemperatureInput, IMeasurementInfo {

    private IPowerController pctrl;
    private int hwRev;
    
	public PowerControllerTemperature(IPowerController pc, int rev) {
        pctrl = pc;
        hwRev = rev;
	}

    // Routines for ITemperatureInput

    private static final int    MIN_TEMP_CELSIUS6      = -40;
    private static final int    MIN_TEMP_FAHRENHEIT6   = -40;
    private static final int    MAX_TEMP_CELSIUS6      = 125;
    private static final int    MAX_TEMP_FAHRENHEIT6   = 257;
    private static final double RESOLUTION_CELSIUS6    = 1.0;
    private static final double RESOLUTION_FAHRENHEIT6 = 1.8;
    private static final double ACCURACY_6             = 0.01;

    // rev 8 constants

    private static final int    MIN_TEMP_CELSIUS8      = -50;
    private static final int    MIN_TEMP_FAHRENHEIT8   = -58;
    private static final int    MAX_TEMP_CELSIUS8      = 120;
    private static final int    MAX_TEMP_FAHRENHEIT8   = 248;
    private static final double RESOLUTION_CELSIUS8    = 0.5;
    private static final double RESOLUTION_FAHRENHEIT8 = 0.9;
    private static final double ACCURACY_8             = 0.01;


    /**
     * Convert from Celsius to Fahrenheit.
     *
     * @param val Celsius value to convert
     * @return the input value converted to Fahrenheit
     */
    public double convertC2F(double val) {
        return ((9.0 / 5.0) * val) + 32.0;
    }

    /**
     * Convert from Fahrenheit to Celsius.
     *
     * @param val Fahrenheit value to convert
     * @return the input value converted to Celsius
     */
    public double convertF2C(double val) {
        return (5.0 / 9.0) * (val - 32.0);
    }

    /**
     * Return the current temperature in degrees Celsius.
     *
     * @return the current temperature in degrees Celsius
     * @throws IOException
     */
    public double getCelsius() {
        return pctrl.getTemperature();
    }

    /**
     * Return the current temperature in degrees Fahrenheit.
     *
     * @return the current temperature in degrees Fahrenheit
     * @throws IOException
     */
    public double getFahrenheit()  {
        return convertC2F(pctrl.getTemperature());
    }

    public String getDescription() {
        return hwRev < 8 ? "MCP9700A internal thermometer" : "LTC2487 internal thermometer";
    }

    public double getMaxSamplingRate() {
        return 0;
    }

    public double getMinValue() {
        return hwRev < 8 ? MIN_TEMP_CELSIUS6 : MIN_TEMP_CELSIUS8;
    }

    public double getMaxValue() {
        return hwRev < 8 ? MAX_TEMP_CELSIUS6 : MAX_TEMP_CELSIUS8;
    }

    public double getResolution() {
        return hwRev < 8 ? RESOLUTION_CELSIUS6 : RESOLUTION_CELSIUS8;
    }

    public double getAccuracy() {
        return hwRev < 8 ? ACCURACY_6 : ACCURACY_8;
    }

    public SensorEvent createSensorEvent() {
        SensorEvent evt = new TemperatureInputEvent(this);
        return evt;
    }

    public void saveEventState(SensorEvent evt) {
        ((TemperatureInputEvent)evt).setCelsius(getCelsius());
    }

}
