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
package com.sun.spot.io.j2me.tinyos;

import javax.microedition.io.DatagramConnection;

import com.sun.spot.peripheral.IRadioControl;
import com.sun.spot.peripheral.ITimeoutableConnection;
import com.sun.spot.peripheral.IMultipleHopConnection;

/**
 * @author Daniel van den Akker
 * 
 * This interface provides an easy access to TinyOS-motes.
 * The interface is similar to the 'Radiogram' GCF interface but has a few essential differences: <br>
 * <ul>
 * 	<li>Data that was sent on a TinyOS mote using the AMSenderC  component is presented to SunSPOT as a TinyOSPacket. TinyOSPackets sent
 *      to a TinyOS-mote may be received with the AMReceiverC component. Please bear in mind that, depending on the mote-archtecture byte-conversions may be necessary.</li>
 * 	<li>The AMId required for the AMSenderC and AMReceiverC components translates to the port number used for TinyOSRadioConnections</li>
 * 	<li>The 'tinyos://' protocol currently does NOT support routing or fragmentation. This means that packets are always sent single-hop and that the maximum nuber of bytes in a 
 * 		TinyOSPacket is limited by the maximum allowed bytes in a IEEE 802.15.4 MAC frame.</li>
 * </ul>
 * 
 * IMPORTANT The 'tinyos://'-protocol was NOT designed to be used as the basis for the implementation of higher-level protocols.
 * In that case, the {@link com.sun.spot.peripheral.radio.TinyOSRadioProtocolManager} should be accessed directly,
 * creating new GCF-handlers for each protocol.
 */
public interface TinyOSRadioConnection extends ITimeoutableConnection, DatagramConnection, IRadioControl, IMultipleHopConnection {
    /**
     * determines whether there are radiograms that can be read from this connection
     * @return true if there are packets that can be read from the connection
     */
	public boolean packetsAvailable();
    
}
