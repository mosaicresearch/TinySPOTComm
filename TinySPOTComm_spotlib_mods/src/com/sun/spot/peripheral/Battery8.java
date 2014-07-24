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

import com.sun.spot.util.Utils;
import java.io.IOException;


public class Battery8 implements IBattery {


	private static final byte UPDATE_BATTERY_DATA = 25;
	private static final byte QUERY_BATTERY_UPDATE_BUSY = 30;
	private static final byte QUERY_STATUS = 32;
	private static final byte QUERY_AVAILABLE_CAPACITY = 33;
	private static final byte SET_AVAILABLE_CAPACITY = 34;
	private static final byte QUERY_MEASURED_CAPACITY = 35;
	private static final byte SET_MEASURED_CAPACITY = 36;
	private static final byte QUERY_CHARGETIME = 37;
	private static final byte SET_CHARGETIME = 38;
	private static final byte QUERY_RUNTIME = 39;
	private static final byte SET_RUNTIME = 40;
	private static final byte QUERY_SLEEPTIME = 41;
	private static final byte SET_SLEEPTIME = 42;
	private static final byte QUERY_SLEEP_DISCHARGE = 43;
	private static final byte SET_SLEEP_DISCHARGE = 44;
	private static final byte QUERY_CHARGE_COUNT = 45;
	private static final byte SET_CHARGE_COUNT = 46;
	private static final byte QUERY_STATE_OF_CHARGE = 47;
	private static final byte QUERY_INTERNAL_RESISTANCE = 48;
	private static final byte QUERY_VBATT_UNLOADED = 49;

	private static final byte SIZEOF_BYTE = 1;
	private static final byte SIZEOF_SHORT = 2;
	private static final byte SIZEOF_INT = 4;
	private static final byte SIZEOF_LONG = 8;

	private static final String BATTERY_MODEL_PROPERTY = "spot.battery.model";
	private static final String BATTERY_RATING_PROPERTY = "spot.battery.rating";
	private static final String BATT770 = "LP523436D";
	private static final int BATT770_RATING = 770;
	
	private static final double MAHR_CONVERSION = 1509949.0;
	private ISpiMaster spiMaster;
	private SpiPcs chipSelectPin;
	private byte[] snd = new byte[9];
	private byte[] rcv = new byte[9];
	private String model;
	private int rating;

	private int major;
	private int minor;
	
	

	/**
	 * Battery constructor for obtaining information about the SPOT rechargeable battery
	 * this can throw a RuntimeException if it is created with a pre-rev6 eSPOT or version earlier than PCTRL-1.99
	 * @param chipSelectPin chip select pin for power controller (PeripheralChipSelect.SPI_PCS_POWER_CONTROLLER)
	 * @param spiMaster SPI object (usually from Spot.getInstance().getSPI())
	 * @throws RuntimeException if pre-rev6 eSPOT, pctrl-1.98 or earlier firmware or invalid spot.battery.model property
	 */
	
    public Battery8(SpiPcs chipSelectPin, ISpiMaster spiMaster) throws RuntimeException {
    	this.chipSelectPin = chipSelectPin;
   		this.spiMaster = spiMaster;
   		String revision = Spot.getInstance().getPowerController().getRevision();
      	int separator = revision.indexOf('.'); // find the decimal point
      	int i;
      	for (i = separator+1; i < revision.length() && revision.charAt(i) >= '0' && revision.charAt(i) <= '9'; i++);
		minor = Integer.parseInt(revision.substring(separator + 1,i));
       	major = Integer.parseInt(revision.substring(separator - 1, separator));
 	   	if (major != 2) throw new RuntimeException("Battery Class must use PCTRL-2.00 or greater");
 	   	if (Spot.getInstance().getHardwareType() < 8) throw new RuntimeException("Battery Class must use eSPOT hardware rev 8.x");
		model = System.getProperty(BATTERY_MODEL_PROPERTY);
		if (model == null) {
			Spot.getInstance().setPersistentProperty(BATTERY_MODEL_PROPERTY, BATT770);
			model = BATT770;
		} 
		String ratingStr = System.getProperty(BATTERY_RATING_PROPERTY);
		if (ratingStr == null) {
			Spot.getInstance().setPersistentProperty(BATTERY_RATING_PROPERTY, Integer.toString(BATT770_RATING));
			rating = BATT770_RATING;
		} else {
            rating = Integer.parseInt(ratingStr);
        }
	}
    
 
    // send SPI query command to power controller
    // retrieve count bytes as an int (1 to 4)
	private int getSPI(byte command, int count) {
		snd[0] = command;
		spiMaster.sendAndReceive(chipSelectPin, count+1, snd, 1, count, rcv);
		int retval = 0;
		for (byte i = 0; i < count; i++) {
			retval = (retval << 8) | (rcv[i] & 0xFF);
		}
		return retval;
	}

 
   // send SPI set command to power controller
   // count bytes in value are sent
	private void setSPI(byte command, int count, int value) {
		snd[0] = command;
		for (int i = count; i > 0; i--) {
			snd[i] = (byte) (value & 0xFF);
			value = value >> 8;
		}
		spiMaster.sendAndReceive(chipSelectPin, count+1, snd, 1, count, rcv);
	}

