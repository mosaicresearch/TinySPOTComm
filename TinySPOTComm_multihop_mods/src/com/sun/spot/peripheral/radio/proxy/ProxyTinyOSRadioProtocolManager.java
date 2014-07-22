/*
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
 */
package com.sun.spot.peripheral.radio.proxy;

import com.sun.spot.interisolate.ReplyEnvelope;
import com.sun.spot.peripheral.radio.ConnectionID;
import com.sun.spot.peripheral.radio.IRadiogramProtocolManager;
import com.sun.spot.peripheral.radio.ITinyOSRadioProtocolManager;
import com.sun.spot.peripheral.radio.proxy.ProxyRadioProtocolManager;

/**
 * @author Daniel van den Akker
 * This class allows for inter-isolate 'tinyos://' connections to be made
 */
public class ProxyTinyOSRadioProtocolManager extends ProxyRadioProtocolManager implements ITinyOSRadioProtocolManager
{
	/**
	 * the identifier for the tinyos protocols
	 */
	public static final String CHANNEL_IDENTIFIER = "TINYOS_SERVER";

	/**
	 * @param protocolNum the protocol number
	 * @param name name
	 */
	public ProxyTinyOSRadioProtocolManager(byte protocolNum, String name)
	{
		super(protocolNum, name, CHANNEL_IDENTIFIER);
	}

	public ConnectionID addServerConnection(byte portNo)
	{
		ReplyEnvelope resultEnvelope = requestSender.send(new AddServerConnectionCommand(portNo));
		resultEnvelope.checkForRuntimeException();
		return (ConnectionID) resultEnvelope.getContents();
	}

	public ConnectionID addBroadcastConnection(byte portNo)
	{
		ReplyEnvelope resultEnvelope = requestSender.send(new AddBroadcastConnectionCommand(portNo));
		resultEnvelope.checkForRuntimeException();
		return (ConnectionID) resultEnvelope.getContents();
	}

}
