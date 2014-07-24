/*
 * Copyright 2005-2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.io.j2me.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.StreamConnection;

import com.sun.spot.globals.SpotGlobals;
import com.sun.spot.io.j2me.datagram.DatagramConnectionImpl;
import com.sun.spot.peripheral.IMultipleHopConnection;
import com.sun.spot.peripheral.ITimeoutableConnection;
import com.sun.spot.peripheral.NoAckException;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.util.Utils;
import com.sun.squawk.Isolate;


/**
 * <p>This class provides the necessary implementation for a socket connection.</p> 
 * <p>A SocketProxy must be running on the desktop in order to properly establish a connection.</p>
 * 
 * <p>
 * The SocketConnection uses the following Manifest properties to establish the connection:<br>
 * {@value #SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY} : IEEE address of the base station where the SocketProxy is running (can also be set as an argument to the VM)<br>
 * {@value #SOCKET_PROXY_BASE_STATION_PORT_MANIFEST_PROPERTY} : Radiogram port to connect to, where the SocketProxy is running. (can also be set as an argument to the VM)
 * </p> 
 * @author Martin Morissette
 */
public class SocketConnection {
    public static final int SPOT_GLOBALS_FIRST_DO_SOCKET_KEY = 123456;

    public static final String SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY = "com.sun.spot.io.j2me.socket.SocketConnection-BaseStationAddress";
    public static final String SOCKET_PROXY_BASE_STATION_PORT_MANIFEST_PROPERTY = "com.sun.spot.io.j2me.socket.SocketConnection-BaseStationPort";
    public static final String SOCKET_PROXY_PROXY_DISCOVER_RADIUS_MANIFEST_PROPERTY = "com.sun.spot.io.j2me.socket.SocketConnection-ProxyDiscoveryRadius";

    public static final String DEFAULT_BASE_STATION_PORT = "10";
    
    public static final int PACKET_TYPE_KILL_REQUEST = 0;
    public static final int PACKET_TYPE_KILL_RESPONSE = 1;
    public static final int PACKET_TYPE_PORT_REQUEST = 2;
    public static final int PACKET_TYPE_PORT_RESPONSE = 3;
    public static final int PACKET_TYPE_MULTICAST_PORT_REQUEST = 4;
    public static final int PACKET_TYPE_MULTICAST_PORT_RESPONSE = 5;
    public static final int PACKET_TYPE_INET_GET_BY_NAME_REQUEST = 6;
    public static final int PACKET_TYPE_INET_GET_BY_NAME_RESPONSE = 7;
    public static final int PACKET_TYPE_INET_GETLOCALHOST_REQUEST = 8;
    public static final int PACKET_TYPE_INET_GETLOCALHOST_RESPONSE = 9;

    public static final Object DO_REQUEST_LOCK;
    protected static SocketConnection[] connections;
    
    protected StreamConnection conn;
    private SocketOutputStream out = null;
    private SocketInputStream in = null;
    protected String basestationAddress;
    protected int port;
    protected OutputStream confirmStream;
    
    protected int opens=0;
    
    protected boolean connected=false;

    static {
        DO_REQUEST_LOCK = new Object();
        connections = new SocketConnection[16];
    }
    
    /**
    *
    */
   public static String doSocketProxyRequest(int requestType, String string, boolean timeouts, SocketConnection socketConnection, DatagramConnectionImpl datagramConnectionImpl) throws IOException {
       synchronized (DO_REQUEST_LOCK) {
           return doSocketProxyRequestPrim(requestType, string, timeouts, socketConnection, datagramConnectionImpl);
       }
   }
   
