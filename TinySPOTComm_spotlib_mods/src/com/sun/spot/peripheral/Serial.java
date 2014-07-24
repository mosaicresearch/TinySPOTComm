/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
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
import java.io.InputStream;
import java.io.OutputStream;

import com.sun.spot.resources.Resource;
import com.sun.squawk.Address;
import com.sun.squawk.Isolate;
import com.sun.squawk.Unsafe;
import com.sun.squawk.VM;
import com.sun.squawk.vm.ChannelConstants;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;

/**
 * Serial - provides read access to the serial/USB ports of an eSPOT
 * <br><br>
 * The rev8 eSPOT has 3 available usarts: usart0, usart1 & usart2.<br>
 * The initial eSPOT only has one: usart0.<br>
 * Note: usart is the same as usart0
 * <br><br>
 * Note that in the case where the URL is "serial://usart" it is possible to append parameters
 * to control the serial port settings. For example:
 * <br><br>
 * "serial://usart?baudrate=115200&databits=8&parity=even&stopbits=0"
 * <br><br>
 * Allowed values for parity are even, odd, mark and none. 
 */
public class Serial extends Resource implements ISerial {
	
	private static final int OLD_USART0_BASE_ADDRESS = 0xFFFC0000;
	private static final int OLD_USART1_BASE_ADDRESS = 0xFFFC4000;
	private static final int USART0_BASE_ADDRESS     = 0xFFFB0000;
	private static final int USART1_BASE_ADDRESS     = 0xFFFB4100;
	private static final int USART2_BASE_ADDRESS     = 0xFFFB8000;
	private static final int USART3_BASE_ADDRESS     = 0xFFFD0000;
	private static final int US_BRGR                 = 0x20 >> 2;
	private static final int US_MR                   = 0x04 >> 2;

	// These have to match the values in syscalls-impl.h
	private int type; // default = 1, usb = 2; usart0 = 4, usart1 = 5, usart2 = 6
	private int usart_base_address;
    private Isolate inIso = null;
    private Vector outIsos = new Vector();

    public Serial() {
        // default constructor for child classes
    }

	public Serial(int type) {
        this.type = type;
        int rev = Spot.getInstance().getHardwareType();
        boolean rev8board = rev > 6;
        if (!rev8board && type > 4) {
            throw new IllegalArgumentException("Usart" + (type - 4) + " is not available on rev " + rev + " SPOTs");
        }
        switch (type) {
            case 4:         // usart0 = us0
                usart_base_address = rev8board ? USART0_BASE_ADDRESS : OLD_USART0_BASE_ADDRESS;
                break;
            case 5:         // usart1 = us2
                usart_base_address = USART2_BASE_ADDRESS;
                break;
            case 6:         // usart2 = us3
                usart_base_address = USART3_BASE_ADDRESS;
                break;
            default:        // usb & us1
                usart_base_address = rev8board ? USART1_BASE_ADDRESS : OLD_USART1_BASE_ADDRESS;
        }
	}

    protected void cleanUpOldIsolates() {
        if (inIso != null && !inIso.isAlive()) {
            inIso = null;
        }
        int i = 0;
        while (i < outIsos.size()) {
            Isolate oso = (Isolate)outIsos.elementAt(i);
            if (oso == null || !oso.isAlive()) {
                outIsos.removeElementAt(i);         // remove any no longer running Isolates
            } else {
                i++;
            }
        }
    }

    /**
     * Check if the serial line is in use by another running isolate.
     *
     * @return true if the serial line is in use
     */
    protected boolean inUseByOtherIsolate() {
        cleanUpOldIsolates();
        Isolate iso = Isolate.currentIsolate();
        boolean inUse = (inIso != null && inIso != iso);    // another Isolate reading it?
        int i = 0;
        while (i < outIsos.size() && !inUse) {
            Isolate oso = (Isolate)outIsos.elementAt(i);
            inUse = iso != oso;                         // another Isolate writing it?
            i++;
        }
        return inUse;
    }

