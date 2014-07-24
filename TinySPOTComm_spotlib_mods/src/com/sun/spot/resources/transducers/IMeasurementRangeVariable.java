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
 * Used to get/set the measurement range of an adjustible sensor.
 *
 * The min, max, resolution and accuracy of the sensor varies according to the
 * measurement range in use.
 */
public interface IMeasurementRangeVariable extends IMeasurementInfo {

    /**
     * Returns the current measurement range setting of the given sensor.
     * 
     * @return the current measurement range setting of the given sensor as an opaque String.
     */
    public String getCurrentRangeSetting();

    /**
     * Sets the measurement range setting of the sensor.
     *
     * @param range the measurement range setting to be set.
     * @throws java.lang.IllegalArgumentException - if the given range is not
     *                                              a valid range for the sensor.
     */
    public void setCurrentRangeSetting(String range);

}
