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
 * Represents a simple pushbutton switch
 */
public interface ISwitch extends ITransducer {
	
    /**
     * Return true if the switch is NOT pressed
     *
     * @return true if the switch is NOT pressed
     */
    public boolean isOpen();

    /**
     * Return true if the switch IS pressed.
     *
     * @return true if the switch IS pressed
     */
    public boolean isClosed();
    
    /**
     * Block the current thread until the switch's state changes.
     * Note: waitForChange does not inhibit deep sleep.
     */
    public void waitForChange();
    
    /**
     * Adds the specified switch listener to receive callbacks from this switch.
     *
     * @param who the switch listener to add. 
     */
    public void addISwitchListener(ISwitchListener who);
    
    /**
     * Removes the specified switch listener so that it no longer receives
     * callbacks from this switch. This method performs no function, nor does
     * it throw an exception, if the listener specified by the argument was not
     * previously added to this switch.  
     *
     * @param who the switch listener to remove. 
     */
    public void removeISwitchListener(ISwitchListener who);
    
    /**
     * Returns an array of all the switch listeners registered on this switch.
     *
     * @return all of this switch's SwitchListeners or an empty array if no switch
     * listeners are currently registered.
     */
    public ISwitchListener[] getISwitchListeners();

}
