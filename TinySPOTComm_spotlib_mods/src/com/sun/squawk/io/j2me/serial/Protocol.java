/*
 * Copyright 2006-2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.squawk.io.j2me.serial;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;

import javax.microedition.io.Connection;

import com.sun.spot.peripheral.ISerial;
import com.sun.spot.resources.Resources;
import com.sun.spot.util.Properties;
import com.sun.squawk.io.ConnectionBase;

/**
 * serial.Protocol - provides read access to any serial ports on an eSPOT
 * <br><br>
 * Each system library will register an ISerial Resource for each serial line
 * that is supported. The resource will be tagged with "serial=name" that will
 * be matched when GCF needs to open "serial://name". For example the basic SPOT
 * library registers Serial resources for usb, usart, usart0, usart1 & usart2.
 * <br><br>
 * Note that it is possible to append parameters to control the serial port
 * settings. For example:
 * <br><br>
 * "serial://usart?baudrate=115200&databits=8&parity=even&stopbits=0"
 * <br><br>
 * Allowed values for parity are even, odd, mark, space and none.
 */
public class Protocol extends ConnectionBase {

	/* (non-Javadoc)
	 * @see com.sun.squawk.io.ConnectionBase#open(java.lang.String, java.lang.String, int, boolean)
	 */
	public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
        String origName = name;
        String params = null;
        if (name != null && name.startsWith("//")) {
            name = name.substring(2);   // strip off any leading '//'
        }
        int i = name.indexOf('?');
        if (i >= 0) {
            params = name.substring(i);
            name = name.substring(0, i);
        }
        ISerial serialLine = (ISerial) Resources.lookup(ISerial.class, "serial=" + name);
        if (serialLine == null) {
            throw new IllegalArgumentException("Unrecognised URL in serial protocol: " + origName);
		} else {
            if (params != null) {
                handleUSARTParams(serialLine, params);
            }
		}
		return serialLine;
	}

	private void handleUSARTParams(ISerial serialLine, String string) throws IOException {
		// string should be "?key1=val2&key2=val2..."
		if (string.charAt(0) != '?') {
			throw new IllegalArgumentException("Unrecognised URL parameters in serial protocol: " + string);
		}
		Properties p = new Properties();
		p.load(new ByteArrayInputStream(string.substring(1).replace('&', '\n').getBytes()));
		Enumeration keys = p.propertyNames();
        int requestedRate = 0;
        int requestedDatabits = 0;
        String parity = null;
        float stopbits = 0;

		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (key.equalsIgnoreCase("baudrate")) {
				requestedRate = Integer.parseInt(p.getProperty(key));
			} else if (key.equalsIgnoreCase("databits")) {
				requestedDatabits = Integer.parseInt(p.getProperty(key));
			} else if (key.equalsIgnoreCase("parity")) {
				parity = p.getProperty(key);
			} else if (key.equalsIgnoreCase("stopbits")) {
				stopbits = Float.parseFloat(p.getProperty(key));
			} else {
				throw new IllegalArgumentException("Unrecognised URL parameter: " + key);
			}
		}
        serialLine.setUSARTParams(requestedRate, requestedDatabits, parity, stopbits);
	}

}