    /**
     *
     */
    public static String doSocketProxyRequestPrim(int requestType, String string, boolean timeouts, SocketConnection socketConnection, DatagramConnectionImpl datagramConnectionImpl) throws IOException {
        synchronized (SpotGlobals.getMutex()) {
            if (SpotGlobals.getGlobal(SPOT_GLOBALS_FIRST_DO_SOCKET_KEY) == null) {
                SpotGlobals.setGlobal(SPOT_GLOBALS_FIRST_DO_SOCKET_KEY, new Object());
                String basestationAddress = Utils.getManifestProperty(SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY, null);
                if (basestationAddress != null) {
                    System.out.print("SocketConnection SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY=\"");
                    System.out.print(basestationAddress);
                    System.out.println("\"");
                }
                doSocketProxyRequestPrim(PACKET_TYPE_KILL_REQUEST, string, timeouts, socketConnection, datagramConnectionImpl);
            }
        }
        String basestationAddress = Utils.getManifestProperty(SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY, null);
        if (basestationAddress == null) {
            // Try to get the property from the Isolates properties
            basestationAddress = Isolate.currentIsolate().getProperty(SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY);
            if (basestationAddress == null) {
                basestationAddress = "broadcast";
            }
        }
        basestationAddress = basestationAddress.trim();

        /* Determine port number */
        String baseStationPort = Utils.getManifestProperty(SOCKET_PROXY_BASE_STATION_PORT_MANIFEST_PROPERTY, null);
        if (baseStationPort == null) {
            // Try to get the property from the Isolates properties
            baseStationPort = Isolate.currentIsolate().getProperty(SOCKET_PROXY_BASE_STATION_PORT_MANIFEST_PROPERTY);
            if (baseStationPort == null) {
                baseStationPort = DEFAULT_BASE_STATION_PORT;
            }
        }
        baseStationPort = baseStationPort.trim();

        // Open init connection with SocketProxy
        DatagramConnection sendConnection = null;
        DatagramConnection receiveConnection = null;
        Datagram receiveDatagram;
        try {
            final long timeout = 10000;
            sendConnection = (DatagramConnection) Connector.open("radiogram://" + basestationAddress + ":" + baseStationPort, Connector.READ_WRITE, true);
            Datagram sendDatagram = sendConnection.newDatagram(sendConnection.getMaximumLength());
            ((ITimeoutableConnection) sendConnection).setTimeout(timeout);
            String hopsString = Utils.getManifestProperty(SOCKET_PROXY_PROXY_DISCOVER_RADIUS_MANIFEST_PROPERTY, null);
            if (hopsString != null) {
                try {
                    hopsString = hopsString.trim();
                    int hops = Integer.parseInt(hopsString);
                    ((IMultipleHopConnection) sendConnection).setMaxBroadcastHops(hops);
                } catch (NumberFormatException e) {
                    System.err.print(SOCKET_PROXY_PROXY_DISCOVER_RADIUS_MANIFEST_PROPERTY);
                    System.err.print(" manifest property has invalid number:");
                    System.err.print(hopsString);
                }
            }
            if (basestationAddress.equals("broadcast")) {
                receiveConnection = (DatagramConnection) Connector.open("radiogram://:" + baseStationPort, Connector.READ_WRITE, true);
                ((ITimeoutableConnection) receiveConnection).setTimeout(timeout);
                receiveDatagram = receiveConnection.newDatagram(receiveConnection.getMaximumLength());
            } else {
                receiveConnection = sendConnection;
                receiveDatagram = sendDatagram;
            }


            // We force timeouts here for the initial connection with the proxy
            // so that we can determine if the proxy is reachable or not
            sendDatagram.writeByte(requestType);
            int responseType;
            switch (requestType) {
            case PACKET_TYPE_KILL_REQUEST:
                responseType = PACKET_TYPE_KILL_RESPONSE;
                break;
            case PACKET_TYPE_PORT_REQUEST:
                responseType = PACKET_TYPE_PORT_RESPONSE;
                sendDatagram.writeUTF(string.toString());
                break;
            case PACKET_TYPE_INET_GET_BY_NAME_REQUEST:
                responseType = PACKET_TYPE_INET_GET_BY_NAME_RESPONSE;
                sendDatagram.writeUTF(string);
                break;
            case PACKET_TYPE_INET_GETLOCALHOST_REQUEST:
                responseType = PACKET_TYPE_INET_GETLOCALHOST_RESPONSE;
                break;
            case PACKET_TYPE_MULTICAST_PORT_REQUEST:
                responseType = PACKET_TYPE_MULTICAST_PORT_RESPONSE;
                sendDatagram.writeUTF(string);
                break;
            default:
                throw new IOException("Invalid request type:" + requestType);
            }

            sendConnection.send(sendDatagram);

            // Rely on receive connection timeout to eventually end this loop
            while (true) {
                receiveConnection.receive(receiveDatagram);
                int response = receiveDatagram.readUnsignedByte();
                if (response == responseType)
                    break;
            }

            switch (responseType) {
            case PACKET_TYPE_KILL_RESPONSE:
                int openOnCount = receiveDatagram.readInt();
                if (openOnCount > 0) {
                    throw new IOException("Socket proxy shows " + openOnCount + " connections already open, restart socket proxy");
                }
                return null;
            case PACKET_TYPE_PORT_RESPONSE:
                int port = receiveDatagram.readInt();
                basestationAddress = receiveDatagram.getAddress();
                socketConnection.init(basestationAddress, port, timeouts);
                return null;
            case PACKET_TYPE_MULTICAST_PORT_RESPONSE:
                port = receiveDatagram.readInt();
                basestationAddress = receiveDatagram.getAddress();
                datagramConnectionImpl.init(port, basestationAddress, timeouts);
                return null;
            case PACKET_TYPE_INET_GET_BY_NAME_RESPONSE:
                String hostAddr = receiveDatagram.readUTF();
                return hostAddr;
            case PACKET_TYPE_INET_GETLOCALHOST_RESPONSE:
                String localhost = receiveDatagram.readUTF();
                return localhost;
            default:
                throw new IOException("Invalid response type:" + responseType);
            }
        } catch (TimeoutException te) {
            throw new IOException("unable to establish connection with socket proxy at address " + basestationAddress + " on port " + baseStationPort + " (timeout)" + ":" + te.getMessage());
        } catch (NoAckException nae) {
            throw new IOException("unable to establish connection with socket proxy at address " + basestationAddress + " on port " + baseStationPort + " (no ack)");
        } finally {
            if (sendConnection != null) {
                try {sendConnection.close();} catch (IOException e) {};
            }
            if (receiveConnection != null) {
                try {receiveConnection.close();} catch (IOException e) {};
            }
        }
    }