	// answer the battery model number
	public String getModelNumber() {
		return model;
	}
	// answer the batteries rated capacity
	public double getRatedCapacity() {
		return (double) rating;
	}
			
//	public String rawBatteryData() {
//		lock();	
//		String model_number = getModelNumber();
//		double rated_capacity = getRatedCapacity();
//		int available_capacity = (int) get(AVAILABLE_CAPACITY_OFFSET,SIZEOF_INT);
//		int capacity = (int) get(CAPACITY_OFFSET,SIZEOF_INT);
//		int sleeptime = (int) get(SLEEPTIME_OFFSET,SIZEOF_INT);
//		int runtime = (int) get(RUNTIME_OFFSET,SIZEOF_INT);
//		int chargetime = (int) get(CHARGETIME_OFFSET,SIZEOF_INT);
//		short chargecount = (short) get(CHARGECOUNT_OFFSET,SIZEOF_SHORT);
//		byte state = (byte) get(STATE_OFFSET,SIZEOF_BYTE);
//		byte flags = (byte) get(FLAGS_OFFSET,SIZEOF_BYTE);
//		short sleep_current = (short) get(SLEEP_DISCHARGE_OFFSET,SIZEOF_SHORT);
//		unlock();
//		StringBuffer s = new StringBuffer(50);
//		s.append("Battery PN: "+model_number+"\n");
//		s.append("Rated Capacity: "+rated_capacity+"\n");
//		s.append("Available Capacity: "+available_capacity+"\n");
//		s.append("Maximum Capacity: "+capacity+"\n");
//		s.append("Sleep Time: "+sleeptime+"\n");
//		s.append("Run Time: "+runtime+"\n");
//		s.append("Charge Time: "+chargetime+"\n");
//		s.append("Charge Count: "+chargecount+"\n");
//		s.append("Battery State: "+state+"\n");
//		s.append("Battery Flags: "+flags+"\n");
//		s.append("Sleep Current: "+sleep_current+"\n");
//		return s.toString();
//	}
	
	// return the available battery capacity in maHr
	public double getAvailableCapacity() {
		return (double) getSPI(QUERY_AVAILABLE_CAPACITY, SIZEOF_INT)/MAHR_CONVERSION;
	}
	
	// get the calculated maximum capacity of the battery in maHr
	public double getMaximumCapacity() {
		return (double) getSPI(QUERY_MEASURED_CAPACITY, SIZEOF_INT)/MAHR_CONVERSION;
	}
	
	// return the percentage charge of the battery level
	public int getBatteryLevel() {
		int available = getSPI(QUERY_AVAILABLE_CAPACITY, SIZEOF_INT);
		int measured = getSPI(QUERY_MEASURED_CAPACITY, SIZEOF_INT);
        int level = (int)((available * 100.0) / measured);
		return level > 100 ? 100 : level;
	}
	
	// return the times the spot has been fully charged
	public short getChargeCount() {
		return (short) getSPI(QUERY_CHARGE_COUNT, SIZEOF_SHORT);
	}
	
