/*
 * Copyright 2006-2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.io.j2me.remoteprinting;

import com.sun.spot.peripheral.ota.ISpotAdminConstants;
import com.sun.spot.resources.Resource;
import com.sun.spot.resources.Resources;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import com.sun.squawk.Isolate;
import com.sun.squawk.VM;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Manages the creation and removal of remote print connections
 */
public class RemotePrintManager extends Resource implements IRemotePrintManager {

	private static IRemotePrintManager remotePrintManager;

	public synchronized static IRemotePrintManager getInstance() {
		if (remotePrintManager == null) {
            remotePrintManager = (IRemotePrintManager)Resources.lookup(IRemotePrintManager.class);
            if (remotePrintManager == null) {
                remotePrintManager = new RemotePrintManager();
                Resources.add(remotePrintManager);
            }
		}
		return remotePrintManager;
	}

	private String divertingTo;
	private boolean diverted;
    private Hashtable printstreams = new Hashtable();

	RemotePrintManager() {
        Isolate.currentIsolate().addLifecycleListener(new Isolate.LifecycleListener() {
            public void handleLifecycleListenerEvent(Isolate islt, int evt) {
                if (evt == Isolate.SHUTDOWN_EVENT_MASK) {
                    String resetMsg = ISpotAdminConstants.BOOTLOADER_CMD_HEADER + " VM exiting\n";
                    Enumeration en = printstreams.elements();
                    while (en.hasMoreElements()) {
                        OutputStream out = (OutputStream)en.nextElement();
                        try {
                            out.write(resetMsg.getBytes());
                            out.flush();
                        } catch (IOException ex) {
                            // ignore
                        }
                    }
                }
            }
        }, Isolate.SHUTDOWN_EVENT_MASK);
    }

    public synchronized OutputStream openOutputStream(String address, int portNo) {
        String key = Long.toString(IEEEAddress.toLong(address)) + ":" + Integer.toString(portNo);
        OutputStream out = (OutputStream) printstreams.get(key);
        if (out == null) {
            boolean loggingConnections = Utils.isOptionSelected("spot.log.connections", false);
            if (loggingConnections) {
                Isolate.currentIsolate().setProperty("spot.log.connections", "false");
            }
            out = new RemotePrintOutputStream(key);
            printstreams.put(key, out);
            noteRedirection(address);
            if (loggingConnections) {
                Isolate.currentIsolate().setProperty("spot.log.connections", "true");
                VM.println("[radiostream] Adding: Output to " + address + " on port " + (portNo & 0xFF));
            }
        }
        return out;
    }

	public synchronized void redirectOutputStreams(String basestationAddr) {
		if (Utils.isOptionSelected("spot.remote.print.disabled", false)) {
			return;
		}
//		Isolate isolate = Isolate.currentIsolate();
        boolean cancel = diverted && !divertingTo.equals(basestationAddr);
        boolean divert = !diverted || cancel;

        Isolate[] isos = Isolate.getIsolates();
        for (int i = 0; i < isos.length; i++) {
            Isolate isolate = isos[i];
            if (cancel) {
                isolate.removeOut("remoteprint://" + divertingTo + ":" + getEchoPort());
                isolate.removeErr("remoteprint://" + divertingTo + ":" + getEchoPort());
            }
            if (divert) {
//              isolate.removeOut("serial:");
//              isolate.removeErr("serial:");
                isolate.addOut("remoteprint://" + basestationAddr + ":" + getEchoPort());
                isolate.addErr("remoteprint://" + basestationAddr + ":" + getEchoPort());
            }
        }

        diverted = true;
        divertingTo = basestationAddr;
    }

	public synchronized void cancelRedirect() {
		Isolate isolate = Isolate.currentIsolate();
		if (diverted) {
			diverted = false;
			isolate.removeOut("remoteprint://"+divertingTo+":" + getEchoPort());
			isolate.removeErr("remoteprint://"+divertingTo+":" + getEchoPort());			
		}
	}

	private int getEchoPort() {
		return ISpotAdminConstants.MASTER_ISOLATE_ECHO_PORT;
	}

	public void noteRedirection(String remoteAddress) {
		if(!diverted) {
			diverted = true;
			divertingTo = remoteAddress;
		}
	}
}
