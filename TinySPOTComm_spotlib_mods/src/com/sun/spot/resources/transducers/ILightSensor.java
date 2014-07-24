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
 * Simple light sensor.
 *
 * @author arshan & Ron Goldman
 */
public interface ILightSensor extends ITransducer {

    /**
     * Return the current light sensor reading.
     *
     * @return the current light intensity.
     * @throws IOException
     */
    int getValue() throws IOException;

    /**
     * To deal with light sources that change rapidly over time, such as
     * fluorescent light bulbs, it is necessary to take multiple readings
     * and average them. This method sleeps for 1 millisecond between readings.
     * At least one reading is always taken, even if n <= 0.
     *
     * @param n the number of readings to take.
     * @return the averaged light intensity.
     * @throws IOException
     */
    int getAverageValue(int n) throws IOException;

    /**
     * Take 17 readings, one every 1 msec and average them.
     * Equivalent to getAverageValue(17);
     *
     * @return the averaged light intensity.
     * @throws IOException
     */
    int getAverageValue() throws IOException;

}
