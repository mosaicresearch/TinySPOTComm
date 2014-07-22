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

package com.sun.spot.peripheral;

import java.io.*;

import com.sun.spot.util.Utils;
import com.sun.squawk.*;
import com.sun.squawk.vm.*;


class USBPowerDaemon implements IDriver, IUSBPowerDaemon {

	// states as reported by the C code
	static final int USB_STATE_RESET		= 0;
	static final int USB_STATE_CONFIGURED	= 1;
	static final int USB_STATE_READY		= 3;


	public static final int EVENT_POWER_ON = 0;
	public static final int EVENT_POWER_OFF = 1;
	public static final int EVENT_RESET = 2;
	public static final int EVENT_ENUMERATED = 3;
	public static final int EVENT_TIMEOUT = 4;
	
	private ILTC3455 ltc;
	private int currentState;
	private int currentUSBState;
	private PIOPin usbPwrPin;
	protected static final long EXPECTED_ENUM_TIME = 5000;
	private TimerThread timerThread;
	protected boolean usbPowerIsAvailable;

	public USBPowerDaemon(ILTC3455 ltc, PIOPin usbPwrPin) {
		this.ltc = ltc;
		this.usbPwrPin = usbPwrPin;
		ltc.setSuspended(false);
		currentState = STATE_UNCONNECTED;
	}
	
	public void startThreads() {
		setUp();
		Thread powerStateChangeThread = new PowerStateChangeThread();
		VM.setAsDaemonThread(powerStateChangeThread);
		powerStateChangeThread.setPriority(Thread.MAX_PRIORITY-1);
		powerStateChangeThread.start();
		Thread usbStateChangeThread = new UsbStateChangeThread();
		VM.setAsDaemonThread(usbStateChangeThread);
		usbStateChangeThread.setPriority(Thread.MAX_PRIORITY-1);
		usbStateChangeThread.start();
	}
	
