/*
 * Copyright 2009-2010 Sun Microsystems, Inc. All Rights Reserved.
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

import com.sun.spot.peripheral.IFlashMemoryDevice;
import com.sun.spot.peripheral.ISpiMaster;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.resources.Resource;

/**
 * Driver for the serial flash memory chip as fitted to Sun SPOT external boards
 * 
 */
public class SerialFlash extends Resource implements IFlashMemoryDevice {
	
	public static final int SPI_CONFIG  = (ISpiMaster.CSR_NCPHA | ISpiMaster.CSR_BITS_8 | ISpiMaster.CSR_SCBR_250K | ISpiMaster.CSR_DLYBCT_200);
	public static final int SPI_CONFIG8 = (ISpiMaster.CSR_NCPHA | ISpiMaster.CSR_BITS_8 | ISpiMaster.CSR_SCBR_MIN  | ISpiMaster.CSR_DLYBCT_MAX);
	
	private static final byte SERIAL_FLASH_COMMAND_RDID         = (byte)0xAB;
	private static final byte SERIAL_FLASH_COMMAND_DPD          = (byte)0xB9;
	private static final byte SERIAL_FLASH_COMMAND_READ         = 3;
	private static final byte SERIAL_FLASH_COMMAND_RDSR         = 5;
	private static final byte SERIAL_FLASH_COMMAND_WRSR         = 1;
	private static final byte SERIAL_FLASH_COMMAND_WREN         = 6;
	private static final byte SERIAL_FLASH_COMMAND_WRDI         = 4;
    private static final byte SERIAL_FLASH_COMMAND_WRITE        = 2;
	private static final byte SERIAL_FLASH_COMMAND_SECTOR_ERASE = (byte)0xD8;
	private static final byte SERIAL_FLASH_COMMAND_CHIP_ERASE   = (byte)0xC7;

	private static final int[] SECTOR_ADDRESSES_25AA512 = new int[] {0x000000, 0x004000, 0x008000, 0x00C000};
	private static final int   SECTOR_SIZE_25AA512    = 0x4000;
	private static final int   SIZE_25AA512           = 0x010000;
	private static final int   PAGE_SIZE_25AA512      = 128;
	        static final byte  RDID_SIGNATURE_25AA512 = (byte)0x29; // also for testing

	private static final int[] SECTOR_ADDRESSES_M25P05 = new int[] {0x000000, 0x008000};
	private static final int   SECTOR_SIZE_M25P05    = 0x8000;
	private static final int   SIZE_M25P05           = 0x010000;
	private static final int   PAGE_SIZE_M25P05      = 256;
	        static final byte  RDID_SIGNATURE_M25P05 = (byte)0x05;  // also for testing

    private static final byte WRITE_PROTECTION_BITS = 0x0C;
    private static final byte WRITE_IN_PROCESS_FLAG = 0x01;

    private int[] serialFlashSectorAddresses;
	private int   serialFlashSectorSize;
	private int   serialFlashSize;
    private int   pageSize;

	private byte[] rxBuffer = new byte[4];
	private byte[] commandBuffer = new byte[4];

    private boolean use24bitAddresses = true;
    private int wakeupDelay;

	private ISPI spi;

	public SerialFlash(ISPI spi) {
		this.spi = spi;
        setFlashType(readChipSignature());
        wakeupDelay = Spot.getInstance().getHardwareType() < 8 ? 10 : 25;
	}

    SerialFlash(ISPI spi, byte sig) {       // for unit tests
		this.spi = spi;
        setFlashType(sig);
        wakeupDelay = 10;
	}

    private byte readChipSignature() {
        commandBuffer[0] = SERIAL_FLASH_COMMAND_RDID;
        commandBuffer[1] = 0;
        commandBuffer[2] = 0;
        commandBuffer[3] = 0;
		spi.sendSPICommand(commandBuffer, 4, rxBuffer, 1, 4);
        // System.out.println("Serial Flash signature = " + Integer.toHexString(rxBuffer[0] & 0x0FF));
        return rxBuffer[0];
    }

    private void setFlashType(byte val) {
        if (val == RDID_SIGNATURE_M25P05) {
            pageSize                   = PAGE_SIZE_M25P05;
            serialFlashSectorAddresses = SECTOR_ADDRESSES_M25P05;
            serialFlashSectorSize      = SECTOR_SIZE_M25P05;
            serialFlashSize            = SIZE_M25P05;
            use24bitAddresses          = true;
        } else if (val == RDID_SIGNATURE_25AA512) {
            pageSize                   = PAGE_SIZE_25AA512;
            serialFlashSectorAddresses = SECTOR_ADDRESSES_25AA512;
            serialFlashSectorSize      = SECTOR_SIZE_25AA512;
            serialFlashSize            = SIZE_25AA512;
            use24bitAddresses          = false;
        } else {
            // System.out.println("Unknown type of serial flash: " + Integer.toHexString(val & 0x0FF));
            throw new SpotFatalException("Unknown type of serial flash: " + Integer.toHexString(val & 0x0FF));
        }
        commandBuffer = new byte[pageSize + 4]; // 4 bytes to hold command and address
    }

