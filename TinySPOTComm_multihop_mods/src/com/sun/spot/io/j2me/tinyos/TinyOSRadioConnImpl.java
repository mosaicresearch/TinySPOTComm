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

import java.io.IOException;

import javax.microedition.io.Connection;
import javax.microedition.io.Datagram;

import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.RadioConnectionBase;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.peripheral.radio.ConnectionID;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.ITinyOSRadioProtocolManager;
import com.sun.spot.peripheral.radio.IncomingData;
import com.sun.spot.peripheral.radio.NoMeshLayerAckException;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.RadioPolicy;
import com.sun.spot.peripheral.radio.TinyOSRadioProtocolManager;
import com.sun.spot.util.IEEEAddress;

/**
 * @author Daniel van den Akker
 * 
 * This class is heavily based on {@link com.sun.spot.io.j2me.radiogram.RadiogramConnImpl} and provides the "tinyos:" protocol for accessing TinyOS motes from SunSPOTs.
 * Restrictions Apply. See {@link TinyOSRadioConnection} for more information
 * @see TinyOSRadioConnection
 */
public class TinyOSRadioConnImpl extends RadioConnectionBase implements TinyOSRadioConnection
{

	ConnectionID sendConnectionID;
	ConnectionID receiveConnectionID;

	private static ITinyOSRadioProtocolManager protocolManager;
	private static IRadioPolicyManager radioPolicyManager;

	private static synchronized ITinyOSRadioProtocolManager getProtocolManager()
	{
		if (protocolManager == null)
		{
			protocolManager = TinyOSRadioProtocolManager.getInstance();
		}
		return protocolManager;
	}

	/**
	 * @param protocolManager the new Protocol Manager
	 */
	public static void setProtocolManager(ITinyOSRadioProtocolManager protocolManager)
	{
		TinyOSRadioConnImpl.protocolManager = protocolManager;
	}

	/**
	 * @return the RadioPolicyManager
	 */
	public static synchronized IRadioPolicyManager getRadioPolicyManager()
	{
		if (radioPolicyManager == null)
		{
			radioPolicyManager = RadioFactory.getRadioPolicyManager();
		}
		return radioPolicyManager;
	}

	/**
	 * @param manager the new RadioPolicyManager
	 */
	public static void setRadioPolicyManager(IRadioPolicyManager manager)
	{
		radioPolicyManager = manager;
	}

	/**
	 * DO NOT USE THIS CONSTRUCTOR - connections should be created using Connector.open(...)
	 * @param addr address
	 * @param portNo port number
	 * @param isServer connection is server?
	 * @param timeouts the timeouts
	 */
	public TinyOSRadioConnImpl(String addr, byte portNo, boolean isServer, boolean timeouts)
	{
		if (isServer)
		{
			receiveConnectionID = sendConnectionID = getProtocolManager().addServerConnection(portNo);
		}
		else
		{
			if (addr.toLowerCase().equals("broadcast"))
			{
				sendConnectionID = getProtocolManager().addBroadcastConnection(portNo);
				//see whether broadcast forwarding is possible ?
				//no it isn': forward broadcasting requires meshing,
				//which is unsupported by the basic TinyOS Stack
				sendConnectionID.setMaxBroadcastHops((byte) 1);
				receiveConnectionID = null;
			}
			else
			{
				long macAddress = new IEEEAddress(addr).asLong();
				sendConnectionID = getProtocolManager().addOutputConnection(macAddress, portNo);
				receiveConnectionID = getProtocolManager().addInputConnection(macAddress, portNo);
			}
		}

		if (receiveConnectionID != null)
		{
			getRadioPolicyManager().registerConnection(receiveConnectionID);
			getRadioPolicyManager().policyHasChanged(receiveConnectionID, RadioPolicy.ON);
		}
		if (timeouts)
		{
			setTimeout(DEFAULT_TIMEOUT);
		}
	}

	/**
	 * DO NOT USE THIS CONSTRUCTOR - connections should be created using Connector.open(...)
	 */
	public TinyOSRadioConnImpl()
	{
	}

