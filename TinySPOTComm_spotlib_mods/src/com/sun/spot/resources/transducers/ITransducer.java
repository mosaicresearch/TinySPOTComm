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

import com.sun.spot.resources.IResource;

/**
 * Provide basic metadata for a sensor or actuator.
 * <p>
 * Most metadata associated with a transducer should just be put into tags so
 * that it can be searched for when doing a resource lookup.
 * For example: name, vendor, model, version, location, latitude, longitude.
 * <p>
 * Some other metadata is defined by the API for the specific transducer.
 * For example: units, scale, data type, etc.
 * <p>
 * Some other metadata depends on the implementing class so needs to be available
 * via the get methods defined for ITransducer:<br>
 * &nbsp;&nbsp;  description, measurement ranges, and maximum sampling rate.<br>
 * plus other implemented interfaces like {@link IMeasurementInfo}:<br>
 * &nbsp;&nbsp;  minimum value, maximum value, resolution and accuracy,<br>
 * or {@link IMeasurementRange}:<br>
 * &nbsp;&nbsp;  number available ranges, current range, set range, min/max for range
 * 
 * @author Ron Goldman
 */
public interface ITransducer extends IResource {

    /**
     * This method returns a readable description of the sensor.
     * The description should tell the essentials of the sensor, including at least the
     * sensor type (accelerometer, microphone, thermometer).
     * The returned string MUST not be null or an empty string.
     *
     * @return the description of the sensor
     */
    public String getDescription();

    /**
     * Return the maximum sampling rate per second for this sensor.
     *
     * @return the maximum sampling rate per second for this sensor or 0 if not applicable.
     */
    public double getMaxSamplingRate(); // in Hz

    /**
     * Create a SensorEvent of the appropriate type for this sensor.
     *
     * @return a SensorEvent of the appropriate type for this sensor.
     */
    public SensorEvent createSensorEvent();

    /**
     * Save the current sensor state in the specified event.
     *
     * @param evt the event to store the sensor state in
     */
    public void saveEventState(SensorEvent evt);
}
