/*
 * Copyright 2010 Oracle Corporation. All Rights Reserved.
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
 * Please contact Oracle Corporation, 500 Oracle Parkway, Redwood
 * Shores, CA 94065 or visit www.oracle.com if you need additional
 * information or have any questions.
 */

package com.sun.spot.resources.transducers;

/**
 * Just like {@link IMeasurementInfo}, but for sensors that return vector (N-tuple) values.
 * This interface allows an application to get measurement information for a sensor:
 * the min and max value, the resolution and the accuracy of the sensor.
 */
public interface IMeasurementInfoVector {

    /**
     * Returns the current minimum value of the sensor (in the current measurement range).
     *
     * @return an array of the min values of the sensor
     */
    public double[] getMinValue();

    /**
     * Returns the current maximum value of the sensor (in the current measurement range).
     *
     * @return an array of the max values of the sensor
     */
    public double[] getMaxValue();

    /**
     * Returns the resolution of the range (in the current measurement range).
     * The resolution is the minimum amount of change in the input of a sensor
     * that can be detected as a change in the output.
     * Resolution is a property of the transducer of a sensor but the input and
     * output ranges and word length of the A/D converter usually set the
     * limitations for the resolution of a digital sensor.
     *
     * @return an array of the resolution values of the sensor
     */
    public double[] getResolution();

    /**
     * Returns the accuracy of the sensor (in the current measurement range)
     * reading (=data value). Because this accuracy value describes the relative
     * measurement error, greater values indicate a less accurate sensor.
     * The accuracy value is within: 0 &le; accuracy &lt; 1
     *
     * @return an array of the accuracy values of the sensor.
     * If the accuracy is not known, a value of -1 is returned.
     */
    public double[] getAccuracy();

}
