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

import java.io.IOException;

/**
 * Simple interface to read temperature.
 * <p>
 * Note the values returned from methods in the IMeasurementInfo Interface
 * (e.g. getMinValue(), getMaxValue(), getResolution()) should be in Celsius.
 */
public interface ITemperatureInput extends ITransducer {

    /**
     * Convert from Celsius to Fahrenheit.
     *
     * @param val Celsius value to convert
     * @return the input value converted to Fahrenheit
     */
    double convertC2F(double val);

    /**
     * Convert from Fahrenheit to Celsius.
     *
     * @param val Fahrenheit value to convert
     * @return the input value converted to Celsius
     */
    double convertF2C(double val);

    /**
     * Return the current temperature in degrees Celsius.
     *
     * @return the current temperature in degrees Celsius
     * @throws IOException
     */
    double getCelsius() throws IOException;
    
    /**
     * Return the current temperature in degrees Fahrenheit.
     *
     * @return the current temperature in degrees Fahrenheit
     * @throws IOException
     */
    double getFahrenheit() throws IOException;
    
}
