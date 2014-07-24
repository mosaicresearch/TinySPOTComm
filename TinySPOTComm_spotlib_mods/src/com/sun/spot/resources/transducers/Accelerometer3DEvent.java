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
 * Capture the state of a 3D accelerometer input event so it can be reported to a callback.
 */
public class Accelerometer3DEvent extends SensorEvent {

    protected double x;
    protected double y;
    protected double z;

    /**
     * Constructor
     *
     * @param sensor the IAccelerometer3D sensor
     */
    public Accelerometer3DEvent(ITransducer sensor) {
        super(sensor);
    }

    /**
     * Return the sensor as an object of type IAccelerometer3D.
     * 
     * @return the sensor cast to IAccelerometer3D
     */
    public IAccelerometer3D getAccelerometer3D() {
        return (IAccelerometer3D) sensor;
    }

    /**
     * Record the accelerometer's current state.
     *
     * @param x the current X acceleration
     * @param y the current Y acceleration
     * @param z the current Z acceleration
     */
    public void setAccels(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        setTime();
    }

    /**
     * Get the X acceleration event state.
     *
     * @return the X acceleration
     */
    public double getAccelX() {
        return x;
    }

    /**
     * Get the Y acceleration event state.
     *
     * @return the Y acceleration
     */
    public double getAccelY() {
        return y;
    }

    /**
     * Get the Z acceleration event state.
     *
     * @return the Z acceleration
     */
    public double getAccelZ() {
        return z;
    }

}
