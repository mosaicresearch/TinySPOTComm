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

package com.sun.spot.peripheral;

import com.sun.spot.resources.Resource;


/**
 * This class provides symbolic access to the RM9200 peripheral ids.
 * The ids are held in the form of a bit mask, with one bit set in the position
 * corresponding to the id of the peripheral (eg if the id is 4, bit 4 is set).
 * These ids are used to specify the required device when enabling interrupts or
 * enabling peripheral clocks.
 * 
 * This class also defines which peripherals are accessible from Java.
 */
public class AT91_Peripherals extends Resource {
	
	public int FIQ_ID_MASK;
	public int SYSIRQ_ID_MASK;
	public int PIOA_ID_MASK;
	public int PIOB_ID_MASK;
	public int PIOC_ID_MASK;
	public int PIOD_ID_MASK;
	public int US0_ID_MASK;
	public int US1_ID_MASK;
	public int US2_ID_MASK;
	public int US3_ID_MASK;
	public int MCI_ID_MASK;
	public int UDP_ID_MASK;
	public int TWI_ID_MASK;
	public int SPI_ID_MASK;
	public int SPI0_ID_MASK;
	public int SPI1_ID_MASK;
	public int SSC0_ID_MASK;
	public int SSC1_ID_MASK;
	public int SSC2_ID_MASK;
	public int TC0_ID_MASK;
	public int TC1_ID_MASK;
	public int TC2_ID_MASK;
	public int TC3_ID_MASK;
	public int TC4_ID_MASK;
	public int TC5_ID_MASK;
	public int UHP_ID_MASK;
	public int EMAC_ID_MASK;
	public int IRQ0_ID_MASK;
	public int IRQ1_ID_MASK;
	public int IRQ2_ID_MASK;
	public int IRQ3_ID_MASK;
	public int IRQ4_ID_MASK;
	public int IRQ5_ID_MASK;
	public int IRQ6_ID_MASK;
	public int ADC_ID_MASK;
	public int ISI_ID_MASK;

	/* C code manages:
	 *   usarts 0 and 1 and the usb device port (which it uses for communications)
	 *   spi (which it needs to talk to the power controller)
	 *   timer counters 4 and 5 (which underpin delay.c).
	 *   What is SYSIRQ?
	 */

	// This list should match that in system.h
	public int PERIPHERALS_NOT_ACCESSIBLE_FROM_JAVA;
	public int PERIPHERALS_ACCESSIBLE_FROM_JAVA;

	private void initMasks() {
        FIQ_ID_MASK     = 1 << 0;
        SYSIRQ_ID_MASK  = 1 << 1;
        PIOA_ID_MASK    = 1 << 2;
        PIOB_ID_MASK    = 1 << 3;
        PIOC_ID_MASK    = 1 << 4;
        PIOD_ID_MASK    = 1 << 5;
        US0_ID_MASK     = 1 << 6;
        US1_ID_MASK     = 1 << 7;
        US2_ID_MASK     = 1 << 8;
        US3_ID_MASK     = 1 << 9;
        MCI_ID_MASK     = 1 << 10;
        UDP_ID_MASK     = 1 << 11;
        TWI_ID_MASK     = 1 << 12;
        SPI_ID_MASK     = 1 << 13;
        SPI0_ID_MASK    = SPI_ID_MASK;
        SPI1_ID_MASK    = SPI_ID_MASK;
        SSC0_ID_MASK    = 1 << 14;
        SSC1_ID_MASK    = 1 << 15;
        SSC2_ID_MASK    = 1 << 16;
        TC0_ID_MASK     = 1 << 17;
        TC1_ID_MASK     = 1 << 18;
        TC2_ID_MASK     = 1 << 19;
        TC3_ID_MASK     = 1 << 20;
        TC4_ID_MASK     = 1 << 21;
        TC5_ID_MASK     = 1 << 22;
        UHP_ID_MASK     = 1 << 23;
        EMAC_ID_MASK    = 1 << 24;
        IRQ0_ID_MASK    = 1 << 25;
        IRQ1_ID_MASK    = 1 << 26;
        IRQ2_ID_MASK    = 1 << 27;
        IRQ3_ID_MASK    = 1 << 28;
        IRQ4_ID_MASK    = 1 << 29;
        IRQ5_ID_MASK    = 1 << 30;
        IRQ6_ID_MASK    = 1 << 31;
        ADC_ID_MASK     = 0;
        ISI_ID_MASK     = 0;
    }

	private void initMasks8() {
        FIQ_ID_MASK     = 1 << 0;
        SYSIRQ_ID_MASK  = 1 << 1;
        PIOA_ID_MASK    = 1 << 2;
        PIOB_ID_MASK    = 1 << 3;
        PIOC_ID_MASK    = 1 << 4;
        PIOD_ID_MASK    = 0;  // not in rev8
        ADC_ID_MASK     = 1 << 5;
        US0_ID_MASK     = 1 << 6;
        US1_ID_MASK     = 1 << 7;
        US2_ID_MASK     = 1 << 8;
        US3_ID_MASK     = 1 << 23;
        MCI_ID_MASK     = 1 << 9;
        UDP_ID_MASK     = 1 << 10;
        TWI_ID_MASK     = 1 << 11;
        SPI0_ID_MASK    = 1 << 12;
        SPI1_ID_MASK    = 1 << 13;
        SPI_ID_MASK     = SPI0_ID_MASK;
        SSC0_ID_MASK    = 1 << 14;
        SSC1_ID_MASK    = 0;  // not in rev8
        SSC2_ID_MASK    = 0;  // not in rev8
        TC0_ID_MASK     = 1 << 17;
        TC1_ID_MASK     = 1 << 18;
        TC2_ID_MASK     = 1 << 19;
        TC3_ID_MASK     = 1 << 26;
        TC4_ID_MASK     = 1 << 27;
        TC5_ID_MASK     = 1 << 28;
        UHP_ID_MASK     = 1 << 20;
        EMAC_ID_MASK    = 1 << 21;
        ISI_ID_MASK     = 1 << 22;
        IRQ0_ID_MASK    = 1 << 29;
        IRQ1_ID_MASK    = 1 << 30;
        IRQ2_ID_MASK    = 1 << 31;
        IRQ3_ID_MASK    = 0;  // not in rev8
        IRQ4_ID_MASK    = 0;  // not in rev8
        IRQ5_ID_MASK    = 0;  // not in rev8
        IRQ6_ID_MASK    = 0;  // not in rev8
    }

    public AT91_Peripherals(int rev) {
        if (rev < 8) {
            initMasks();
        } else {
            initMasks8();
        }
			// This list should match that in system.h
        PERIPHERALS_NOT_ACCESSIBLE_FROM_JAVA =
                SYSIRQ_ID_MASK | US0_ID_MASK | US1_ID_MASK | US2_ID_MASK | US3_ID_MASK | UDP_ID_MASK | SPI0_ID_MASK | SPI1_ID_MASK | TC4_ID_MASK | TC5_ID_MASK | IRQ4_ID_MASK | IRQ5_ID_MASK;
        PERIPHERALS_ACCESSIBLE_FROM_JAVA = 0xFFFFFFFF & ~PERIPHERALS_NOT_ACCESSIBLE_FROM_JAVA;
    }

}
