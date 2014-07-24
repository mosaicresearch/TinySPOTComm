/*
 * Copyright 2007-2010 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.peripheral;

import com.sun.spot.resources.IResource;

public interface IFiqInterruptDaemon extends IResource {

    /**
     * Specify whether the VM should exit when the button is pressed.
     *
     * @param enable if true the VM will exit when the button is pressed.
     */
    void setExitOnButtonPress(boolean enable);

	/**
	 * Add a handler for power controller time alarms.
	 * It is very unlikely that you will want to create a handler for time alarms.
	 * The default handler logs a message if spot.diagnostics is set.
	 * @param handler the new handler to use
	 */
	void addAlarmHandler(IEventHandler handler);

	/**
	 * Remove a handler for power controller time alarms.
	 * @param handler the new handler to remove
	 */
    void removeAlarmHandler(IEventHandler handler);

	/**
	 * Add a handler for reset button presses.
	 * The default handler calls VM.stopVM(0).
	 * @param handler the new handler to use
	 */
	void addButtonHandler(IEventHandler handler);

	/**
	 * Remove a handler for reset button presses.
	 * @param handler the new handler to remove
	 */
    void removeButtonHandler(IEventHandler handler);

	/**
	 * Add a handler for power off.
	 * The power off event occurs when the user uses the reset button to turn off
	 * the SPOT. The handler has about 400ms to do work before the power goes away.
	 * The default handler does nothing.
	 * @param handler the new handler to use
	 */
	void addPowerOffHandler(IEventHandler handler);

	/**
	 * Remove a handler for power off.
	 * @param handler the new handler to remove
	 */
    void removePowerOffHandler(IEventHandler handler);

	/**
	 * Add a handler for low battery warnings.
	 * The default handler logs a message if spot.diagnostics is set.
	 * @param handler the new handler to use
	 */
	void addLowBatteryHandler(IEventHandler handler);

	/**
	 * Remove a handler for low battery warnings.
	 * @param handler the new handler to remove
	 */
    void removeLowBatteryHandler(IEventHandler handler);

	/**
	 * Add a handler for external power applied events.
	 * The default handler logs a message if spot.diagnostics is set.
	 * @param handler the new handler to use
	 */
	void addExternalPowerHandler(IEventHandler handler);

	/**
	 * Remove a handler for external power applied events.
	 * @param handler the new handler to remove
	 */
    void removeExternalPowerHandler(IEventHandler handler);

	/**
	 * Add a handler for sensorboard events.
	 * The default handler logs a message if spot.diagnostics is set.
	 * @param handler the new handler to use
	 */
    void addSensorBoardHandler(IEventHandler handler);

	/**
	 * Remove a handler for sensorboard events.
	 * @param handler the new handler to remove
	 */
    void removeSensorBoardHandler(IEventHandler handler);

	/**
	 * Add a handler for power controller time alarms.
	 * It is very unlikely that you will want to create a handler for time alarms.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is no longer returned.
	 * @param handler the new handler to use
	 * @return null
     * @deprecated Please use addAlarmHandler() instead.
	 */
	IEventHandler setAlarmHandler(IEventHandler handler);

	/**
	 * Add a handler for reset button presses.
	 * The default handler calls VM.stopVM(0).
	 * The previous handler is no longer returned.
	 * @param handler the new handler to use
	 * @return null
     * @deprecated Please use addButtonHandler() instead.
	 */
	IEventHandler setButtonHandler(IEventHandler handler);

	/**
	 * Add a handler for poweroff.
	 * The power off event occurs when the user uses the reset button to turn off
	 * the SPOT. The handler has about 400ms to do work before the power goes away.
	 * The default handler does nothing.
	 * The previous handler is no longer returned.
	 * @param handler the new handler to use
	 * @return null
     * @deprecated Please use addPowerOffHandler() instead.
	 */
	IEventHandler setPowerOffHandler(IEventHandler handler);

	/**
	 * Add a handler for low battery warnings.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is no longer returned.
	 * @param handler the new handler to use
	 * @return null
     * @deprecated Please use addLowBatteryHandler() instead.
	 */
	IEventHandler setLowBatteryHandler(IEventHandler handler);

	/**
	 * Add a handler for external power applied events.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is no longer returned.
	 * @param handler the new handler to use
	 * @return null
     * @deprecated Please use addExternalPowerHandler() instead.
	 */
	IEventHandler setExternalPowerHandler(IEventHandler handler);

	/**
	 * Add a handler for sensorboard events.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is no longer returned.
	 * @param handler the new handler to use
	 * @return null
     * @deprecated Please use addSensorBoardHandler() instead.
	 */
    IEventHandler setSensorBoardHandler(IEventHandler handler);

}
