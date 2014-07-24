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
 * Access and control of 3-color LEDs
 */
public interface ITriColorLED extends ILed {
    
    /**
     * Set the colour of the LED. The colour will only change if the LED is on, but
     * the values supplied are stored.
     *
     * @param redRGB the intensity of the red portion, in the range 0-255
     * @param greenRGB the intensity of the green portion, in the range 0-255
     * @param blueRGB the intensity of the blue portion, in the range 0-255
     */
    void setRGB(int redRGB, int greenRGB, int blueRGB);
    
    /**
     * Set the color of the LED.
     *
     * @param clr the new color
     */
    void setColor(LEDColor clr);
    
    /**
     * Get the current color of the LED.
     *
     * @return the current color
     */
    LEDColor getColor();
    
    /**
     * Return the current red setting.
     *
     * @return the current red setting
     */
    int getRed();
    
    /**
     * Return the current green setting.
     *
     * @return the current green setting
     */
    int getGreen();
    
    /**
     * Return the current blue setting.
     *
     * @return the current blue setting
     */
    int getBlue();
    
}