    /**
     * Perform a DNS lookup on the host over a radidostream connection.
     */
    public static String getAddressByHost(String host) throws IOException {
        String hostAddr = doSocketProxyRequest(PACKET_TYPE_INET_GET_BY_NAME_REQUEST, host, false, null, null);
        return hostAddr;
    }

    public static String getLocalHost() throws IOException {
        String localhost = doSocketProxyRequest(PACKET_TYPE_INET_GETLOCALHOST_REQUEST, null, false, null, null);
        return localhost;
    }
    
    /**
     * Create a SocketConnection object.
     * @param initializer Initializer string to send the proxy to init the connection.
     * @param timeouts set to true to use timeouts
     * @throws IOException when unable to establish the connection with the proxy 
     * @throws IllegalArgumentException when the property {@value #SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY} is not set in the Manifest file or passed as an argument to the VM. 
     */
    public SocketConnection(ProxyInitializer initializer, boolean timeouts) throws IllegalArgumentException, IOException {
        doSocketProxyRequest(PACKET_TYPE_PORT_REQUEST, initializer.toString(), timeouts, this, null);
    }

    public SocketConnection(String baseStationAddress, int radioPort, boolean timeouts, OutputStream confirmStream) throws IllegalArgumentException, IOException {
        init(baseStationAddress, radioPort, timeouts);
        this.confirmStream = confirmStream;
    }