	// return the sleep current in microamps
	public int getSleepCurrent() {
		return (int) ((double) getSPI(QUERY_CHARGE_COUNT, SIZEOF_BYTE)/27.487791);
	}


	public void setSleepCurrent(int microamps) {
		int adc = (int) ((double) microamps * 27.487791);
		setSPI(SET_SLEEP_DISCHARGE, 2, adc);
	}
			
	public byte getFlags() {
		return 0; // deprecated
	}

	public boolean hasTemperatureSensor() {
		return true;
	}		

	public boolean hasBattery() {
		byte soc = (byte) getSPI(QUERY_STATE_OF_CHARGE, SIZEOF_BYTE);
		return soc != NO_BATTERY;
	}		

	public boolean calibrationCycleDetected() {
		short status = (short) getSPI(QUERY_STATUS, SIZEOF_BYTE);
		return (status & IPowerController.BATTERY_CALIBRATED) != 0;
	}		
	
	public byte getState() {
		return (byte) getSPI(QUERY_STATE_OF_CHARGE, SIZEOF_BYTE);
	}

	public String getStateAsString(int state) {
		switch (state) {
			case IBattery.NO_BATTERY:    return "no battery";    
			case IBattery.DEAD_BATTERY:  return "dead battery";  
			case IBattery.LOW_BATTERY:   return "low battery";   
			case IBattery.DISCHARGING:   return "discharging";   
			case IBattery.CHARGING:      return "charging";      
			case IBattery.EXT_POWERED: return "externally powered"; 
			case IBattery.OUT_OF_RANGE_TEMP: return "battery temperature out of range"; 
			default: return "";
		}
	}
		
	public void updatePersistantInfo() throws IOException { 
        long timeout = System.currentTimeMillis() + ((long) 1000);
        snd[0] = UPDATE_BATTERY_DATA; // write battery info
		spiMaster.sendAndReceive(chipSelectPin, 1, snd, 0, 0, null);

        snd[0] = QUERY_BATTERY_UPDATE_BUSY; 
        snd[1] = (byte) 0;
        do {
			Utils.sleep(50); // should take about 100msec
        	rcv[0] = (byte) 0;
			spiMaster.sendAndReceive(chipSelectPin, 2, snd, 1, 1, rcv);
         	if (System.currentTimeMillis() > timeout) throw new IOException("Power Controller not responding when sent persistant write");
		} while (rcv[0] != 0);
	}

	/**
	 * return the time the SPOT has been asleep since last charge
	 * @return sleep time in milliseconds
	 */
	
	// return the time unit was asleep in milliseconds
	public int getSleepTime() {
		return (getSPI(QUERY_SLEEPTIME, SIZEOF_INT) * 256)/1000;
	}

	/**
	 * return the time the SPOT has been running since last charge
	 * @return run time in milliseconds
	 */
	
	// return the time unit was asleep in milliseconds
	public int getRunTime() {
		return (getSPI(QUERY_RUNTIME, SIZEOF_INT) * 200)/1000;
	}
	
	/**
	 * return the time the SPOT has been charging 
	 * @return charge time in milliseconds
	 */
	// return the time unit was charging in seconds
	public int getChargeTime() {
		return (getSPI(QUERY_CHARGETIME, SIZEOF_INT) * 200)/1000;
	}

	// return the seconds the spot has been sleeping since last charge
	public long[] getTime() {
		long[] time = new long[3];
		time[IBattery.SLEEPTIME] = getSleepTime();
		time[IBattery.RUNTIME] =  getRunTime();
		time[IBattery.CHARGETIME] = getChargeTime();
		return time;
	}

	/**
	 * reset charge, run and sleep accumulation timers to zero 
	 */
	public void resetBatteryTimers() {
		setSPI(SET_SLEEPTIME, SIZEOF_INT, 0);
		setSPI(SET_RUNTIME, SIZEOF_INT, 0);
		setSPI(SET_CHARGETIME, SIZEOF_INT, 0);
	}

    public String rawBatteryData() {
        return "to be finished...";
    }

} // end class
