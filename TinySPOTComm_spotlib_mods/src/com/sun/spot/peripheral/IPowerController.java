/*
 * Copyright 2006-2010 Sun Microsystems, Inc. All Rights Reserved.
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

import com.sun.spot.resources.IResource;

public interface IPowerController extends IResource {

	// PowerController command bytes:
	public static final byte GET_AND_CLEAR_STATUS_CMD = 0;
	public static final byte QUERY_V_CORE             = 1;
	public static final byte QUERY_V_CC               = 2;
	public static final byte QUERY_V_BATT             = 3;
	public static final byte QUERY_TEMPERATURE        = 4;
	public static final byte QUERY_V_EXT              = 5;
	public static final byte QUERY_V_USB              = 6;
	public static final byte QUERY_I_CHARGE           = 7;
	public static final byte QUERY_I_DISCHARGE        = 8;
	// GET_TIME_CMD = 9 not used
	public static final byte GET_ALARM_CMD            = 10;
	public static final byte GET_STRING_LEN_CMD       = 11;
	public static final byte GET_STRING_CMD           = 12;
	public static final byte SET_TIME_CMD             = 13;
	public static final byte SET_ALARM_CMD            = 14;
	// SET_SLEEP_CMD = 15 not used
	public static final byte QUERY_I_MAX              = 16;
	public static final byte SET_INDICATE_CMD         = 17;
	public static final byte QUERY_STARTUP            = 18;
	public static final byte GET_POWER_FAULT_CMD      = 19;
	// RUN_BOOTLOADER = 20 not used
	// QUERY_PROGMEM = 21 not used
	// QUERY_NEXTPROGMEM = 22 not used
    public static final byte SET_SHUTDOWN_TIMEOUT     = 22;
	// FORCE_ADC = 23 not used
	public static final byte SET_CONTROL_CMD          = 24;
    public static final byte SET_STATUS_CMD           = 25;
	public static final byte GET_CONTROL_CMD          = 26;
	public static final byte SET_WATCHDOG_CMD         = 28;
	// QUERY_WATCHDOG = 29
	public static final byte RESET_WATCHDOG_CMD       = 31;
    public static final byte QUERY_STATUS_CMD         = 32;
    public static final byte QUERY_BUTTON_CMD         = 23;

	// getEvents() flags:  (rev 6 or earlier)
	public static final byte COLD_BOOT_EVENT              = 1<<0;   // 1
	public static final byte BUTTON_EVENT                 = 1<<1;   // 2
	public static final byte ALARM_EVENT                  = 1<<2;   // 4
	public static final byte SENSOR_EVENT                 = 1<<3;   // 8
	public static final byte BATTERY_EVENT                = 1<<4;   // 10 new battery or discharged < 3.0V
	public static final byte SLEEP_EVENT                  = 1<<5;   // 20
	public static final byte LOW_BATTERY_EVENT            = 1<<6;   // 40 battery at minimum voltage 3.2V
	public static final byte EXTERNAL_POWER_EVENT = (byte) (1<<7);  // 80  VUSB or VEXT applied during battery operation

	// getEvents() flags:  (rev 8)
	public static final byte BUTTON_EVENT8        = 1<<1;  // 2
	public static final byte ALARM_EVENT8         = 1<<2;  // 4
	public static final byte SENSOR_EVENT8        = 1<<3;  // 8
	public static final byte POWER_CHANGE_EVENT8  = 1<<4;  // 10 usb/ext/battery power change
	public static final byte WATCHDOG_EVENT8      = 1<<6;  // 40 Watchdog timer has expired

	// getPowerFault() flags:
	public static final byte VBATT_FAULT    = 1<<0;     // 1
	public static final byte VUSB_FAULT     = 1<<1;     // 2
	public static final byte VEXT_FAULT     = 1<<2;     // 4
	public static final byte VCC_FAULT      = 1<<3;     // 8
	public static final byte VCORE_FAULT    = 1<<4;     // 10
	public static final byte POWERUP_FAULT  = 1<<5;     // 20
	public static final byte OVERLOAD_FAULT = 1<<6;     // 40
	
    // getStatus() flags:  (rev 8)
	public static final int BATTERY_POWER      = 1<<0;  // 1
	public static final int USB_POWER          = 1<<1;  // 2
	public static final int EXT_POWER          = 1<<2;  // 4
	public static final int LOW_BATTERY        = 1<<3;  // 8
	public static final int RTC_STABLE         = 1<<4;  // 10
	public static final int RTC_VALID          = 1<<5;  // 20
	public static final int BATTERY_CALIBRATED = 1<<6;  // 40
	public static final int COLD_BOOT          = 1<<7;  // 80

    // getButtonEvent() values:  (rev 8)
    public static final int REBOOT      = 1;
    public static final int SHUTDOWN    = 2;
    public static final int ALTBOOT     = 3;    // should never see in Java

    /**
	 * Bit mask value for the {@link #setIndicate(byte)} parameter. 1 indicates that the SPOT
	 * should display the power state using its LEDs.
	 */
	public static final byte SHOW_POWERSTATE = 1<<0;

	/**
	 * Bit mask value for the {@link #setIndicate(byte)} parameter. 1 indicates that the SPOT
	 * should display events using its LEDs.
	 */
	public static final byte SHOW_EVENTS = 1<<1;

	/**
	 * Bit mask value for the {@link #setControl(byte)} parameter. Setting the bit indicates
	 * that the power controller should NOT wake the SPOT main board when it detects external
	 * board interrupts. The default is that the bit is unset, i.e. wake on interrupt is enabled.
	 */
    public static final byte WAKE_ON_INTERRUPT    = 1 << 4;
    public static final byte NO_WAKE_ON_INTERRUPT = 1 << 0;

	/**
	 * Bit mask value for the {@link #setControl(byte)} parameter. Setting the bit indicates that 
	 * the power controller SHOULD shut the SPOT down when it detects loss of external power.
	 * The default is unset, i.e. the SPOT does not shutdown on external power loss.
	 */
    public static final byte SHUTDOWN_EXTERNAL_POWERLOSS    = 1 << 1;
    public static final byte NO_SHUTDOWN_EXTERNAL_POWERLOSS = 1 << 5;

	/**
	 * Bit mask value for the {@link #setControl(byte)} parameter. Setting the bit indicates that
	 * the power controller SHOULD set USB Suspend (USB not enabled).
	 * The default is unset, i.e. USB enabled.
	 */
    public static final byte USB_SUSPEND = 1 << 2;
    public static final byte USB_ENABLE  = 1 << 6;

	/**
	 * Bit mask value for the {@link #setControl(byte)} parameter. Setting the bit indicates that
	 * the power controller SHOULD set USB high power mode.
	 * The default is unset, i.e. USB low power mode.
	 */
    public static final byte USB_HIGH_POWER_MODE = 1 << 3;
    public static final byte USB_LOW_POWER_MODE  = (byte) (1 << 7);

    /**
	 * Get the power control PowerController firmware revision string.
	 * @return the firmware revision string
	 */
	String getRevision();
	
	/**
	 * Get the power control PowerController's time.
	 * @return the time in milliseconds since midnight Jan 1, 1970
	 */
	long getTime();

	/**
	 * Set the power control PowerController's time.
	 * @param systemTimeMillis the time in milliseconds since midnight Jan 1, 1970
	 */
	void setTime(long systemTimeMillis);

	/**
	 * Get the reason for the last power control PowerController interrupt.
	 * This is a bitmask with the following bits ORed together:<ul>
	 * <li>{@link #COLD_BOOT_EVENT} occurs when the attention button is pushed while powered down</li>
	 * <li>{@link #BUTTON_EVENT} occurs when the attention button is pushed while not powered down</li>
	 * <li>{@link #ALARM_EVENT} occurs when a timer alarm has expired</li>
	 * <li>{@link #SENSOR_EVENT} occurs when the sensor board issues an interrupt</li>
	 * <li>{@link #BATTERY_EVENT} occurs when either a new battery is attached or the existing battery is discharged below 3.0V</li>
	 * <li>{@link #SLEEP_EVENT} occurs on wake up from deep sleep</li>
	 * <li>{@link #LOW_BATTERY_EVENT} occurs when the battery reaches the minimum voltage for safe operation (3.2V)</li>
	 * <li>{@link #EXTERNAL_POWER_EVENT} occurs when external power is applied to the USB interface (VUSB or VEXT)</li>
	 * <li>{@link #WATCHDOG_EVENT8} occurs when the watchdog timer has expired</li>
	 *</ul>
	 * @return the PowerController event bits
	 */
	int getEvents();

	/**
	 * Return the ARM CPU core voltage in millivolts (nominally 1800mv).
	 * @return the ARM CPU core voltage (mv)
	 */
	int getVcore();

	/**
	 * Return the main board IO voltage in millivolts (nominally 3000mv).
	 * @return the IO voltage (mv)
	 */
	int getVcc();

	/**
	 * Return the battery supply voltage in millivolts (nominally 2700mv - 4700mv).
	 * This is a rough indicator of remaining battery life. 
         * The battery is nominally 3700mv through most of its state of charge and drops off 
         * pretty quickly towards full discharge. 
         * 
         * <p>At 3500mv the SPOT will start to indicate low battery (power LED switches from green to red) 
         * <p>At 3300mv the SPOT will shutdown automatically into deep sleep 
         * 
	 * @return the battery voltage (mv)
	 */
	int getVbatt();

	/**
	 * Return the voltage supplied by an external power source (if any) in millivolts
	 * (nominally 0mv - 5500mv).
	 * @return the external voltage (mv)
	 */
	int getVext();

	/**
	 * Return the externally supplied USB voltage (if any) in millivolts
	 * (nominally 5000mv).
	 * @return the USB voltage (mv)
	 */
	int getVusb();

	/**
	 * Return the current charging the battery in milliamps.
	 * Only one of Icharge or Idischarge will be be positive at any time. The other will be zero. 
	 * @return the discharge current (mA)
	 */
	int getIcharge();

	/**
	 * Return the current being drawn from the battery in milliamps.
	 * Only one of Icharge or Idischarge will be be positive at any time. The other will be zero. 
	 * @return the discharge current (mA)
	 */
	int getIdischarge();

	/**
	 * Disable automatic synchronisation between PowerController time and System time. This will cause the two
	 * to gradually drift apart and is not recommended for general use. The main legitimate use of
	 * this function is to stop SPI activity when an app needs exclusive use of the SPI pins.
	 * 
	 */
	void disableSynchronisation();

	/**
	 * Re-enable automatic synchronisation after a previous call to disableSynchronisation.
	 * 
	 */
	void enableSynchronisation();

	/**
	 * Return the maximum current (in milliamps) that has been drawn from the battery since the last time this was called.
	 * @return the maximum discharge current (mA) since the last call
	 */
	int getIMax();

	/**
	 * Return the time it took (in microseconds) for the power to stabilize from startup.
	 * @return the time it took (in microseconds) for the power to stabilize from startup.
	 */
	int getStartupTime();

	/**
	 * Return a bit mask of possible power faults.
	 * @return a bit mask of possible power faults.
	 */
	int getPowerFault();

	/**
	 * Set a bit mask to control the power controller LED.
	 * See {@link IPowerController#SHOW_EVENTS} and {@link IPowerController#SHOW_POWERSTATE}
	 */
	void setIndicate(byte mask);

	/**
	 * Set a bit mask to control whether the power controller accepts interrupts from the sensor board
	 * and uses them to wake the SPOT if it is sleeping, and how the power controller deals with loss of 
	 * external power. See {@link IPowerController#WAKE_ON_INTERRUPT} and {@link IPowerController#SHUTDOWN_EXTERNAL_POWERLOSS}
	 */
	void setControl(byte mask);

	/**
	 * Retrieve the current bit mask control settings.
     * @return the current bit mask control settings.
	 */
	int getControl();

	/**
	 * Retrieve the temperature measured from the main board temperature sensor. returned in degrees centigrade.
	 */
	double getTemperature();
	
	/**
	 * @return Answer an {@link IBattery} for access to information about the battery if any.
	 */
	IBattery getBattery();

    /**************************************************
     *
     * Routines for Rev 8 SPOTs
     *
     **************************************************/

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
	void setWatchdog(long time);
    
	/**
	 * restart the watchdog timer. 
	 * restartWatchdog must be called periodically for normal operation 
	 */
	void restartWatchdog();

    /**
     * Return status flags showing current power state, rtc state, etc.
     *
     * @return status flags
     */
    int getStatus();

    /**
     * Return a value indicating the last button event: reboot, shut down or alt boot
     *
     * @return the value indicating the last button event
     */
	int getButtonEvent();

    /**
     * Set the time delay from when IRQ is sent to the ARM notifying it of 
     * the shutdown to powering off ARM. Value is 1 to 255 representing 100msec
     * to 25600msec delay in 100ms increments. Default time out is 3 secs.
     * 
     * @param time in 100ms increments until ARM is powered off
     */
    void setShutdownTimeout(int time);

}
