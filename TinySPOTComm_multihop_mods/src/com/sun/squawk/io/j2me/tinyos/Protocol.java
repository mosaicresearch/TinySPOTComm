/*
 * Copyright (C) 2009  Daniel van den Akker	(daniel.vandenakker@ua.ac.be)
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * TinySPOTComm 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 only, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *            
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package com.sun.squawk.io.j2me.tinyos;


import java.io.IOException;

import javax.microedition.io.Connection;

import com.sun.spot.io.j2me.tinyos.TinyOSRadioConnImpl;
import com.sun.squawk.io.ConnectionBase;


/**
 * @author Daniel van den Akker
 * 
 * This class provides the "tinyos" protocol for accessing the TinyOS motes using datagrams.
 * It is an implementor of {@link com.sun.spot.io.j2me.tinyos.TinyOSRadioConnection}, and 
 * is based on {@link com.sun.squawk.io.j2me.radiogram.Protocol}
 * 
 * @see com.sun.spot.io.j2me.radiogram.RadiogramConnection
 */
public class Protocol extends ConnectionBase {

	private TinyOSRadioConnImpl conn;

	/**
	 * Default constructor - normally not called by user code which should use the GCF
	 * framework instead.
	 * 
	 */
	public Protocol () {
		super ();
	}

	public Connection open(String protocolName, String name, int mode, boolean timeouts) {
		int portNoAsInt = 0; // Special value if no port requested
		
		//System.out.println("Connection.open called");
		name = name.substring(2); // strip the two /s
		int split = name.indexOf(":");
		
		if (split >= 0 && split != (name.length()-1)) {
			portNoAsInt = Integer.parseInt(name.substring(split+1));
			if (portNoAsInt <= 0 || portNoAsInt > 255) {
				throw new IllegalArgumentException("Cannot open " + name + ". Port number is invalid");
			}
		} else if (split < 0) {
			split = name.length();
		} else {
			// trailing colon special case
			split = name.length()-1;
		}
		byte portNo = (byte) portNoAsInt;
		boolean isServer = (split == 0);
		String addr = name.substring(0, split);

		conn = new TinyOSRadioConnImpl(addr,portNo,isServer,timeouts);
		return conn;
	}

	public void close() throws IOException {
		conn.close();
	}

}