    private void wakeUp() {
		commandBuffer[0] = SERIAL_FLASH_COMMAND_RDID;
		spi.sendSPICommand(commandBuffer, 1, null, 0);
        for (int i = 0; i < wakeupDelay; i++) { // need to wait about 100 usec for chip to wake up
            rxBuffer[0] += i;
        }
    }

    private void powerDown() {
		commandBuffer[0] = SERIAL_FLASH_COMMAND_DPD;
		spi.sendSPICommand(commandBuffer, 1, null, 0);
    }

	/**
	 * Read data from the SerialFlash flash memory.
	 * 
	 * @param address address in memory to start reading, in range 0 to 0xFFFF
	 * @param numOfBytes number of bytes to read, in range 0 to (0x10000-address)
	 * @param buffer the hold the data
	 * @param offset offset into buffer for first byte read
	 */
	public synchronized void read(int address, int numOfBytes, byte[] buffer, int offset) {
		byte[] rxbuffer = new byte[numOfBytes];
		if (!validAddress(address)) throw new IllegalArgumentException("Illegal address: 0x" + Integer.toHexString(address));
		if (!validAddress(address+numOfBytes-1)) throw new IllegalArgumentException("Attempt to read beyond end of memory: 0x" + Integer.toHexString(address+numOfBytes-1));
        wakeUp();
        commandBuffer[0] = SERIAL_FLASH_COMMAND_READ;
        int i = 1;
        if (use24bitAddresses) {
            commandBuffer[i++] = (byte) ((address >> 16) & 0xFF);
        }
		commandBuffer[i++] = (byte)((address >> 8) & 0xFF);
		commandBuffer[i++] = (byte)(address & 0xFF);
		spi.sendSPICommand(commandBuffer, i, rxbuffer, numOfBytes, i);
		System.arraycopy(rxbuffer, 0, buffer, offset, numOfBytes);
        powerDown();
	}

	public void write(int address, int numOfBytes, byte[] buffer, int offset) {
		if (!validAddress(address)) throw new IllegalArgumentException("Illegal address: 0x" + Integer.toHexString(address));
		if ((address % pageSize) != 0) throw new IllegalArgumentException("Attempt to write to unaligned address: 0x" + Integer.toHexString(address));
		if (numOfBytes > pageSize) throw new IllegalArgumentException("Attempt to write more than one page: " + numOfBytes);

        wakeUp();
		enableWriting();
		commandBuffer[0] = SERIAL_FLASH_COMMAND_WRITE;
        int i = 1;
        if (use24bitAddresses) {
            commandBuffer[i++] = (byte) ((address >> 16) & 0xFF);
        }
		commandBuffer[i++] = (byte)((address >> 8) & 0xFF);
		commandBuffer[i++] = (byte)(address & 0xFF);
		for (int j = 0; j < numOfBytes; j++) {
			commandBuffer[i+j] = buffer[offset+j];
		}
		spi.sendSPICommand(commandBuffer, numOfBytes+i, rxBuffer, 0);
		waitForWriteToComplete();
        powerDown();
	}