    /**
     * Set one of the ARM9's USART line parameteres.
     * @param baudrate baud speed to set module to
     * @param databits supported values are 5,6,7,8
     * @param parity supported options are none, odd, even, space & mark
     * @param stopbits supported options are 1, 1.5 or 2
     */
    public void setUSARTParams(int baudrate, int databits, String parity, float stopbits) throws IOException {
        if (inUseByOtherIsolate()) {
            throw new IOException("Can't set usart" + (type - 3) + " parameters as device is in use.");
        }

        if (baudrate > 0) {
            int requiredClockDivisor = (2 * Spot.getInstance().getMclkFrequency() / (16 * baudrate) + 1) / 2;
            Unsafe.setInt(Address.fromPrimitive(usart_base_address), US_BRGR, requiredClockDivisor);
        }

		int curmode = Unsafe.getInt(Address.fromPrimitive(usart_base_address), US_MR);
        int mode = curmode;
        if (databits != 0) {
            mode &= ~(0x3 << 6);
            mode |= (databits - 5) << 6;
        }
        if (parity != null) {
            mode &= ~(0x7 << 9);
            if (parity.equalsIgnoreCase("none")) {
                mode |= 0x4 << 9;
            } else if (parity.equalsIgnoreCase("even")) {
                mode |= 0x0 << 9;
            } else if (parity.equalsIgnoreCase("odd")) {
                mode |= 0x1 << 9;
            } else if (parity.equalsIgnoreCase("space")) {
                mode |= 0x2 << 9;
            } else if (parity.equalsIgnoreCase("mark")) {
                mode |= 0x3 << 9;
            } else {
                throw new IllegalArgumentException("Unrecognised parity parameter: " + parity);
            }
        }
        if (stopbits != 0) {
            mode &= ~(0x3 << 12);
            mode |= (int) (stopbits * 2 - 2) << 12;
		}
        if (mode != curmode) {
            Unsafe.setInt(Address.fromPrimitive(usart_base_address), US_MR, mode);
        }
	}

    public void close() {
        // nothing to do here
    }

	public InputStream openInputStream() throws IOException {
        if (inIso != null && inIso.isAlive()) {
            throw new IOException("Serial input stream already in use");
        }
        inIso = Isolate.currentIsolate();
		return new SerialInputStream(this);
    }
	
    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

	public OutputStream openOutputStream() throws IOException {
        cleanUpOldIsolates();
        outIsos.addElement(Isolate.currentIsolate());
		return new SerialOutputStream(this);
	}

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    public void closeInputStream() throws IOException {
        inIso = null;
    }

    public void closeOutputStream() throws IOException {
        outIsos.removeElement(Isolate.currentIsolate());
    }

    private byte[] resultArray = new byte[1];
    private int[] numberOfCharactersReadArray = new int[1];

    public int read() throws IOException {
        int chars = read(resultArray, 0, 1);
        if (chars != 1) {
            throw new IOException("Error reading one character:" + chars);
        }
        return resultArray[0] & 0xFF;
    }

    public int read(byte b[], int off, int len) throws IOException {
        VM.execIO(ChannelConstants.GET_SERIAL_CHARS, 0, off, len, type, 0, 0, 0, numberOfCharactersReadArray, b);
        return numberOfCharactersReadArray[0];
    }

    public int available() throws IOException {
        return VM.execSyncIO(ChannelConstants.AVAILABLE_SERIAL_CHARS, type, 0, 0, 0, 0, 0, null, null);
    }

    private byte[] oneByteArray = new byte[1];

    public synchronized void write(int b) throws IOException {
        oneByteArray[0] = (byte) b;
        write(oneByteArray, 0, 1);
    }

    public synchronized void write(byte b[], int off, int len) throws IOException {
        VM.execSyncIO(ChannelConstants.WRITE_SERIAL_CHARS, off, len, type, 0, 0, 0, b, null);
    }


	private class SerialInputStream extends InputStream {

		private Serial parent;

		SerialInputStream(Serial parent) throws IOException {
            this.parent = parent;
		}

		public int read() throws IOException {
            return parent.read();
		}
		
		public int read(byte b[], int off, int len) throws IOException {
            return parent.read(b, off, len);
		}
		
		public int available() throws IOException {
            return parent.available();
		}

		public void close() throws IOException {
			super.close();
            parent.closeInputStream();
		}
	}

	private class SerialOutputStream extends OutputStream {
		
		private Serial parent;

		SerialOutputStream(Serial parent) throws IOException {
            this.parent = parent;
		}
		
		public void write(int b) throws IOException {
			parent.write(b);
		}
		
		public void write(byte b[], int off, int len) throws IOException {
			parent.write(b, off, len);
		}
		
		public void close() throws IOException {
			super.close();
            parent.closeOutputStream();
		}
	}
}

