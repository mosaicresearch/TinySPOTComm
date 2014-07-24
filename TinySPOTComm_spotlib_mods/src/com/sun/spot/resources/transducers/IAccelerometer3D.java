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
 * IAccelerometer3D provides methods that should be meaningful for any 3-axis accelerometer.
 *
 * One set of methods return the current acceleration measured along each axis. 
 *
 * A second set of methods use the acceleration along an axis in order to compute the
 * inclination, or tilt, of that axis.
 *
 * @author Ron Goldman
 */
public interface IAccelerometer3D extends ITransducer {

    public static final int X_AXIS   = 1;
    public static final int Y_AXIS   = 2;
    public static final int Z_AXIS   = 3;
    public static final int ALL_AXES = 4;
    
    /**
     * Read the current acceleration along the X axis.
     *
     * @return the current acceleration in G's along the X axis
     */
    double getAccelX() throws IOException;

    /**
     * Read the current acceleration along the Y axis.
     *
     * @return the current acceleration in G's along the Y axis
     */
    double getAccelY() throws IOException;

    /**
     * Read the current acceleration along the Z axis.
     *
     * @return the current acceleration in G's along the Z axis
     */
    double getAccelZ() throws IOException;

    /**
     * Read the current acceleration for the indicated axis.
     *
     * @param axis which axis to return (ALL_AXES = total acceleration)
     * @return the current acceleration in G's
     */
    double getAccel(int axis) throws IOException;

    /**
     * Compute the current total acceleration.
     * This is the vector sum of the acceleration along the X, Y & Z axes.
     *
     * @return the current total acceleration in G's
     */
    double getAccel() throws IOException;

    /**
     * Return all the current values.
     *
     * @return all current values as array [ Ax, Ay, Az ]
     * @throws IOException
     */
    double[] getAccelValues() throws IOException;


    /**
     * Compute the inclination of the SPOT's X axis.
     * This angle is with respect to any acceleration if the SPOT is moving.
     *
     * @return the current angle of the X axis in radians, in the range of -pi/2 through pi/2.
     */
    double getTiltX() throws IOException;

    /**
     * Compute the inclination of the SPOT's Y axis.
     * This angle is with respect to any acceleration if the SPOT is moving.
     *
     * @return the current angle of the Y axis in radians, in the range of -pi/2 through pi/2.
     */
    double getTiltY() throws IOException;
    
    /**
     * Compute the inclination of the SPOT's Z axis.
     * This angle is with respect to any acceleration if the SPOT is moving.
     *
     * @return the current angle of the Z axis in radians, in the range of -pi/2 through pi/2.
     */
    double getTiltZ() throws IOException;

    /**
     * Compute the inclination of the specified SPOT axis.
     * This angle is with respect to any acceleration if the SPOT is moving.
     *
     * @param axis which axis to return Note: specifying ALL_AXES will throw an IllegalArgumentException.
     * @return the current angle of the indicated axis in radians, in the range of -pi/2 through pi/2.
     */
    double getTilt(int axis) throws IOException;

}