    protected void init(String baseStationAddress, int radioPort, boolean timeouts) throws IOException {
        synchronized (DO_REQUEST_LOCK) {
            this.basestationAddress = baseStationAddress;
            this.port = radioPort;
            String toOpen = "radiostream://" + baseStationAddress + ":" + radioPort;
            // There is a case where the desktop closes connection, sends notification to SPOT, but application on SPOT
            // may not read from socket anymore and the CLOSE_CONNECTION is never read and processed.  So if desktop
            // gives us a port that it says is unused, then it knows better, try to close the one we have on our end
            int index = -1;
            for (int i=0; i < connections.length; i++) {
                SocketConnection entry = (SocketConnection) connections[i];
                if (entry == null) {
                    index = i;
                } else {
                    if (entry.connected) {
                        if (entry.basestationAddress.equals(basestationAddress) && entry.port == port) {
                            try {entry.disconnect();} catch (IOException e1) {};
                            connections[i] = null;
                            index = i;
                        }
                    } else {
                        connections[i] = null;
                        index = i;
                    }
                }
            }
            conn = (StreamConnection) Connector.open(toOpen, Connector.READ_WRITE, timeouts);
            if (index == -1) {
                SocketConnection[] newConnections = new SocketConnection[connections.length + 4];
                System.arraycopy(connections, 0, newConnections, 0, connections.length);
                index = connections.length;
                connections = newConnections;
            }
            connections[index] = this;
            connected=true;
            opens++;
        }
    }

    /**
     * Get a SocketInputStream object associated to this conneciton.
     * @return a SocketInputStream object associated to this conneciton. 
     * @throws IOException
     */
    public SocketInputStream getInputStream() throws IOException {
        if (in == null) {
            opens++;
            in = new SocketInputStream(conn.openInputStream());
            if (confirmStream != null) {
                confirmStream.write(1);
                confirmStream.flush();
            }
        }
        return in;
    }

    /**
     * Get a SocketOutputStream object associated to this conneciton.
     * @return a SocketOutputStream object associated to this conneciton.
     * @throws IOException
     */
    public SocketOutputStream getOutputStream() throws IOException {
        if (out == null) {
            opens++;
            out = new SocketOutputStream(conn.openOutputStream());
        }
        return out;
    }
    
    /**
     * Disconnect and close the SocketConnection. 
     * @throws IOException
     */
    protected void disconnect() throws IOException {
        if (connected) {
            
            if(in!=null){
                try {in.closeConnection();} catch (IOException e) {};
                in = null;                
            }
            
            if(out!=null){
                try {out.closeConnection();} catch (IOException e) {};
                out = null;                
            }

            try {conn.close();} catch (IOException e) {};
            conn = null;
            connected = false;
        }
    }

    /**
     * Disconnect and close the SocketConnection.
     * @throws IOException
     */
    public void close() throws IOException {
        if(connected){
            if (--opens == 0){
                disconnect();    
            }
        }
    }

    /**
     * Socket specific input stream.
     * @author Martin Morissette
     */
    public class SocketInputStream extends SocketProtocolInputStream {

        private boolean opened=false;
        
        public SocketInputStream(InputStream in) {
            super(in);
            opened = true;
        }
        
        public int read() throws IOException {
            if(!opened){
                throw new IOException("inputstream is closed");
            }            
            return super.read();
        }
        
        public void close() throws IOException {
            if(opened){
//                super.close();
                opened=false;
                if (--opens == 0){
                    disconnect();    
                }
            }
        }
        
        private void closeConnection() throws IOException{
            super.close();
        }
               
        protected void connectionClosedReceived() throws IOException {
            connectionClosedReceived = true;
            // Force closing of the stream since we got a notification from proxy that is closed its end
            opens = 1;
            close();
        }
        
    }
    
    /**
     * Socket specific output stream.
     * @author Martin Morissette
     *
     */
    private class SocketOutputStream extends SocketProtocolOutputStream {
        private boolean opened=false;
        
        public SocketOutputStream(OutputStream out) {
            super(out);
            opened=true;
        }
        
        public void write(int data) throws IOException {
            if(!opened){
                throw new IOException("outputstream is closed");
            }
            super.write(data);
        }
        
        public void flush() throws IOException {
            if(!opened){
                throw new IOException("outputstream is closed");
            }
            super.flush();
        }
        
        public void close() throws IOException {
            if(opened){
//                super.close();
                opened=false;
                if (--opens == 0){
                    disconnect();    
                }
            }
        }
        
        private void closeConnection() throws IOException{
            super.close();
        }
        
    }

}
