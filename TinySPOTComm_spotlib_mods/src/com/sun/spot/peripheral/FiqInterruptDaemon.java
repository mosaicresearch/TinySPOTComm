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

import java.io.IOException;

import com.sun.spot.peripheral.handler.NullEventHandler;
import com.sun.spot.peripheral.handler.StopVMEventHandler;
import com.sun.spot.resources.Resource;
import com.sun.spot.util.Utils;
import com.sun.squawk.CrossIsolateThread;
import com.sun.squawk.Isolate;
import com.sun.squawk.VM;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 * The FiqInterruptDaemon gives access to the handlers used for various notifications
 * from the power controller. A handler that implements {@link IEventHandler} can be
 * supplied to handle a specific event, replacing the existing handler (all events
 * have a default handler). Note that the handler is called at MAX_SYS_PRIORITY.
 * Your code should reduce the priority as appropriate.
 */
public class FiqInterruptDaemon extends Resource implements Runnable, IDriver, IFiqInterruptDaemon {
	private IPowerController powerController;
    private USBPowerDaemon usb;
	private EventListener buttonHandler;
	private EventListener alarmHandler;
	private EventListener powerOffHandler;
	private EventListener lowBatteryHandler;
	private EventListener externalPowerHandler;
    private EventListener sensorBoardHandler;
    private IEventHandler stopVMHandler;
	private Object deepSleepThreadMonitor = new Object();
	private IAT91_AIC aic;
	private ISpotPins spotPins;
    private int FIQ_ID_MASK;
    private boolean rev8p;
    private int prevExtPower;
    private boolean exitOnButtonPress = true;
	
	public FiqInterruptDaemon(IPowerController powerController, IAT91_AIC aic, ISpotPins spotPins) {
		this.spotPins =spotPins;
		this.aic = aic;
		this.powerController = powerController;
        usb = null;
		alarmHandler = new EventListener(new NullEventHandler("Alarm event ignored"));
		buttonHandler = new EventListener();
        stopVMHandler = new StopVMEventHandler();
		powerOffHandler = new EventListener(new NullEventHandler("Power off"));
		lowBatteryHandler = new EventListener(new NullEventHandler("Low battery"));
		externalPowerHandler = new EventListener(new NullEventHandler("External power applied"));
		sensorBoardHandler = new EventListener(new NullEventHandler("Sensor board interrupt"));
        FIQ_ID_MASK = Spot.getInstance().getAT91_Peripherals().FIQ_ID_MASK;
        rev8p = Spot.getInstance().getHardwareType() >= 8;
        prevExtPower = 0;   // assume ext power off so handler called if on
	}

