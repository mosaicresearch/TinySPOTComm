/*
 * Copyright 2009-2010 Sun Microsystems, Inc. All Rights Reserved.
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
 * Access and control of an array of 3 color LEDs
 */
public interface ITriColorLEDArray extends ICompositeTransducer {

    /**
     * Return the number of LEDs in the array
     *
     * @return the number of LEDs in the array
     */
    public int size();

    /**
     * Return the Nth LED in the array
     *
     * @param i the index of the LED to return
     * @return the specified LED in the array
     */
    public ITriColorLED getLED(int i);

    /**
     * Return all the LEDs in an array
     *
     * @return the LEDs in an array
     */
    public ITriColorLED[] toArray();

    /**
     * Turn all the LEDs on
     */
    public void setOn();

    /**
     * Turn all the LEDs off
     */
    public void setOff();

    /**
     * Set all the LED's state
     *
     * @param on true to set all the LEDs on, false to set them off.
     */
    public void setOn(boolean on);

    /**
     * Set each LED's state based on the bits flag.
     *
     * @param bits set LED N on if (bits & 1 << N) != 0
     */
    public void setOn(int bits);

    /**
     * Set the colour of all the LEDs in the array. The colour will only change if the LED is on, but
     * the values supplied are stored.
     *
     * @param redRGB the intensity of the red portion, in the range 0-255
     * @param greenRGB the intensity of the green portion, in the range 0-255
     * @param blueRGB the intensity of the blue portion, in the range 0-255
     */
    void setRGB(int redRGB, int greenRGB, int blueRGB);
    
    /**
     * Set the color of the LEDs.
     *
     * @param clr the new color
     */
    void setColor(LEDColor clr);
    
}
