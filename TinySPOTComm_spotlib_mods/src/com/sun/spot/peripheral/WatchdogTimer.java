/*
 * Copyright 2010 Oracle Corporation. All Rights Reserved.
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
 * Please contact Oracle Corporation, 500 Oracle Parkway, Redwood
 * Shores, CA 94065 or visit www.oracle.com if you need additional
 * information or have any questions.
 */

package com.sun.spot.peripheral;

import com.sun.spot.resources.Resources;

/**
 * Implements a (virtual) watchdog timer that can be used to force the SPOT to
 * reboot. The SPOT has one physical watchdog timer in the power controller.
 * All of the virtual watchdog timers are mapped onto this one physical one.
 * It gets set to expire when the virtual watchdog with the earliest timeout
 * is scheduled to expire.
 */
public class WatchdogTimer {

    private long timeInterval = 0;
    private PowerController8 pctrl;

    WatchdogTimer next;          // for use by PowerController8
    long expirationTime;

    /**
     * Constructor to make a new watchdog timer.
     */
    public WatchdogTimer() {
        pctrl = (PowerController8) Resources.lookup(PowerController8.class);
        if (pctrl == null) {
            throw new SpotFatalException("WatchdogTimer not supported on this SPOT.");
        }
    }

    /**
	 * Start the watchdog timer with the specified timeout interval.
     * The watchdog timer operates in both deep sleep and running modes.
	 * If restart() is not called prior to the watchdog timing out,
     * the SPOT will be rebooted.
     * <p>
	 * For the rev8 SPOT the watchdog timer counts by 256 millisecond ticks and
     * the maximum watchdog timeout value is 549,755,813,000 milliseconds or 17 years.
     *
     * @param timeInterval in milliseconds until watchdog timer will expire.
	 */
	public void start(long timeInterval) {
        this.timeInterval = timeInterval;
        expirationTime = System.currentTimeMillis() + timeInterval;
        pctrl.startWatchdog(this);
    }

	/**
	 * Restart the watchdog timer. This method must be called periodically to
     * prevent the watchdog timer from expiring and rebooting the SPOT.
	 */
	public void restart() {
        expirationTime = System.currentTimeMillis() + timeInterval;
        pctrl.restartWatchdog(this);
    }

    /**
     * Stop this watchdog timer.
     */
    public void stop() {
        timeInterval = 0;
        pctrl.stopWatchdog(this);
    }

    /**
     * Get the current timeout period for the watchdog timer.
     * A value of zero indicates that the watchdog timer is disabled.
     *
     * @return the time interval in milliseconds
     */
    public long getTimeInterval() {
        return timeInterval;
    }

    /**
     * Check if the last reboot was caused by the watchdog timer expiring.
     *
     * @return true if the watchdog timer caused the last reboot
     */
    public static boolean causedLastReboot() {
        PowerController8 pctrl8 = (PowerController8) Resources.lookup(PowerController8.class);
        return pctrl8 != null ? pctrl8.didWatchdogExpire() : false;
    }

}