	public void startThreads() {
		Thread thread = new Thread(this, "Fiq Interrupt Daemon");
		VM.setAsDaemonThread(thread);
		VM.setSystemThreadPriority(thread, VM.MAX_SYS_PRIORITY);
		thread.start();
		
		/*
		 * Create a separate thread that runs when we return from deep sleep. This thread
		 * is unblocked by the setUp method. On return from deep sleep we need to process
		 * any power controller status changes that happened while we were asleep. We can't
		 * afford to just call handlePowerControllerStatusChange from setUp because the
		 * processing of the handlers might take a while. Since this thread is at MAX_PRIORITY
		 * it will take precedence over most user threads once deep sleep set up is finished.
		 */
		Thread returnFromDeepSleepThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
					synchronized (deepSleepThreadMonitor) {
						try {
							deepSleepThreadMonitor.wait();
						} catch (InterruptedException e) {
							// no-op
						}
						handlePowerControllerStatusChange();
					}
				}
		 	}}, 
		 	"FIQ interrupt handler return from deep sleep");
		VM.setAsDaemonThread(returnFromDeepSleepThread);
		returnFromDeepSleepThread.start();
	}

	public void run() {
		// Handle status change once without interrupt in case we got an interesting status bit passed in from the bootloader
		handlePowerControllerStatusChange();

		initFiq();
		Spot.getInstance().getDriverRegistry().add(this);
		while (true) {
			try {
				VM.waitForInterrupt(FIQ_ID_MASK);
				handlePowerControllerStatusChange();
			} catch (IOException e) {
			}
			aic.enableIrq(FIQ_ID_MASK);
			// reset priority in case the handler changed it
			VM.setSystemThreadPriority(Thread.currentThread(), VM.MAX_SYS_PRIORITY);
		}
	}

	private void handlePowerControllerStatusChange() {
		int status = powerController.getEvents();
//		Utils.log("[FiqInterruptDaemon] Handled power controller status of 0x" + Integer.toHexString(status));
		if ((status & IPowerController.SENSOR_EVENT) != 0){
            sensorBoardHandler.signalEvent();
		}
		if ((status & IPowerController.ALARM_EVENT) != 0){
			alarmHandler.signalEvent();
		}
        if (rev8p) {
            int ev = powerController.getButtonEvent();
//            Utils.log("[FiqInterruptDaemon] Button status = " + ev);
            int power = powerController.getStatus();
//            Utils.log("[FiqInterruptDaemon] Power status = 0x" + Integer.toHexString(power));
            if ((status & IPowerController.WATCHDOG_EVENT8) != 0) {
                Utils.log("[FiqInterruptDaemon] Watchdog timer expired");
            }
            if ((status & IPowerController.BUTTON_EVENT) != 0) {
                if (ev == IPowerController.SHUTDOWN) {
                    new Thread("User PowerOff Handler") {
                        public void run() {
                            powerOffHandler.signalEvent();
                        }
                    }.start();
                    VM.stopVM(0);   // should cause shutdown lifecycle listeners to be run
                } else if ((power & IPowerController.COLD_BOOT) != 0) {
//                    Utils.log("[FiqInterruptDaemon] Cold boot");
                } else {
                    buttonHandler.signalEvent();
                    Utils.sleep(100);
                    if (exitOnButtonPress) {
                        stopVMHandler.signalEvent();
                    }
                }
            }
            if ((status & IPowerController.POWER_CHANGE_EVENT8) != 0) {
                if (usb != null) {
                    usb.powerChangeEvent((power & (IPowerController.USB_POWER | IPowerController.EXT_POWER)) != 0);
                }
                if ((power & IPowerController.LOW_BATTERY) != 0) {
                    lowBatteryHandler.signalEvent();
                }
                if ((power & IPowerController.EXT_POWER) != prevExtPower) {
                    prevExtPower = power & IPowerController.EXT_POWER;
                    externalPowerHandler.signalEvent();
                }
            }
        } else {
            if ((status & IPowerController.BUTTON_EVENT) != 0) {
                buttonHandler.signalEvent();
                Utils.sleep(100);
                if (exitOnButtonPress) {
                    stopVMHandler.signalEvent();
                }
            }
            if ((status & IPowerController.COLD_BOOT_EVENT) != 0) {
                powerOffHandler.signalEvent();
            }
            if ((status & IPowerController.LOW_BATTERY_EVENT) != 0) {
                lowBatteryHandler.signalEvent();
            }
            if ((status & IPowerController.BATTERY_EVENT) != 0) {
                if (usb != null) {
                    usb.powerChangeEvent(false);
                }
            }
            if ((status & IPowerController.EXTERNAL_POWER_EVENT) != 0) {
                if (usb != null) {
                    usb.powerChangeEvent(true);
                }
                externalPowerHandler.signalEvent();
            }
        }
	}

    /**
     * Specify whether the VM should exit when the button is pressed.
     *
     * @param enable if true the VM will exit when the button is pressed.
     */
    public void setExitOnButtonPress(boolean enable) {
        exitOnButtonPress = enable;
    }

	/**
	 * Add a handler for power controller time alarms.
	 * It is very unlikely that you will want to create a handler for time alarms.
	 * The default handler logs a message if spot.diagnostics is set.
	 * @param handler the new handler to use
	 */
	public void addAlarmHandler(IEventHandler handler) {
		alarmHandler.addListener(handler);
	}

	public void removeAlarmHandler(IEventHandler handler) {
		alarmHandler.removeListener(handler);
	}

    /**
	 * Add a handler for reset button presses.
	 * The default handler calls VM.stopVM(0).
	 * @param handler the new handler to use
	 */
	public void addButtonHandler(IEventHandler handler) {
		buttonHandler.addListener(handler);
	}

	public void removeButtonHandler(IEventHandler handler) {
		buttonHandler.removeListener(handler);
	}

	/**
	 * Add a handler for poweroff.
	 * The power off event occurs when the user uses the reset button to turn off
	 * the SPOT. The handler has about 400ms to do work before the power goes away.
	 * The default handler does nothing.
	 * @param handler the new handler to use
	 */
	public void addPowerOffHandler(IEventHandler handler) {
		powerOffHandler.addListener(handler);
	}

	public void removePowerOffHandler(IEventHandler handler) {
		powerOffHandler.removeListener(handler);
	}

	/**
	 * Add a handler for low battery warnings.
	 * The default handler logs a message if spot.diagnostics is set.
	 * @param handler the new handler to use
	 */
	public void addLowBatteryHandler(IEventHandler handler) {
		lowBatteryHandler.addListener(handler);
	}

	public void removeLowBatteryHandler(IEventHandler handler) {
		lowBatteryHandler.removeListener(handler);
	}

	/**
	 * Add a handler for external power applied events.
	 * The default handler logs a message if spot.diagnostics is set.
	 * @param handler the new handler to use
	 */
	public void addExternalPowerHandler(IEventHandler handler) {
		externalPowerHandler.addListener(handler);
	}

	public void removeExternalPowerHandler(IEventHandler handler) {
		externalPowerHandler.removeListener(handler);
	}

	/**
	 * Add a handler for sensorboard events.
	 * The default handler logs a message if spot.diagnostics is set.
	 * @param handler the new handler to use
	 */
    public void addSensorBoardHandler(IEventHandler handler) {
		sensorBoardHandler.addListener(handler);
	}

    public void removeSensorBoardHandler(IEventHandler handler) {
		sensorBoardHandler.removeListener(handler);
	}

	/**
	 * Add a handler for power controller time alarms.
	 * It is very unlikely that you will want to create a handler for time alarms.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is no longer returned.
	 * @param handler the new handler to use
	 * @return null
     * @deprecated Please use addAlarmHandler() instead.
	 */
	public IEventHandler setAlarmHandler(IEventHandler handler) {
        IEventHandler old = alarmHandler.popListener();
		addAlarmHandler(handler);
		return old;
	}

	/**
	 * Add a handler for reset button presses.
	 * The default handler calls VM.stopVM(0).
	 * The previous handler is no longer returned.
	 * @param handler the new handler to use
	 * @return null
     * @deprecated Please use addButtonHandler() instead.
	 */
	public IEventHandler setButtonHandler(IEventHandler handler) {
        IEventHandler old = buttonHandler.popListener();
		addButtonHandler(handler);
		return old;
	}

	/**
	 * Add a handler for power off.
	 * The power off event occurs when the user uses the reset button to turn off
	 * the SPOT. The handler has about 400ms to do work before the power goes away.
	 * The default handler does nothing.
	 * The previous handler is no longer returned.
	 * @param handler the new handler to use
	 * @return null
     * @deprecated Please use addPowerOffHandler() instead.
	 */
	public IEventHandler setPowerOffHandler(IEventHandler handler) {
        IEventHandler old = powerOffHandler.popListener();
		addPowerOffHandler(handler);
		return old;
	}

	/**
	 * Add a handler for low battery warnings.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is no longer returned.
	 * @param handler the new handler to use
	 * @return null
     * @deprecated Please use addLowBatteryHandler() instead.
	 */
	public IEventHandler setLowBatteryHandler(IEventHandler handler) {
        IEventHandler old = lowBatteryHandler.popListener();
		addLowBatteryHandler(handler);
		return old;
	}
	
	/**
	 * Add a handler for external power applied events.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is no longer returned.
	 * @param handler the new handler to use
	 * @return null
     * @deprecated Please use addExternalPowerHandler() instead.
	 */
	public IEventHandler setExternalPowerHandler(IEventHandler handler) {
        IEventHandler old = externalPowerHandler.popListener();
		addExternalPowerHandler(handler);
		return old;
	}

	/**
	 * Add a handler for sensorboard events.
	 * The default handler logs a message if spot.diagnostics is set.
	 * The previous handler is no longer returned.
	 * @param handler the new handler to use
	 * @return null
     * @deprecated Please use addSensorBoardHandler() instead.
	 */
    public IEventHandler setSensorBoardHandler(IEventHandler handler) {
        IEventHandler old = sensorBoardHandler.popListener();
		addSensorBoardHandler(handler);
		return old;
	}

    void setUSBPowerHandler(USBPowerDaemon usb) {
        this.usb = usb;
    }

	public String getDriverName() {
		return "FIQ Interrupt Daemon";
	}

	public void setUp() {
		initFiq();
		synchronized (deepSleepThreadMonitor) {
			deepSleepThreadMonitor.notify();
		}
	}

	private void initFiq() {
		spotPins.getAttentionPin().claim();
		aic.configure(FIQ_ID_MASK, 0, IAT91_AIC.SRCTYPE_EXT_LOW_LEVEL);
		aic.enableIrq(FIQ_ID_MASK);
	}

	public void shutDown() {
		tearDown();
	}

	public boolean tearDown() {
		aic.disableIrq(FIQ_ID_MASK);
		spotPins.getAttentionPin().release();
		return true;
	}


    private class EventListener {
        private Vector listeners = new Vector();
        private Hashtable isolates = new Hashtable();
        private IEventHandler defaultHandler;

        public EventListener() {
        }

        public EventListener(IEventHandler def) {
            defaultHandler = def;
        }

        public void addListener(IEventHandler who) {
            if (!listeners.contains(who)) {
                listeners.addElement(who);
                isolates.put(who, Isolate.currentIsolate());
            }
        }

        public void removeListener(IEventHandler who) {
            if (listeners.removeElement(who)) {
                isolates.remove(who);
            }
        }

        public IEventHandler popListener() {
            IEventHandler who = null;
            if (listeners.size() > 0) {
                who = (IEventHandler)listeners.firstElement();
                listeners.removeAllElements();
                isolates.clear();
            }
            return who;
        }


        public IEventHandler[] getListeners() {
            IEventHandler[] list = new IEventHandler[listeners.size()];
            for (int i = 0; i < listeners.size(); i++) {
                list[i] = (IEventHandler) listeners.elementAt(i);
            }
            return list;
        }

        public void signalEvent() {
            if (listeners.isEmpty()) {
                if (defaultHandler != null) {
                    defaultHandler.signalEvent();
                }
            } else {
                for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
                    final IEventHandler who = (IEventHandler) e.nextElement();
                    Isolate iso = (Isolate) isolates.get(who);
                    if (iso != null || !iso.isExited()) {
                        // run in proper context
                        new CrossIsolateThread(iso, "Event Handler Listener") {
                            public void run() {
                                who.signalEvent();
                            }
                        }.start();
                    }
                }
            }
        }
    }
}
