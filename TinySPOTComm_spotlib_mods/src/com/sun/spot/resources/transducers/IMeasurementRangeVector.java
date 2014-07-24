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
 * Just like {@link IMeasurementRange}, but for sensors that return vector (N-tuple) values.
 */
public interface IMeasurementRangeVector extends IMeasurementInfoVector {

    /**
     * Returns the number of possible measurement ranges for this given sensor.
     *
     * @return the number of possible measurement ranges for this given sensor.
     */
    public int getNumberRanges();

    /**
     * Returns the index of the current measurement range of the given sensor.
     * 
     * @return the current measurement range of the given sensor.
     */
    public int getCurrentRange();

    /**
     * Sets the measurement range of the sensor.
     *
     * @param range the index of the measurement range to be set.
     * @throws java.lang.IllegalArgumentException - if the given range is not
     *                                              a valid range for the sensor.
     */
    public void setCurrentRange(int range);

    /**
     * Returns the Nth minimum values vector of the specified measurement range.
     *
     * @param  index
     * @return an array of the Nth min values of the specified measurement range
     *
     * @throws java.lang.IllegalArgumentException - if index >= size or index < 0
     */
    public double[] getMinValue(int index);

    /**
     * Returns the Nth maximum values vector of the specified measurement range.
     *
     * @param  index
     * @return an array of the Nth max values of the specified measurement range
     *
     * @throws java.lang.IllegalArgumentException - if index >= size or index < 0
     */
    public double[] getMaxValue(int index);

    /**
     * Returns the Nth resolution vector of the specified measurement range.
     *
     * @param  index
     * @return an array of the Nth resolution values of the specified measurement range
     *
     * @throws java.lang.IllegalArgumentException - if index >= size or index < 0
     */
    public double[] getResolution(int index);

    /**
     * Returns the Nth accuracy vector of the specified measurement range.
     *
     * @param  index
     * @return an array of the Nth accuracy values of the specified measurement range
     *
     * @throws java.lang.IllegalArgumentException - if index >= size or index < 0
     */
    public double[] getAccuracy(int index);


}
