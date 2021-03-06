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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import javax.microedition.io.Datagram;

import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.ITinyOSRadioProtocolManager;
import com.sun.spot.peripheral.radio.IncomingData;
import com.sun.spot.peripheral.radio.LowPanHeaderInfo;
import com.sun.spot.peripheral.radio.NoMeshLayerAckException;
import com.sun.spot.peripheral.radio.RadioPacket;
import com.sun.spot.util.IEEEAddress;

/**
 * @author Daniel van den Akker 
 * 
 * Helper class, based on {@link com.sun.spot.io.j2me.radiogram.Radiogram}, used for providing for "tinyos:" connections. This
 * class implements {@link javax.microedition.io.Datagram} for communication between Spots AND/OR TinyOS Motes. You should NOT normally instantiate this
 * class directly, but rather via the GCF framework: see the first reference below for more details.
 * 
 * @see TinyOSRadioConnection
 */
public class TinyOSPacket implements Datagram
{

	/**
	 * Maximum number of bytes that can be stored in a packet. Data must fit into a single 802.15.4 packet, minus 2 bytes: 1 for lowpan 1 for multiplexing
	 */
	// -9: account for 16-bit addressed MAC header, -2 account for lowpan + multiplexing byte
	public static final int MAX_LENGTH = RadioPacket.MAX_DATA_LENGTH - 9 - 1 - ITinyOSRadioProtocolManager.DATA_OFFSET;
	private byte[] payload;
	private int payloadIndex;
	private int endOfDataIndex;
	private DataInputStream dis;
	private DataOutputStream dos;
	private long address;

	private TinyOSRadioConnImpl connection;
	private LowPanHeaderInfo headerInfo;
	private long timestamp;

	/**
	 * @param size the required size of this TinyOSPacket
	 * @param connection the Protocol that is using this TinyOSPacket
	 */
	public TinyOSPacket(int size, TinyOSRadioConnImpl connection)
	{
		this.connection = connection;
		reset();
		setRadioPacket(size);
		if (connection.isPointToPoint())
		{
			address = connection.getMacAddress();
		}
		else if (connection.isBroadcast())
		{
			address = -1;
		}
		dis = new DataInputStream(new TinyOSInputStream());
		dos = new DataOutputStream(new TinyOSOutputStream());
	}

	/**
	 * @param size the required size of this TinyOSPacket
	 * @param connection the Protocol that is using this TinyOSPacket
	 * @param addr the address to associate with this TinyOSPacket
	 */
	public TinyOSPacket(int size, TinyOSRadioConnImpl connection, String addr)
	{
		this(size, connection);
		setAddress(addr);
	}

	/**
	 * Return the address associated with this TinyOSPacket. If this TinyOSPacket has been received then this is the sender's address. If the address has been
	 * set with setAddress(...) then this is the address that was set.
	 * 
	 * @return the address associated with this TinyOSPacket in dotted hex notation
	 */
	public String getAddress()
	{
		return new IEEEAddress(address).asDottedHex();
	}

	/**
	 * Return the address associated with this TinyOSPacket. If this TinyOSPacket has been received then this is the sender's address. If the address has been
	 * set with setAddress(...) then this is the address that was set.
	 * 
	 * @return the address associated with this TinyOSPacket as a long
	 */
	public long getAddressAsLong()
	{
		return address;
	}

	/**
	 * Returns the contents of the TinyOSPacket as a byte array
	 * 
	 * @return a byte array containing the contents of the TinyOSPacket
	 */
	public byte[] getData()
	{
		byte[] buffer = new byte[getLength()];
		System.arraycopy(payload, ITinyOSRadioProtocolManager.DATA_OFFSET, buffer, 0, buffer.length);
		return buffer;
	}

	/**
	 * Return the number of bytes of data in this datagram. Operates on a received TinyOSPacket If called on an outgoing datagram, it will return zero.
	 * 
	 * @return the number of bytes of data
	 */
	public int getLength()
	{
		return endOfDataIndex - ITinyOSRadioProtocolManager.DATA_OFFSET;
	}

	/**
	 * @return always returns 0 because offsets are not used for TinyOSPackets
	 */
	public int getOffset()
	{
		return 0;
	}

	/**
	 * Set the address associated with this TinyOSPacket. This is normally the destination address to which the TinyOSPacket will be sent.
	 * 
	 * @param addr the address in dotted hex or as a simple integer
	 */
	public void setAddress(String addr)
	{
		if (!connection.isServer())
		{
			// can't set the address of non-server TinyOSPacket
			throw new IllegalStateException("Cannot set the address of a TinyOSPacket on a non-server connection");
		}
		address = new IEEEAddress(addr).asLong();
	}

