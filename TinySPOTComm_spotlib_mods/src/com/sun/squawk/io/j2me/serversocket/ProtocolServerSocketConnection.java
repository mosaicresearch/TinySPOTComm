package com.sun.squawk.io.j2me.serversocket;

import java.io.DataInputStream;
import java.io.IOException;

import javax.microedition.io.StreamConnection;

import com.sun.spot.io.j2me.socket.ProxyInitializer;
import com.sun.spot.io.j2me.socket.SocketConnection;
import com.sun.spot.io.j2me.socket.SocketProtocolInputStream;
import com.sun.spot.io.j2me.socket.SocketProtocolOutputStream;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.util.IEEEAddress;
import com.sun.squawk.io.j2me.socket.Protocol;

public class ProtocolServerSocketConnection extends SocketConnection {
	protected ProxyInitializer initializer;
	protected boolean timeouts;
	protected DataInputStream acceptStream;

	public ProtocolServerSocketConnection(ProxyInitializer initializer, boolean timeouts) throws IllegalArgumentException, IOException {
		super(initializer, timeouts);
		this.initializer = initializer;
		this.timeouts = timeouts;
		acceptStream = new DataInputStream(new SocketProtocolInputStream(conn.openInputStream()));
		confirmStream = new SocketProtocolOutputStream(conn.openOutputStream());
	}

	public StreamConnection acceptAndOpen() throws IOException {
		// Read in port to connect a socket to
        int radioPort;
        long basestationAddress;
        while (true) {
    	    try {
        		radioPort = acceptStream.readInt();
        		basestationAddress = acceptStream.readLong();
        		break;
    	    } catch (TimeoutException e) {
    	    }
        }
		String basestationAddressString = IEEEAddress.toDottedHex(basestationAddress);
		SocketConnection socketConnection = new SocketConnection(basestationAddressString, radioPort, true, confirmStream);
		StreamConnection socketProtocol = new Protocol(socketConnection);
		return socketProtocol;
    }

    /**
     * Disconnect and close the SocketConnection. 
     * @throws IOException
     */
    protected void disconnect() throws IOException {
        if (connected) {
            if(acceptStream != null){
                try {acceptStream.close();} catch (IOException e) {};
                acceptStream = null;                
            }
            if(confirmStream != null){
                try {confirmStream.close();} catch (IOException e) {};
                confirmStream = null;                
            }
            super.disconnect();
        }
    }

}
