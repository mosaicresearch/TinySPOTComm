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

/**
 * Reflect status of and allow control of an industry standard servo.
 * <a href=http://en.wikipedia.org/wiki/Servomechanism>what is a servo?</a> 
 * @author arshan
 */
public interface IServo extends ITransducer {

    /**
     * Set the ouput pulse width for this servo.
     * Valid values are typically 1000-2000 usec.
     * Once this value is set the ServoController for the servo is responsible for 
     * assuring that the pulse is sent with sufficient regularity to properly control
     * the servo.
     * @param val size of pulse in microseconds
     */
    void setValue(int val);

    /**
     * return the current value of the pulse width
     * @return size of current control pulse in microseconds
     */
    int  getValue();
    
    /**
     * Set the relative position of this servo as a percentage of the position within 
     * the set bounds of this servo.
     * @param val value from 0.0 to 1.0 
     */
    void setPosition(float val);
    
    /**
     * specify the bounds that this servo can operate in
     * @param low the lower bound for the size of this servos pulse in microseconds
     * @param high the upper bound for the size of this servos pulse in microseconds
     */
    void setBounds(int low , int high);

}