	/**
	 * Set the address associated with this TinyOSPacket by copying it from the supplied TinyOSPacket. This is normally the destination address to which the
	 * TinyOSPacket will be sent.
	 * 
	 * @param reference the TinyOSPacket from which the address is to be copied
	 */
	public void setAddress(Datagram reference)
	{
		if (!connection.isServer())
		{
			// can't set the address of non-server TinyOSPacket
			throw new IllegalStateException("Cannot set the address of a TinyOSPacket on a non-server connection");
		}
		address = ((TinyOSPacket) reference).address;
	}

	/**
	 * Not implemented
	 */
	public void setLength(int len)
	{
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * Not implemented
	 */
	public void setData(byte[] buffer, int offset, int len)
	{
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * Ensures that the next read or write operation will read/write from the start of the TinyOSPacket
	 */
	public void reset()
	{
		payloadIndex = endOfDataIndex = ITinyOSRadioProtocolManager.DATA_OFFSET;
	}

	/**
	 * Ensures that the next read operation will read from the start of the TinyOSPacket
	 * 
	 * @throws java.io.IOException unable to reset read pointer to beginning of datagram
	 */
	public void resetRead() throws IOException
	{
		dis.reset();
	}

	/**
	 * @return the number of bytes available for reading
	 * @throws IOException when an err occurs
	 */
	public int available() throws IOException
	{
		return dis.available();
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataInput#readFully(byte[])
	 */
	public void readFully(byte[] b) throws IOException
	{
		dis.readFully(b);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataInput#readFully(byte[], int, int)
	 */
	public void readFully(byte[] b, int off, int len) throws IOException
	{
		dis.readFully(b, off, len);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataInput#skipBytes(int)
	 */
	public int skipBytes(int n) throws IOException
	{
		return dis.skipBytes(n);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataInput#readBoolean()
	 */
	public boolean readBoolean() throws IOException
	{
		return dis.readBoolean();
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataInput#readByte()
	 */
	public byte readByte() throws IOException
	{
		return dis.readByte();
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataInput#readUnsignedByte()
	 */
	public int readUnsignedByte() throws IOException
	{
		return dis.readUnsignedByte();
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataInput#readShort()
	 */
	public short readShort() throws IOException
	{
		return dis.readShort();
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataInput#readUnsignedShort()
	 */
	public int readUnsignedShort() throws IOException
	{
		return dis.readUnsignedShort();
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataInput#readChar()
	 */
	public char readChar() throws IOException
	{
		return dis.readChar();
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataInput#readInt()
	 */
	public int readInt() throws IOException
	{
		return dis.readInt();
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataInput#readLong()
	 */
	public long readLong() throws IOException
	{
		return dis.readLong();
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataInput#readUTF()
	 */
	public String readUTF() throws IOException
	{
		return dis.readUTF();
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataOutput#write(int)
	 */
	public void write(int b) throws IOException
	{
		dos.write(b);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataOutput#write(byte[])
	 */
	public void write(byte[] b) throws IOException
	{
		dos.write(b);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataOutput#write(byte[], int, int)
	 */
	public void write(byte[] b, int off, int len) throws IOException
	{
		dos.write(b, off, len);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataOutput#writeBoolean(boolean)
	 */
	public void writeBoolean(boolean v) throws IOException
	{
		dos.writeBoolean(v);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataOutput#writeByte(int)
	 */
	public void writeByte(int v) throws IOException
	{
		dos.writeByte(v);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataOutput#writeShort(int)
	 */
	public void writeShort(int v) throws IOException
	{
		dos.writeShort(v);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataOutput#writeChar(int)
	 */
	public void writeChar(int v) throws IOException
	{
		dos.writeChar(v);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataOutput#writeInt(int)
	 */
	public void writeInt(int v) throws IOException
	{
		dos.writeInt(v);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataOutput#writeLong(long)
	 */
	public void writeLong(long v) throws IOException
	{
		dos.writeLong(v);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataOutput#writeChars(java.lang.String)
	 */
	public void writeChars(String s) throws IOException
	{
		dos.writeChars(s);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.DataOutput#writeUTF(java.lang.String)
	 */
	public void writeUTF(String str) throws IOException
	{
		dos.writeUTF(str);
	}

	/**
	 * Link Quality Indication (LQI) is a characterization of the quality of a received packet. Its value is computed from the CORR, correlation value. The LQI
	 * ranges from 0 (bad) to 255 (good).
	 * 
	 * @return - LQI value
	 * @see com.sun.spot.peripheral.radio.RadioPacket#getLinkQuality()
	 */
	public int getLinkQuality()
	{
		return headerInfo.linkQuality;
	}

	/**
	 * CORR measures the average correlation value of the first 4 bytes of the packet header. A correlation value of ~110 indicates a maximum quality packet
	 * while a value of ~50 is typically the lowest quality packet detectable by the SPOT's receiver.
	 * 
	 * @return - CORR value
	 * @see com.sun.spot.peripheral.radio.RadioPacket#getCorr()
	 */
	public int getCorr()
	{
		return headerInfo.corr;
	}

	/**
	 * RSSI (received signal strength indicator) measures the strength (power) of the signal for the packet. It ranges from +60 (strong) to -60 (weak). To
	 * convert it to decibels relative to 1 mW (= 0 dBm) subtract 45 from it, e.g. for an RSSI of -20 the RF input power is approximately -65 dBm.
	 * 
	 * @return - RSSI value
	 * @see com.sun.spot.peripheral.radio.RadioPacket#getRssi()
	 */
	public int getRssi()
	{
		return headerInfo.rssi;
	}

	/**
	 * Hop count is the number of times a packet will be retransmitted until it reaches the final destination. For AODV routed packets, the remaining hop count
	 * should be zero, since AODV routes use exact hop counts. Hop count may be non-zero for mesh forwarded broadcast packets
	 * 
	 * @return - number of hops remaining in this TinyOSPacket
	 */
	public int getHopCount()
	{
		String hopcount = headerInfo.getProperty("hopcount");
		return Integer.parseInt(hopcount);
	}

	/**
	 * @return timestamp
	 */
	public long getTimestamp()
	{
		return timestamp;
	}
        
        public boolean isBroadcast() {
           return headerInfo.destinationAddress == 0xFFFF;
        }

	void send() throws NoAckException, ChannelBusyException, NoRouteException, NoMeshLayerAckException
	{
		timestamp = connection.send(payload, address, endOfDataIndex);
	}

	void receive() throws IOException
	{
		reset();
		long timeout = connection.getTimeout();
		IncomingData receivedData;
		if (timeout >= 0)
		{
			receivedData = connection.receivePacket(timeout);
			if (receivedData == null)
			{
				throw new TimeoutException("TinyOSPacket receive timeout");
			}
		}
		else
		{
			receivedData = connection.receivePacket();
			if (receivedData == null)
			{
				throw new InterruptedIOException("Connection was closed");
			}
		}
		System.arraycopy(receivedData.payload, 0, payload, 0, receivedData.payload.length);
		endOfDataIndex = receivedData.payload.length;
		headerInfo = receivedData.headerInfo;
		// rps -- changed to orginator address (BugID 572)
		address = headerInfo.originator;
		timestamp = headerInfo.timestamp;
	}

	public double readDouble() throws IOException
	{
		return dis.readDouble();
	}

	public float readFloat() throws IOException
	{
		return dis.readFloat();
	}

	public void writeDouble(double v) throws IOException
	{
		dos.writeDouble(v);
	}

	public void writeFloat(float v) throws IOException
	{
		dos.writeFloat(v);
	}

	/**
	 * @return the Protocol controlling this TinyOSPacket
	 */
	TinyOSRadioConnImpl getConnection()
	{
		return connection;
	}

	/**
	 * the timeout for the current connection
	 * 
	 * @return timeout
	 */
	public long getTimeout()
	{
		return connection.getTimeout();
	}

	private void setRadioPacket(int size)
	{
		payload = new byte[size + ITinyOSRadioProtocolManager.DATA_OFFSET];
	}

	private class TinyOSInputStream extends InputStream
	{
		public int read()
		{
			if (payloadIndex >= endOfDataIndex)
			{
				return -1;
			}
			else
			{
				return payload[payloadIndex++] & 0xFF;
			}
		}

		public void close() throws IOException
		{
			payload = null;
			super.close();
		}

		public int available()
		{
			return endOfDataIndex - payloadIndex;
		}

		public void reset()
		{
			payloadIndex = ITinyOSRadioProtocolManager.DATA_OFFSET;
		}
	}

	private class TinyOSOutputStream extends OutputStream
	{
		public void write(int b)
		{
			if (endOfDataIndex >= payload.length)
			{
				throw new IndexOutOfBoundsException("TinyOSData packet is full");
			}
			payload[endOfDataIndex++] = (byte) b;
		}

		public void close() throws IOException
		{
			payload = null;
			super.close();
		}
	}
}
