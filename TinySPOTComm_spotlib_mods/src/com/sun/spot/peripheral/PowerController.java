/*
 * Copyright 2006-2009 Sun Microsystems, Inc. All Rights Reserved.
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

import com.sun.spot.resources.Resource;
import com.sun.spot.resources.transducers.ITemperatureInput;
import com.sun.spot.resources.transducers.IMeasurementInfo;
import com.sun.spot.resources.transducers.SensorEvent;
import com.sun.spot.resources.transducers.TemperatureInputEvent;
import com.sun.squawk.*;
import com.sun.squawk.vm.*;
import java.io.IOException;

class PowerController extends Resource implements IPowerController {
	
	static final int SPI_CONFIG  = (ISpiMaster.CSR_NCPHA | ISpiMaster.CSR_BITS_8 | ISpiMaster.CSR_SCBR_250K | ISpiMaster.CSR_DLYBCT_200);

	protected ISpiMaster spiMaster;
	protected SpiPcs chipSelectPin;
	protected IBattery battery;

    protected PowerController() {
        // for subclasses
    }
    
	public PowerController(ISpiMaster spiMaster, PeripheralChipSelect pcs) {
		this.spiMaster = spiMaster;
		this.chipSelectPin = new SpiPcs(pcs, SPI_CONFIG);
	}
	
	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IPowerController#getRevision()
	 */
	public String getRevision() {
		byte[] rxBuf = new byte[1];
		byte[] txBuf = new byte[] {GET_STRING_LEN_CMD};
		spiMaster.sendAndReceive(chipSelectPin, 1, txBuf, 1, 1, rxBuf);
	
		int stringSize = rxBuf[0] & 0xFF;

        txBuf[0] = GET_STRING_CMD;
		rxBuf = new byte[stringSize];
		spiMaster.sendAndReceive(chipSelectPin, 1, txBuf, 1, stringSize, rxBuf);
		
		StringBuffer s = new StringBuffer(stringSize);
		for (int i = 0; i < rxBuf.length; i++) {
            if (rxBuf[i] != 0) {
                s.append((char) rxBuf[i]);
            }
		}
		return s.toString();
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IPowerController#getTime()
	 */
	public long getTime() {
        long highResult = VM.execSyncIO(ChannelConstants.AVR_GET_TIME_HIGH, 0);
        long lowResult  = VM.execSyncIO(ChannelConstants.AVR_GET_TIME_LOW, 0);
        return (highResult << 32) | (lowResult & 0x00000000FFFFFFFFL);
        
        // this seems to cause a lot of test failures:
        // return get64bits(spiMaster, chipSelectPin, PowerController.GET_TIME_CMD);
	}

	public void setTime(long systemTimeMillis) {
        int timeHigh = (int) ((systemTimeMillis>>32) & 0xFFFFFFFF);
		int timeLow = (int) (systemTimeMillis & 0xFFFFFFFF);
		long delta = systemTimeMillis - System.currentTimeMillis();
		byte[] txBuf = new byte[9];
		txBuf[0] = IPowerController.SET_TIME_CMD;
		for (int i = 8; i > 0; i--) {
			txBuf[i] = (byte)systemTimeMillis;
			systemTimeMillis = systemTimeMillis >> 8;
		}
		spiMaster.sendAndReceive(chipSelectPin, 9, txBuf, 0, 0, null);
		
		VM.execSyncIO(ChannelConstants.SET_SYSTEM_TIME, timeHigh, timeLow, 0, 0, 0, 0, null, null);
		Spot.getInstance().getSleepManager().adjustStartTime(delta);
	}	

	public int getEvents() {
        return VM.execSyncIO(ChannelConstants.AVR_GET_STATUS, 0);
	}

	public int getPowerFault() {
		return makeSingleByteQuery(IPowerController.GET_POWER_FAULT_CMD);
	}
	
	public int getVcore() {
        return makeADCQuery(QUERY_V_CORE, 3000);
	}

	public int getVcc() {
        return makeADCQuery(QUERY_V_CC, 3904);
	}

	public int getVbatt() {
        return makeADCQuery(QUERY_V_BATT, 5110);
	}
	
	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IPowerController#getVext()
	 */
	public int getVext() {
        return makeADCQuery(QUERY_V_EXT, 6000);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IPowerController#getVusb()
	 */
	public int getVusb() {
        return makeADCQuery(QUERY_V_USB, 6000);
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IPowerController#getIcharge()
	 */
	public int getIcharge() {
        return makeADCQuery(QUERY_I_CHARGE, 512);
	}

	public int getIMax() {
		return makeADCQuery(QUERY_I_MAX, 512);
	}
	
	public int getStartupTime() {
		return makeADCQuery(QUERY_STARTUP, 1024);
	}

	public int getIdischarge() {
        return makeADCQuery(QUERY_I_DISCHARGE, 512);
	}
	
	public double getTemperature() {
		if (Spot.getInstance().getHardwareType() < 6) {
			throw new IllegalStateException("Cannot measure power controller temperature for SPOTs with hardware before rev6");
		}
		return (makeADCQuery(QUERY_TEMPERATURE, 3000) - 495)/10.0;
	}

	public void setIndicate(byte mask) {
        byte[] txBuf = new byte[2];
		txBuf[0] = IPowerController.SET_INDICATE_CMD;
		txBuf[1] = mask;
		spiMaster.sendAndReceive(chipSelectPin, 2, txBuf, 0, 0, null);
	}

	protected int makeADCQuery(byte avrQuery, int conversionMultiplier) {
		int adc_value = makeDoubleByteQuery(avrQuery);
		return (adc_value * conversionMultiplier) / 1024;
	}

	protected int makeSingleByteQuery(byte command) {
		byte[] rxBuf = new byte[1];
		byte[] txBuf = new byte[] {command};
		spiMaster.sendAndReceive(chipSelectPin, 1, txBuf, 1, 1, rxBuf);
		return rxBuf[0] & 0xFF;
	}
	
	protected int makeDoubleByteQuery(byte command) {
		byte[] rxBuf = new byte[2];
		byte[] txBuf = new byte[] {command};
		spiMaster.sendAndReceive(chipSelectPin, 1, txBuf, 1, 2, rxBuf);
		return (rxBuf[1] & 0xff) << 8 | (rxBuf[0] & 0xff);
	}

	public void disableSynchronisation() {
		VM.execSyncIO(ChannelConstants.ENABLE_AVR_CLOCK_SYNCHRONISATION, 0);
	}

	public void enableSynchronisation() {
		VM.execSyncIO(ChannelConstants.ENABLE_AVR_CLOCK_SYNCHRONISATION, 1);
	}

	public void setControl(byte mask) {
        byte[] txBuf = new byte[2];
		txBuf[0] = IPowerController.SET_CONTROL_CMD;
		txBuf[1] = mask;
		spiMaster.sendAndReceive(chipSelectPin, 2, txBuf, 0, 0, null);
	}

	public int getControl() {
		return makeSingleByteQuery(IPowerController.GET_CONTROL_CMD);
	}

    public synchronized IBattery getBattery() {
		if (battery == null) {
			battery = new Battery(chipSelectPin, spiMaster);
		}
		return battery;
	}

    // Rev 8 routines

    public void setWatchdog(long time) {
		throw new IllegalStateException("Watchdog not implemented prior Rev 8");
	}
	
	public void restartWatchdog() {
		return;
 	}

    public int getStatus() {
        return 0;
    }

    public int getButtonEvent() {
        return 0;
    }

    public void setShutdownTimeout(int time) { }

}