	private class UsbStateChangeThread extends Thread {
		public void run() {
			int usbState = getUsbState();
			int event = determineUSBEvent(usbState);
			currentUSBState = usbState;
			if (event != EVENT_RESET) {
				processEvent(event);			
			}
			while (true) {
				try {
					VM.waitForInterrupt(IAT91_Peripherals.UDP_ID_MASK);
					int newState = getUsbState();
					if (currentUSBState != newState) {
						event = determineUSBEvent(newState);
						currentUSBState = newState;
						processEvent(event);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class PowerStateChangeThread extends Thread {
		
		public void run() {
			while (true) {
				if (usbPowerIsAvailable != usbPwrPin.isHigh()) {
					usbPowerIsAvailable = !usbPowerIsAvailable;
					int event = usbPowerIsAvailable ? EVENT_POWER_ON : EVENT_POWER_OFF;
					processEvent(event);
				} else {
					try {
						usbPwrPin.pio.waitForIrq(usbPwrPin.pin);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					usbPwrPin.pio.enableIrq(usbPwrPin.pin);					
				}
			}
		}
	}

	private int determineUSBEvent(int newState) {
		switch (newState) {
			case USB_STATE_CONFIGURED:
			case USB_STATE_READY:
				return EVENT_ENUMERATED;
			case USB_STATE_RESET:
				return EVENT_RESET;
			default:
				return -1;
		}
	}

	synchronized void processEvent(int event) {
//		Spot.getInstance().log("[USB Power Daemon] Processing event " + event + " in state " + currentState);
		switch (event) {
			case EVENT_POWER_ON: {
				switch (currentState) {
					case STATE_UNCONNECTED:
						startTimer();
						currentState = STATE_AWAITING_ENUM;
						break;
					case STATE_AWAITING_ENUM:
					case STATE_ENUMERATED:
					case STATE_BATTERY:
						error(event);
				}
			}
			break;
			case EVENT_POWER_OFF: {
				switch (currentState) {
					case STATE_UNCONNECTED:
						error(event);
						break;
					case STATE_AWAITING_ENUM:
						cancelTimer();
						currentState = STATE_UNCONNECTED;
						break;
					case STATE_ENUMERATED:
					case STATE_BATTERY:
						Utils.log("[USB Power Daemon] Setting to low USB power");
						ltc.setHighPower(false);
						currentState = STATE_UNCONNECTED;
				}
			}
			break;
			case EVENT_RESET: {
				switch (currentState) {
					case STATE_UNCONNECTED:
						// reset does happen in STATE_UNCONNECTED shortly after pulling the cable out
					case STATE_AWAITING_ENUM:
						break;
					case STATE_ENUMERATED:
						currentState = STATE_AWAITING_ENUM;
						break;
					case STATE_BATTERY:
						error(event);
				}
			}
			break;
			case EVENT_ENUMERATED: {
				switch (currentState) {
					case STATE_UNCONNECTED:
						// ignore this - the USB driver doesn't reset when the
						// plug is pulled, so you can get this event on a restart
						break;
					case STATE_AWAITING_ENUM:
						cancelTimer();
						Utils.log("[USB Power Daemon] USB in high power");
						// No need to set high power pin here, as this is done by the C driver for us.
						currentState = STATE_ENUMERATED;
						break;
					case STATE_ENUMERATED:
						break;
					case STATE_BATTERY:
						error(event);
				}
			}
			break;
			case EVENT_TIMEOUT:	{
				switch (currentState) {
					case STATE_UNCONNECTED:
						error(event);
						break;
					case STATE_AWAITING_ENUM:
						Utils.log("[USB Power Daemon] Setting to high USB power");
						ltc.setHighPower(true);
						currentState = STATE_BATTERY;
						break;
					case STATE_ENUMERATED:
					case STATE_BATTERY:
						error(event);
				}
			}
		}
	}

	private void cancelTimer() {
		if (timerThread != null) {
			timerThread.stopTimer();
		}
		timerThread = null;
	}

	private void error(int event) {
		String msg = "[USB Power Daemon] Unexpected event " + event + " in state " + currentState;
		Utils.log(msg);
//		throw new SpotFatalException(msg);
	}

	private void startTimer() {
		Utils.log("[USB Power Daemon] Starting timer");
		cancelTimer();
		timerThread = new TimerThread();
		timerThread.setPriority(Thread.MAX_PRIORITY-1);
		timerThread.start();
	}
	
	private class TimerThread extends Thread {
		private boolean stopped = false;
		
		public void stopTimer() {
			stopped = true;
			this.interrupt();
		}
		
		public boolean isStopped() {
			return stopped;
		}

		public void run() {
			try {
				Thread.sleep(EXPECTED_ENUM_TIME);
			} catch (InterruptedException e) {
			}
			if (!stopped) {
				processEvent(EVENT_TIMEOUT);
				stopped = true;
			}
		}
	}

	public String getDriverName() {
		return "USB power daemon";
	}

	public boolean tearDown() {
		usbPwrPin.release();
		return true;
	}

	public void shutDown() {
	}

	public void setUp() {
		usbPwrPin.claim();
		usbPwrPin.openForInput();
		usbPowerIsAvailable = usbPwrPin.isHigh();
		currentState = STATE_UNCONNECTED;
		if (usbPowerIsAvailable) {
			int usbState = getUsbState();
			if (usbState == USB_STATE_CONFIGURED || usbState == USB_STATE_READY) {
				currentState = STATE_ENUMERATED;
				Utils.log("[USB Power Daemon] ");
			} else {
				processEvent(EVENT_POWER_ON);
			}
		}
		usbPwrPin.pio.enableIrq(usbPwrPin.pin);
	}

	void setCurrentState(int state) {
		currentState = state;
	}

	public boolean isTimerRunning() {
		return timerThread != null && !timerThread.isStopped();
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IUSBPowerDaemon#getCurrentState()
	 */
	public int getCurrentState() {
		return currentState;
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IUSBPowerDaemon#isUsbPowered()
	 */
	public boolean isUsbPowered() {
		return usbPowerIsAvailable;
	}
	
	public boolean isUsbEnumerated() {
		return currentState==STATE_ENUMERATED;
	}

	public boolean isUsbInUse() {
		return currentUSBState==USB_STATE_READY;
	}

	private int getUsbState() {
		return VM.execSyncIO(ChannelConstants.USB_GET_STATE, 0, 0, 0, 0, 0, 0, null, null);
	}


}