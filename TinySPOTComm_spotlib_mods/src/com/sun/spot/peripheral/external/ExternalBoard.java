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

package com.sun.spot.peripheral.external;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import com.sun.spot.flashmanagement.FlashFileInputStream;
import com.sun.spot.flashmanagement.FlashFileOutputStream;
import com.sun.spot.peripheral.*;
import com.sun.spot.resources.Resource;
import com.sun.spot.util.Properties;
import com.sun.squawk.peripheral.INorFlashSector;

/**
 * Represents an external board. Used primarily to manage the properties stored in the serial flash memory
 * of any SPOT external board. Acts as the base class for specific board implemenations.
 */
public class ExternalBoard extends Resource implements IExternalBoard {

    public static final String ID_PROPERTY_NAME = "PART_ID";

    protected static final int BOARD_MAGIC_WORD = 0xFEEDFACE;

    private SerialFlash serialFlash = null;
    private PeripheralChipSelect cs_pin;
    private Properties properties = null;
    private String partId = null;
    private String partId2 = null;

    /**
     * Create an interface to the external board
     * @param pcs The chip select pin of the board
     */
    public ExternalBoard(PeripheralChipSelect pcs) {
        cs_pin = pcs;
    }

    /**
     * Create an interface to an external board
     * @param partId The part id to match against
     */
    protected ExternalBoard(String partId) {
        this.partId = partId;
    }

    /**
     * Create an interface to an external board
     * @param partId The part id to match against
     * @param partId2 An alternative part id to match against
     */
    protected ExternalBoard(String partId, String partId2) {
        this.partId = partId;
        this.partId2 = partId2;
    }

    /**
     * Get the properties of this board.
     *
     * @return The properties which will be empty if unable to read them
     */
    public Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
            try {
                // open an input stream on the bottom sector of the memory device
                InputStream is = new FlashFileInputStream(new NorFlashSector(getSerialFlash(), 0, INorFlashSector.SYSTEM_PURPOSED));
                DataInputStream dis = new DataInputStream(is);
                if (dis.readLong() == BOARD_MAGIC_WORD) {
                    BoundedInputStream bis = new BoundedInputStream(is);
                    properties.load(bis);
                }
            } catch (IOException e) {
                System.err.println("Error reading external board flash memory: " + e.getMessage());
            }
        }
        return properties;
    }

    /**
     * Set the properties of this board
     *
     * @param p The properties to set.
     * @throws IOException
     */
    public void setProperties(Properties p) throws IOException {
        OutputStream os = new FlashFileOutputStream(new NorFlashSector(getSerialFlash(), 0, INorFlashSector.SYSTEM_PURPOSED));
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeLong(BOARD_MAGIC_WORD);
        // need to determine the size of the properties
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        p.store(b, "Board Properties");
        dos.writeInt(b.size());
        p.store(os, "Board Properties");
    }

    /**
     * Get the serial flash memory device
     *
     * @return The memory as an {@link IFlashMemoryDevice}
     */
    public synchronized IFlashMemoryDevice getSerialFlash() {
        if (serialFlash == null) {
            int hwrev = Spot.getInstance().getHardwareType();
            serialFlash = new SerialFlash(newBoardDeviceSPI(7, (hwrev < 8) ? SerialFlash.SPI_CONFIG : SerialFlash.SPI_CONFIG8));
        }
        return serialFlash;
    }

    /**
     * Get a BoardDeviceSPI for accessing a device on the board. See {@link ISpiMaster} for
     * details of SPI configuration settings.
     * @param deviceAddress the index of the device
     * @param spiConfiguration value to be written into SPI_CSR for transfers to this device
     * @return the BoardDeviceSPI for access to the device
     */
    protected ISPI newBoardDeviceSPI(int deviceAddress, int spiConfiguration) {
        return new BoardDeviceSPI(deviceAddress, getChipSelect(), spiConfiguration);
    }

    /**
     * Handle the case where no board id matches
     */
    protected void hardwareNotValid() {
        throw new SpotFatalException("Missing or incorrect external board");
    }

    /**
     * Force the chip select pin to be as specified. This is an unusual occurance, since the chip select pin
     * is usually determined by matching against the external board map
     *
     * @param pin SpiPcs for the board
     */
    protected void forceChipSelectPin(PeripheralChipSelect pin) {
        cs_pin = pin;
    }

    /**
     * Look up the chip select pin for the board whose ID_PROPERTY_NAME property has the value previously supplied
     * @return The chip select pin as a mask with one bit set
     */
    private PeripheralChipSelect getChipSelect() {
        if (cs_pin == null) {
            // need to look up the cs pin for this board
            if (partId == null) {
                throw new SpotFatalException("Request for chip select of external board with both cs_pin and partId unset");
            }
            Hashtable map = Spot.getInstance().getExternalBoardMap();
            Enumeration e = map.keys();
            while (e.hasMoreElements()) {
                PeripheralChipSelect key = (PeripheralChipSelect) e.nextElement();
                Properties p = (Properties) map.get(key);
                String id = (String) p.get(ID_PROPERTY_NAME);
                if (partId.equals(id) || (partId2 != null && partId2.equals(id))) {
                    cs_pin = key;
                    properties = p;
                    break;
                }
            }
            if (cs_pin == null) {
                // didn't find a match -- the board is not present or is not initialized
                hardwareNotValid();
            }
        }
        return cs_pin;
    }

    /**
     * @return true if this board is physically present
     */
    public boolean isInstalled() {
        try {
            getSerialFlash(); // to make sure field is initialized
            return true; // (serialFlash.getStatusReg() & 0x71) == 0;
        } catch (SpotFatalException ex) {
            // System.out.println("External board not installed");
            return false;
        }
    }

    public int getBoardIndex() {
        return getChipSelect() == PeripheralChipSelect.SPI_PCS_BD_SEL1() ? 0 : 1;
    }
}
