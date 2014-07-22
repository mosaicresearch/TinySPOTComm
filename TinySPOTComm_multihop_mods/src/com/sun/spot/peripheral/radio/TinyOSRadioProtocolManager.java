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
package com.sun.spot.peripheral.radio;

import com.sun.spot.interisolate.InterIsolateServer;
import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.radio.proxy.IRadioServerContext;
import com.sun.spot.peripheral.radio.proxy.ProxyTinyOSRadioProtocolManager;

/**
 * @author Daniel van den Akker 
 * A RadioProtocolManager, based on the {@link RadiogramProtocolManager}, that adds support for communication with TinyOS nodes to
 * the SunSPOT stack 
 * */
public class TinyOSRadioProtocolManager extends RadioProtocolManager implements IProtocolManager, ITinyOSRadioProtocolManager
{
	/**
	 * The number identifying the TinyOS Protocol family.
	 */
	public static final byte PROTOCOL_FAMILY_NUMBER = 63; // 0x3f
	/**
	 * The name used by the GCF to identify tinyos connections
	 */
	public static final String PROTOCOL_NAME = "tinyos";
	private static ITinyOSRadioProtocolManager theInstance;
	//used to bypass the LowPAN layer when sending packets
	private IRadioPacketDispatcher dispatch;

	TinyOSRadioProtocolManager(ILowPan lowpan, IRadioPolicyManager radioPolicyManager, IRadioPacketDispatcher dispatch)
	{
		super(lowpan, radioPolicyManager);
		this.dispatch = dispatch;
		lowpan.registerProtocolFamily(PROTOCOL_FAMILY_NUMBER, this);
	}

	TinyOSRadioProtocolManager()
	{
		this(LowPan.getInstance(), RadioFactory.getRadioPolicyManager(), RadioPacketDispatcher.getInstance());
	}

	/**
	 * main method used to add tinyos support for interisolate connections
	 * @param args SunSPOT args
	 */
	public static void main(String[] args)
	{
		InterIsolateServer.run(ProxyTinyOSRadioProtocolManager.CHANNEL_IDENTIFIER, new IRadioServerContext()
		{
			public IRadioProtocolManager getRadioProtocolManager()
			{
				return TinyOSRadioProtocolManager.getInstance();
			}
		});
	}

	/**
	 * Retrieves the instance of the TinyOSRadioProtocolManager
	 * 
	 * @return the instance
	 */
	public synchronized static ITinyOSRadioProtocolManager getInstance()
	{
		if (theInstance == null)
		{
			if (RadioFactory.isMasterIsolate())
			{
				theInstance = new TinyOSRadioProtocolManager();
			}
			else
			{
				theInstance = new ProxyTinyOSRadioProtocolManager(PROTOCOL_FAMILY_NUMBER, PROTOCOL_NAME);
			}
		}
		return theInstance;
	}

	/**
	 * This method creates and sends a TinyOS compatible {@link RadioPacket}.
	 * unlike other SunSPOT protocols, such as the radiogram protocol, sent packets are not passed to the LowPan layer. They
	 * are directly delivered to the RadioPacketDispatcher.
	 */
	public long send(ConnectionID cid, long toAddress, byte[] payload, int length) throws NoAckException, ChannelBusyException, NoRouteException
	{
		if (!cid.canSend())
			throw new IllegalArgumentException(cid.toString() + " cannot be used for sending");

		if (toAddress == 0)
		{
			throw new IllegalArgumentException("Cannot send to address 0");
		}

		//GCF reserves the 1st databyte for the port number.
		payload[PORT_OFFSET] = cid.getPortNo();
		
		//create the radiopacket. depending on the type of connection either a broadcast or a datapacket is created.
		//Since packets are to be TinyOS compatible, 16bit addressing is used. 
		RadioPacket packet;
		if (cid.isBroadcast())
		{
			packet = RadioPacket.getBroadcastPacket(RadioPacket.ADDR_16);
			packet.setDestinationAddress(0xFFFF);
		}
		else
		{
			packet = RadioPacket.getDataPacket(RadioPacket.ADDR_16);
			packet.setDestinationAddress(toAddress);
		}

		// if too much information: cut packet short add '+1' for PROTOCOL_FAMILY_NUMBER
		int payload_size = Math.min(length + 1, packet.getMaxMacPayloadSize()); 
		//fill radiopacket
		packet.setMACPayloadLength(payload_size);
		packet.setMACPayloadAt(0, PROTOCOL_FAMILY_NUMBER); // set protocol family
		System.arraycopy(payload, 0, packet.buffer, packet.getPayloadOffset() + 1, payload_size - 1);

		if(cid.isBroadcast())
		{
			dispatch.sendPacket(packet); //send only once
		}
		else
		{
			//this is normally done in the LowPAN layer, since we're bypassing that, we're doing it here
			NoAckException no_ack = null;
			for (int i = 0; i < 3; i++) 
			 {
				try
				{
					dispatch.sendPacket(packet);
					no_ack=null;
					break;
				}
				catch (NoAckException e)
				{
					no_ack = e;
				}
			}
			if(no_ack != null)
				throw no_ack;
		}

		return packet.getTimestamp();
	}

	public synchronized void processIncomingData(byte[] payload, LowPanHeaderInfo headerInfo)
	{
		// First see if we have a matching server connection, because if we have, all input goes there
		byte portNumber = payload[PORT_OFFSET];
		ConnectionState destinationCS = getConnectionState(0, SERVER, portNumber);
		if (destinationCS == null)
		{
			destinationCS = getConnectionState(headerInfo.originator, INPUT, portNumber);
		}
		if (destinationCS == null)
		{
			// System.out.println("discarding packet with key " + needle);
		}
		else
		{
			destinationCS.addToQueue(new IncomingData(payload, headerInfo));
		}
	}

	public String getName()
	{
		return PROTOCOL_NAME;
	}

	public synchronized ConnectionID addServerConnection(byte portNo)
	{
		return addConnection(true, new ConnectionID(0, portNo, SERVER));
	}

	public synchronized ConnectionID addBroadcastConnection(byte portNo)
	{
		ConnectionID cid = new ConnectionID(-1, portNo, BROADCAST);

		BroadcastConnectionState foundState = (BroadcastConnectionState) connectionIDTable.get(cid);
		if (foundState != null)
		{
			foundState.incrementReferenceCount();
			return cid;
		}
		return addConnection(false, cid);
	}

	int getNumberOfEntriesInConnectionStateTable()
	{
		return connectionIDTable.size();
	}
}