	/**
	 * Verify data in the SerialFlash flash memory.
	 * 
	 * @param address address in memory to start verifying, in range 0 to 0xFF00 but must be page-aligned
	 * @param numOfBytes number of bytes to write, in range 0 to pageSize
	 * @param buffer the data to verify against
	 * @return true if data matches
	 */
	public synchronized boolean verify(int address, int numOfBytes, byte[] buffer) {
		if (!validAddress(address)) throw new IllegalArgumentException("Illegal address: 0x" + Integer.toHexString(address));
		if ((address % pageSize) != 0) throw new IllegalArgumentException("Attempt to verify unaligned address: 0x" + Integer.toHexString(address));
		if (numOfBytes > pageSize) throw new IllegalArgumentException("Attempt to verify more than one page: " + numOfBytes);

        byte rbuf[] = new byte[numOfBytes];
		read(address, numOfBytes, rbuf, 0);
		for (int i = 0; i < numOfBytes; i++) {
			if (rbuf[i] != buffer[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Erase a sector
	 * 
	 * @param address an address within sector to erase
	 */
	public synchronized void eraseSectorAtAddress(int address) {
		if (!validAddress(address)) throw new IllegalArgumentException("Illegal address: 0x" + Integer.toHexString(address));
        wakeUp();
		enableWriting();
		commandBuffer[0] = SERIAL_FLASH_COMMAND_SECTOR_ERASE;
        int i = 1;
        if (use24bitAddresses) {
            commandBuffer[i++] = (byte) ((address >> 16) & 0xFF);
        }
		commandBuffer[i++] = (byte)((address >> 8) & 0xFF);
		commandBuffer[i++] = (byte)(address & 0xFF);
		spi.sendSPICommand(commandBuffer, i, rxBuffer, 0);
		waitForWriteToComplete();
        powerDown();
	}

	/**
	 * Check whether a sector is erased.
	 * 
	 * @param address an address within sector to check
	 * @return true if sector is erased
	 */
	public synchronized boolean sectorErased(int address) {
		if (!validAddress(address)) throw new IllegalArgumentException("Illegal address: 0x" + Integer.toHexString(address));
		// determine which sector this is
		int sectorNum = getSectorContainingAddress(address);
		int checkAddress = serialFlashSectorAddresses[sectorNum];
		int topAddress = checkAddress + serialFlashSectorSize;
        byte rbuf[] = new byte[pageSize];
		while (checkAddress < topAddress) {
			// read a page
			read(checkAddress, pageSize, rbuf, 0);
			// loop through page checking erased
			for (int j = 0; j < pageSize; j++) {
				if (rbuf[j] != (byte)0xFF) {
					return false;
				}
			}
			checkAddress += pageSize;
		}
		return true;
	}

	/**
	 * Erase all data in the chip
	 */
	public synchronized void eraseChip() {
        wakeUp();
		enableWriting();
		commandBuffer[0] = SERIAL_FLASH_COMMAND_CHIP_ERASE;
		spi.sendSPICommand(commandBuffer, 1, rxBuffer, 0);
		waitForWriteToComplete();
        powerDown();
	}

	/**
	 * Get the page size for writing. Each call to write can write no more than one page.
	 * 
	 * @return The page size in bytes
	 */
	public int getPageSize() {
		return pageSize;
	}
	
	/**
	 * Set or clear the write protection
	 * 
	 * @param b If b is true the device becomes write protected; if b is false the device becomes writable.
	 */
	public void setWriteProtection(boolean b) {
		setStatusReg(b ? WRITE_PROTECTION_BITS : 0);
	}

	/**
	 * Check whether the device is write protected
	 * 
	 * @return true if it is write protected
	 */
	public boolean isWriteProtected() {
		return (getStatusReg() & WRITE_PROTECTION_BITS) != 0;
	}

	/**
	 * Get the capacity of the device
	 * @return The size of the memory in bytes
	 */
	public int getSize() {
		return serialFlashSize;
	}

	/**
	 * Get the size of a device sector
	 * 
	 * @param sectorNum The sector whose size is to be returned
	 * @return The size of a sector in bytes
	 */
	public int getSectorSize(int sectorNum) {
		return serialFlashSectorSize;
	}

	/* (non-Javadoc)
	 * @see com.sun.squawk.peripheral.spot.IFlashMemoryDevice#getNumberOfSectors()
	 */
	public int getNumberOfSectors() {
		return serialFlashSectorAddresses.length;
	}

	public int getLastSectorAvailableToJava() {
		return serialFlashSectorAddresses.length - 1;
	}
	
	public int getSectorAddress(int sectorNum) {
		return serialFlashSectorAddresses[sectorNum];
	}

	synchronized int getStatusReg() {
        wakeUp();
		commandBuffer[0] = SERIAL_FLASH_COMMAND_RDSR;
		spi.sendSPICommand(commandBuffer, 1, rxBuffer, 1, 1);
        powerDown();
		return rxBuffer[0] & 0xFF;
	}

	synchronized void setStatusReg(byte regValue) {
        wakeUp();
		enableWriting();
		commandBuffer[0] = SERIAL_FLASH_COMMAND_WRSR;
		commandBuffer[1] = regValue;
		spi.sendSPICommand(commandBuffer, 2, rxBuffer, 0);
		waitForWriteToComplete();
        powerDown();
	}


	
	public int getSectorContainingAddress(int addr) {
		int sectorNum;
		for (sectorNum = serialFlashSectorAddresses.length-1; sectorNum >= 0 ; sectorNum--) {
			if (serialFlashSectorAddresses[sectorNum] <= addr) {
				break;
			}
		}
		return sectorNum;
	}

	private boolean validAddress(int address) {
		return address >=0 && address < serialFlashSize;
	}

	private int waitForWriteToComplete() {
		int regValue;
		do {
            commandBuffer[0] = SERIAL_FLASH_COMMAND_RDSR;
            spi.sendSPICommand(commandBuffer, 1, rxBuffer, 1, 1);
            regValue = rxBuffer[0] & 0xFF;
		} while ((regValue & WRITE_IN_PROCESS_FLAG) != 0);
		return regValue; 
	}

	private void enableWriting() {
		commandBuffer[0] = SERIAL_FLASH_COMMAND_WREN;
		spi.sendSPICommand(commandBuffer, 1, rxBuffer, 0);
	}

	public int getNumberOfSectorsInRegion(int startAddress, int length) {
		return 1+getSectorContainingAddress(startAddress+length-1) - getSectorContainingAddress(startAddress);
	}
}
