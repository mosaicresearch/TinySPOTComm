/*
 * Copyright 2006-2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * Color class to be used by TriColorLED.
 * 
 * Overloaded as a container for some static definitions of regularly used colors.
 *
 * @author arshan
 */
public class LEDColor {
    
    /**
     * standard definition of the color red
     */
    public static final LEDColor RED = new LEDColor(255,0,0);
    /**
     * standard definition of the color green
     */
    public static final LEDColor GREEN = new LEDColor(0,255,0);
    /**
     * standard definition of the color blue
     */
    public static final LEDColor BLUE = new LEDColor(0,0,255);
 
    /**
     * standard definition of the color cyan
     */
    public static final LEDColor CYAN = new LEDColor(0,255,255);
    /**
     * standard definition of the color magenta
     */
    public static final LEDColor MAGENTA = new LEDColor(255,0,255);
    /**
     * non-standard definition of the color yellow (color corrected for LEDs)
     */
    public static final LEDColor YELLOW = new LEDColor(255,128,0);

    /**
     * standard definition of the color turquoise
     */
    public static final LEDColor TURQUOISE = new LEDColor(0,100,255);
    /**
     * standard definition of the color puce
     */
    public static final LEDColor PUCE = new LEDColor(204,136,153);
    /**
     * standard definition of the color mauve
     */
    public static final LEDColor MAUVE = new LEDColor(153,51,102);
    /**
     * standard definition of the color chartreuse
     */
    public static final LEDColor CHARTREUSE = new LEDColor(127,255,0);
    /**
     * non-standard definition of the color orange (color corrected for LEDs)
     */
    public static final LEDColor ORANGE = new LEDColor(255,32,0);
    /**
     * standard definition of the color white
     */
    public static final LEDColor WHITE = new LEDColor(255,255,255);

    
    private int red   = 0;
    private int green = 0;
    private int blue  = 0;
    
    /**
     * Creates a new instance of LEDColor
     * @param r initial values of the red value, range 0-255
     * @param g initial values of the green value, range 0-255
     * @param b initial values of the blue value, range 0-255
     */
    public LEDColor(int r , int g , int b) {
        red = r;
        green = g;
        blue = b;
    }
    
    /**
     * return the value of the red portion of this color
     * @return value of the red portion of this color, range 0-255
     */
    public int red()   { return red; }
    public int green() { return green;}
    public int blue()  { return blue;}
   
    public void setRGB(int r , int g , int b) { red = r ; green = g ; blue = b;}
    public void setRed(int val)   { red = val; }
    public void setGreen(int val) { green = val; } 
    public void setBlue(int val)  { blue  = val; }

    public boolean equals(Object obj) {
        if (obj instanceof LEDColor) {
            LEDColor clr = (LEDColor)obj;
            return (clr.red() == red) &&
                    (clr.green() == green) &&
                    (clr.blue() == blue);
        } else return false;
    }
    
    public int hashCode() {
        return (red & 0xff) << 16 | (green & 0xff) << 8 | (blue & 0xff);
    }
}
