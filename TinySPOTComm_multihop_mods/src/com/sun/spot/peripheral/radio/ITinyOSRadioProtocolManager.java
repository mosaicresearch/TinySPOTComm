package com.sun.spot.peripheral.radio;

/**
 * @author Daniel van den Akker
 * This interface provides access to TinyOSRadioProtocolManager 
 */
public interface ITinyOSRadioProtocolManager extends IRadioProtocolManager
{
	/**
	 * The offset into data buffers at which data starts
	 */
	final int DATA_OFFSET = PORT_OFFSET + 1;
	/**
	 * identifies server connections
	 */
	final int SERVER = 2;
	/**
	 * identifies broadcast connections
	 */
	final int BROADCAST = 3;

	/**
	 * Register a server connection
	 * 
	 * @param portNo port number to communicate over
	 * @return resultant ConnectionID
	 */
	ConnectionID addServerConnection(byte portNo);

	/**
	 * Register a broadcast connection
	 * 
	 * @param portNo port number to communicate over
	 * @return resultant ConnectionID
	 */
	ConnectionID addBroadcastConnection(byte portNo);
}
