/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.peripheral.ota;

import com.sun.spot.service.IService;
import java.util.Date;

public interface IOTACommandServer extends IService {

	/**
	 * The radiostream port on which to listen for hosts connecting
	 */
	int PORT = 8;
	
	/**
	 * Command that we recognise to start a new session
	 */
	String START_OTA_SESSION_CMD = "START_OTA_SESSION_CMD";
	
	/**
	 * Command to respond information about the SPOT 
	 */
	String HELLO_CMD = "HELLO_CMD";

	/**
	 * Version of the {@link #HELLO_CMD} that we support 
	 */
	int HELLO_COMMAND_VERSION = 2;
        
	/**
	 * Starting with (major) version 2, we support major/minor version
	 * numbering. This lets us signal/detect changes that are backward
	 * compatible and those that aren't.
	 */
	int HELLO_COMMAND_MINOR_VERSION = 2;
	
	/**
	 * Major version of the eSPOT hardware to report in response to {@link #HELLO_CMD}
	 */
	int HARDWARE_MAJOR_REV_ESPOT = 0;
	
	
	/**
	 * Attach a listener to be notified of the start and stop of flash
	 * operations.
	 * 
	 * @param sml the listener
	 */
	void addListener(IOTACommandServerListener sml);

	/**
	 * Answer the IEEE address of the sender of the last command received.
	 * 
	 * @return -- the address
	 */
	String getBaseStationAddress();

	/**
	 * @return Returns true if the server has been suspended by software.
	 */
	boolean isSuspended();

	/**
	 * @param suspended Suspends or resumes the server (it is initially running).
	 */
	void setSuspended(boolean suspended);

	/**
	 * @return The time when the server last received a message from the host
	 */
	Date timeOfLastMessageFromHost();

}
