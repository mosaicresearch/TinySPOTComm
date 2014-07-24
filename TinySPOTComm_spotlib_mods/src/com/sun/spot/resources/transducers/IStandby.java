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
 * Provides a way to put a sensor/device into standby mode in order
 * to conserve power.
 *
 */
public interface IStandby {

    /**
     * Put the sensor into standby mode to save power.
     * Note: attempts to access the sensor while it is in standby mode
     * may throw an IllegalStateException.
     *
     * @param sleep if true then put sensor into standby mode
     */
    void setStandbyMode(boolean sleep);

    /**
     * Check if the sensor is currently in standby mode.
     *
     * @return true if the sensor is in standby mode
     */
    boolean isInStandbyMode();

}