	public void close() throws IOException
	{
		if (receiveConnectionID != null)
		{
			getProtocolManager().closeConnection(receiveConnectionID);
			getRadioPolicyManager().deregisterConnection(receiveConnectionID);
			receiveConnectionID = null;
			if (isServer())
			{
				// if it's a server, then sendConnectionID == receiveConnectionID so don't deregister it twice
				sendConnectionID = null;
			}
		}
		if (sendConnectionID != null)
		{
			getProtocolManager().closeConnection(sendConnectionID);
			sendConnectionID = null;
		}
		super.close();
	}

	public int getMaximumLength()
	{
		return TinyOSPacket.MAX_LENGTH;
	}

	public int getNominalLength()
	{
		return TinyOSPacket.MAX_LENGTH;
	}

	public void send(Datagram dgram) throws NoAckException, ChannelBusyException, NoRouteException, NoMeshLayerAckException
	{
		TinyOSPacket rg = (TinyOSPacket) dgram;
		if (rg.getConnection() == this)
		{
			((TinyOSPacket) dgram).send();
		}
		else
		{
			throw new IllegalArgumentException("Attempt to send radiogram on unassociated connection");
		}
	}

	public void receive(Datagram dgram) throws IOException
	{
		if (isBroadcast())
		{
			throw new IllegalStateException("Can't receive on broadcast connection");
		}
		TinyOSPacket rg = (TinyOSPacket) dgram;
		if (rg.getConnection() == this)
		{
			((TinyOSPacket) dgram).receive();
		}
		else
		{
			throw new IllegalArgumentException("Attempt to receive radiogram on unassociated connection");
		}
	}

	public Datagram newDatagram(int size)
	{
		return new TinyOSPacket(size, this);
	}

	public Datagram newDatagram(int size, String addr)
	{
		return new TinyOSPacket(size, this, addr);
	}

	public Datagram newDatagram(byte[] buf, int size)
	{
		throw new IllegalStateException("Method newDatagram(byte[] buf, int size) is not implemented");
	}

	public Datagram newDatagram(byte[] buf, int size, String addr)
	{
		throw new IllegalStateException("Method newDatagram(byte[] buf, int size, String addr) is not implemented");
	}

	/**
	 * @return whether current connection is a broadcast connection
	 */
	public boolean isBroadcast()
	{
		return sendConnectionID.isBroadcast();
	}

	/**
	 * @return whether the current connection is a point-to-point connection
	 */
	public boolean isPointToPoint()
	{
		return sendConnectionID.isOutput();
	}

	/**
	 * @return whether the connection is a server
	 */
	public boolean isServer()
	{
		return sendConnectionID.isServer();
	}

	/**
	 * @return the MAC address of the connection
	 */
	public long getMacAddress()
	{
		return sendConnectionID.getMacAddress();
	}

	public void setRadioPolicy(RadioPolicy policy)
	{
		if (receiveConnectionID != null)
		{
			getRadioPolicyManager().policyHasChanged(receiveConnectionID, policy);
		}
		else
		{
			throw new IllegalStateException("Can't set radio policy for output-only connections");
		}
	}

	public Connection open(String arg0, String arg1, int arg2, boolean arg3) throws IOException
	{
		throw new SpotFatalException("cannot reopen a connection");
	}

	long send(byte[] payload, long toAddress, int length) throws NoAckException, ChannelBusyException, NoRouteException, NoMeshLayerAckException
	{
		return getProtocolManager().send(sendConnectionID, toAddress, payload, length);
	}

	IncomingData receivePacket(long timeout)
	{
		return getProtocolManager().receivePacket(receiveConnectionID, timeout);
	}

	/**
	 * @return retrieves a packet from the radio 
	 */
	public IncomingData receivePacket()
	{
		return getProtocolManager().receivePacket(receiveConnectionID);
	}

	public byte getLocalPort()
	{
		return sendConnectionID.getPortNo();
	}

	public boolean packetsAvailable()
	{
		return getProtocolManager().packetsAvailable(receiveConnectionID);
	}

	public void setMaxBroadcastHops(int hops)
	{
		throw new IllegalStateException("Broadcasts across multiple hops are not supported");
	}

	public int getMaxBroadcastHops()
	{
		return 1;
	}
}
