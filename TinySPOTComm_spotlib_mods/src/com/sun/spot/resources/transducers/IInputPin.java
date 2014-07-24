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
 * Interface IInputPin defines access to a single digital input pin.
 */
public interface IInputPin extends ITransducer {
    
    /**
     * Return true if the input bit is low.
     *
     * @return true if the input bit is low.
     */
    public boolean isLow();
    
    /**
     * Return true if the input bit is high.
     *
     * @return true if the input bit is high.
     */
    public boolean isHigh();
    
    /**
     * Block the current thread until the pin's state changes.
     * Note: waitForChange does not inhibit deep sleep.
     */
    public void waitForChange();
    
    
    /**
     * Adds the specified input pin listener to receive callbacks from this
     * input pin. Note: that if pin changes happen too fast some transitions
     * may be missed. At least 3 milliseconds is required between pin changes.
     *
     * @param who the input pin listener to add. 
     */
    public void addIInputPinListener(IInputPinListener who);
    
    /**
     * Removes the specified input pin listener so that it no longer receives
     * callbacks from this input pin. This method performs no function, nor does
     * it throw an exception, if the listener specified by the argument was not
     * previously added to this input pin.  
     *
     * @param who the input pin listener to remove. 
     */
    public void removeIInputPinListener(IInputPinListener who);
    
    /**
     * Return an array of all the input pin listeners registered on this input pin.
     *
     * @return all of this input pin's listeners or an empty array if no
     * input pin listeners are currently registered.
     */
    public IInputPinListener[] getIInputPinListeners();

}
