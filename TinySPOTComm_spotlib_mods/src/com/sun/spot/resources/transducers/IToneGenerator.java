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
 * Interface to represent a simple tone generator.
 * Should have something like a real frequency response,
 * to participate in this interface.
 *
 * @author arshan
 * @author ron (modified for Yellow, July 2010)
 */
public interface IToneGenerator extends ITransducer {

    /**
     * start a tone at the specified frequency
     *
     * @param hz value of frequency in Hertz
     */
    void startTone(double hz);

    /**
     * start a tone at the specified frequency for the specified duration
     *
     * @param hz value of frequency in Hertz
     * @param dur the time of the duration in milliseconds
     */
    void startTone(double hz, int dur);

    /**
     * stop current tone
     */
    void stopTone();

    /**
     * set the desired tone frequency
     *
     * @param hz value of frequency in Hertz
     * @deprecated please specify the desired frequency in the call to startTone()
     */
    void setFrequency(double hz);

    /**
     * get the current frequency setting
     *
     * @return frequency value in Hertz
     * @deprecated 
     */
    double getFrequency();
    
    /**
     * Set the duration of current beep time.
     *
     * @param dur the time of the duration in milliseconds
     * @deprecated Please use startTone(double hz, int dur) instead
     */
    void setDuration(int dur);
    
    /**
     * Initiate a tone at the set frequency for the set duration, the beep will 
     * terminate itself asynchronously.
     *
     * @deprecated Please use startTone(double hz, int dur) instead
     */
    void beep();
    
    /**
     * start a tone at the set frequency
     *
     * @deprecated Please use startTone(double hz) instead
     */
    void startTone();
    
    /**
     * end running tone
     *
     * @deprecated Please use stopTone() instead
     */
    void endTone();
}
