/*
 * Copyright 2010 Oracle. All Rights Reserved.
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
 * Please contact Oracle, 16 Network Circle, Menlo Park, CA 94025 or
 * visit www.oracle.com if you need additional information or have
 * any questions.
 */

package com.sun.spot.peripheral;

class PowerController8 extends PowerController {
	
	static final int SPI_CONFIG8 = (ISpiMaster.CSR_NCPHA | ISpiMaster.CSR_BITS_8 | ISpiMaster.CSR_SCBR_MIN  | ISpiMaster.CSR_DLYBCT_MAX);

    private boolean watchdogExpired;
    private int watchdogPeriod;
    private WatchdogTimer first;

	public PowerController8(ISpiMaster spiMaster, PeripheralChipSelect pcs) {
		this.spiMaster = spiMaster;
		this.chipSelectPin = new SpiPcs(pcs, SPI_CONFIG8);
        watchdogExpired = false;
        watchdogPeriod = 0;
        first = null;
	}

	public int getVcore() {
        return makeADCQuery(QUERY_V_CORE, 3000, 1024);
	}

	public int getVcc() {
        return makeADCQuery(QUERY_V_CC, 3904, 1024);
	}

	public int getVbatt() {
        return makeADCQuery(QUERY_V_BATT, 5000, 65536);
	}
	
	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IPowerController#getVext()
	 */
	public int getVext() {
        return makeADCQuery(QUERY_V_EXT, 6000, 1024);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IPowerController#getVusb()
	 */
	public int getVusb() {
        return makeADCQuery(QUERY_V_USB, 6000, 1024);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IPowerController#getIcharge()
	 */
	public int getIcharge() {
        return makeADCQuery(QUERY_I_CHARGE, 3125, 262144);
	}

	public int getIMax() {
		return makeADCQuery(QUERY_I_MAX, 3125, 262144);
	}
	
	public int getStartupTime() {
		return makeADCQuery(QUERY_STARTUP, 1, 1);
	}

	public int getIdischarge() {
        return makeADCQuery(QUERY_I_DISCHARGE, 3125, 262144);
	}
	
	public double getTemperature() {
		return (makeADCQuery(QUERY_TEMPERATURE, 133747, 65536)/10.0) - 273.15;
	}

	protected int makeADCQuery(byte avrQuery, int conversionMultiplier, int conversionDivisor) {
		int adc_value = makeDoubleByteQuery(avrQuery);
		return (adc_value * conversionMultiplier) / conversionDivisor;
	}

    public synchronized IBattery getBattery() {
		if (battery == null) {
			battery = new Battery8(chipSelectPin, spiMaster);
		}
		return battery;
	}

    // rev 8 routines

	public int getEvents() {
        int status = super.getEvents();
        if ((status & WATCHDOG_EVENT8) != 0) {
            watchdogExpired = true;
        }
        return status;
	}

    public int getStatus() {
        return makeDoubleByteQuery(QUERY_STATUS_CMD);
    }

	public int getPowerFault() {
		return makeDoubleByteQuery(IPowerController.GET_POWER_FAULT_CMD);
	}

    public int getButtonEvent() {
        return makeSingleByteQuery(QUERY_BUTTON_CMD);
    }

    /**
     * Set the time delay from when IRQ is sent to the ARM notifying it of
     * the shutdown to powering off ARM. Value is 1 to 255 representing 100msec
     * to 25600msec delay in 100ms increments. Default time out is 3 secs.
     *
     * @param time in 100ms increments until ARM is powered off
     */
    public void setShutdownTimeout(int time) {
		byte[] txBuf = new byte[2];
		txBuf[0] = IPowerController.SET_SHUTDOWN_TIMEOUT;
        txBuf[1] = (byte) time;
		spiMaster.sendAndReceive(chipSelectPin, 2, txBuf, 0, 0, null);
    }

    /**
	 * Start the watchdog timer period to timeout in time milliseconds.
     * The watchdog timer operates in both deep sleep and running modes.
	 * If restartWatchdog() is not called prior to the watchdog timing out,
     * the SPOT will be rebooted.
     * <p>
	 * Disable watchdog timer by setting the time to zero
     * <p>
	 * For the rev8 SPOT the watchdog timer counts by 256 msec ticks and
     * the maximum watchdog timeout value is 549755813000 milliseconds or 17 years.
     *
     * @param time in milliseconds until watchdog timer will expire or zero to
     * disable the watchdog timer.
	 */
    public void setWatchdog(long time) {
		if (time > 549755813000L) {
			throw new IllegalArgumentException("Cannot set watchdog timer > 17 years (549755813000 milliseconds)");
		}
        watchdogPeriod = (int)time;
		int ticks = (int)((time+255)/256);
        byte[] txBuf = new byte[5];
		txBuf[0] = SET_WATCHDOG_CMD;
		for (int i = 4; i > 0; i--) {
			txBuf[i] = (byte)ticks;
			ticks = ticks >> 8;
		}
		spiMaster.sendAndReceive(chipSelectPin, 5, txBuf, 0, 0, null);
	}
	
    /**
     * Get the current timeout period for the watchdog timer.
     * A value of zero indicates that the watchdog timer is disabled.
     *
     * @return the timeout period in seconds
     */
    public int getWatchdog() {
        return watchdogPeriod;
    }

	/**
	 * Restart the watchdog timer. This method must be called periodically to
     * prevent the watchdog timer from expiring and rebooting the SPOT.
	 */
	public void restartWatchdog() {
        if (watchdogPeriod == 0) {
			throw new IllegalStateException("Cannot restart watchdog timer that is not running");
		}
        byte[] txBuf = new byte[1];
		txBuf[0] = RESET_WATCHDOG_CMD;
		spiMaster.sendAndReceive(chipSelectPin, 1, txBuf, 0, 0, null);
	}
	
    /**
     * Check if the last reboot was caused by the watchdog timer expiring.
     *
     * @return true if the watchdog timer caused the last reboot
     */
    public boolean didWatchdogExpire() {
        return watchdogExpired;
    }

    // routines to manage virtual WatchdogTimers

    private void removeWatchdog(WatchdogTimer wd) {
        if (first == null) {
            return;
        } else if (first == wd) {
            first = wd.next;              // splice out watchdog to remove
            if (first == null) {
                setWatchdog(0);     // turn off watchdog
            } else {
                setWatchdog(first.expirationTime - System.currentTimeMillis());
            }
        } else {
            WatchdogTimer last = first;
            while (last.next != null) {
                if (last.next == wd) {
                    last.next = wd.next;  // splice out watchdog to remove
                    break;
                } else {
                    last = last.next;
                }
            }
        }
    }

    private void addWatchdog(WatchdogTimer wd) {
        if (first == null || wd.expirationTime < first.expirationTime) {
            wd.next = first;
            first = wd;
            setWatchdog(first.expirationTime - System.currentTimeMillis());
       } else {
            WatchdogTimer last = first;
            while (last.next != null && last.next.expirationTime < wd.expirationTime) {
                last = last.next;
            }
            wd.next = last.next;
            last.next = wd;
        }
    }

    public void startWatchdog(WatchdogTimer wd) {
        removeWatchdog(wd);
        addWatchdog(wd);
    }

    public void restartWatchdog(WatchdogTimer wd) {
        removeWatchdog(wd);
        addWatchdog(wd);
    }

    public void stopWatchdog(WatchdogTimer wd) {
        removeWatchdog(wd);
    }

}
