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
import com.sun.spot.resources.Resources;

public class PeripheralChipSelect extends Resource {
	
	public static PeripheralChipSelect SPI_PCS_BD_SEL1() {
        return (PeripheralChipSelect)Resources.lookup(PeripheralChipSelect.class, "BD_SEL1");
    }
	public static PeripheralChipSelect SPI_PCS_BD_SEL2() {
        return (PeripheralChipSelect)Resources.lookup(PeripheralChipSelect.class, "BD_SEL2");
    }
	public static PeripheralChipSelect SPI_PCS_CC2420() {
        return (PeripheralChipSelect)Resources.lookup(PeripheralChipSelect.class, "CC2420");
    }
	public static PeripheralChipSelect SPI_PCS_POWER_CONTROLLER() {
        return (PeripheralChipSelect)Resources.lookup(PeripheralChipSelect.class, "PCTRL");
    }

	private int pcsIndex;
    private String name = null;

    public static final void initPeripheralChipSelect(int hardwareRev) {
        if (Resources.lookup(PeripheralChipSelect.class, "PCTRL") == null) {
            if (SPI_PCS_BD_SEL1() == null) {
                if (hardwareRev <= 6) {
                    createPCS(0, "BD_SEL1");
                    createPCS(1, "BD_SEL2");
                    createPCS(2, "CC2420");
                    createPCS(3, "PCTRL");
                } else {
                    createPCS(0, "PCTRL");
                    createPCS(1, "BD_SEL1");
                    createPCS(2, "BD_SEL2");
                    createPCS(4, "CC2420");
                }
            }
        }
    }

    private static void createPCS(int index, String name) {
        PeripheralChipSelect pcs = new PeripheralChipSelect(index, name);
        pcs.addTag(name);
        Resources.add(pcs);
    }

	private PeripheralChipSelect(int pcsIndex, String name) {
		this.pcsIndex = pcsIndex;
		this.name = name;
    }
	
	public int getPcsIndex() {
		return pcsIndex;
	}
	
	public String toString() {
        if (name != null) {
            return name;
        } else {
			return "PCS_" + pcsIndex;
		}
	}
}
