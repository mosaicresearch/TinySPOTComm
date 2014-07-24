/*
 * Copyright 2006-2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.io.j2me.datagram;

import com.sun.midp.io.j2me.multicast.DatagramObject;
import com.sun.spot.io.j2me.socket.*;
import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.StreamConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;


/**
 * <p>This class provides the necessary implementation for a multicast datagram connection.</p>
 * <p>A SocketProxy must be running on the desktop in order to properly establish a connection.</p>
 * 
 * <p>
 * The MulticastConnection uses the following Manifest properties to establish the connection:<br>
 * {@value com.sun.spot.io.j2me.socket.SocketConnection#SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY} : IEEE address of the base station where the SocketProxy is running (can also be set as an argument to the VM)<br>
 * {@value com.sun.spot.io.j2me.socket.SocketConnection#SOCKET_PROXY_BASE_STATION_PORT_MANIFEST_PROPERTY} : Radiogram port to connect to, where the SocketProxy is running. (can also be set as an argument to the VM)
 * </p>
 * @author Michael Lagally
 * adapted SocketConnection by:
 * @author Martin Morissette
 */
public class DatagramConnectionImpl {

    public static final int MAX_DATAGRAM_LENGTH = 65535;
    public static final int NOMINAL_DATAGRAM_LENGTH = 1024;

    public static int getIpNumber(String sHost) {
        String s = new String(sHost);
        String s1 = s.substring(0, s.indexOf('.'));
        s = s.substring(s.indexOf('.') + 1);
        String s2 = s.substring(0, s.indexOf('.'));
        s = s.substring(s.indexOf('.') + 1);
        String s3 = s.substring(0, s.indexOf('.'));
        String s4 = s.substring(s.indexOf('.') + 1);
        // System.out.println (s1+"-"+s2+"-"+s3+"-"+s4);
        int ipNumber = (Integer.parseInt(s1) << 24)+
                (Integer.parseInt(s2)<<16)+
                (Integer.parseInt(s3)<<8)+
                (Integer.parseInt(s4));
        return ipNumber;
    }
    
    private StreamConnection conn;
    private DataOutputStream out = null;
    private DataInputStream in = null;
    
    private int localPort = 0;

    int opens = 0;

    private boolean connected = false;

    /**
     * Create a SocketConnection object.
     * 
     * @param initializer
     *            Initializer string to send the proxy to init the connection.
     * @param timeouts
     *            set to true to use timeouts
     * @throws IOException
     *             when unable to establish the connection with the proxy
     * @throws IllegalArgumentException
     *             when the property
     *             {@value com.sun.spot.io.j2me.socket.SocketConnection#SOCKET_PROXY_BASE_STATION_ADDRESS_MANIFEST_PROPERTY}
     *             is not set in the Manifest file or passed as an argument to
     *             the VM.
     */
    public DatagramConnectionImpl(ProxyInitializer initializer, boolean timeouts) throws IllegalArgumentException, IOException {
        SocketConnection.doSocketProxyRequest(SocketConnection.PACKET_TYPE_MULTICAST_PORT_REQUEST, initializer.toString(), timeouts, null, this);
    }

    public void init(int port, String basestationAddress, boolean timeouts) throws IllegalArgumentException, IOException {
        localPort = port;
        conn = (StreamConnection) Connector.open("radiostream://" + basestationAddress + ":" + port, Connector.READ_WRITE, timeouts);
        in = conn.openDataInputStream();
        out = conn.openDataOutputStream();
        connected = true;
        opens++;
    }

    public int getMaximumLength() {
        return MAX_DATAGRAM_LENGTH;
    }

    public int getNominalLength() {
        return NOMINAL_DATAGRAM_LENGTH;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getLocalAddress() {
        return "127.0.0.1";
    }

    /**
     * Receive data to the data buffer taking offet and length into account over
     * a raidostream connection. This method returns the ipNumber in the upper
     * 32 bits of the return value, in the upper 16 bits it returns the port
     * number and the lower 16 bits contain the number of bytes read.
     */

    public long receive(byte[] data, int offset, int length) {
        try {

            int ipNumber = in.readInt();
            int locPort = in.readInt();
            int size = in.readInt();
            // System.out.println ("In receive: size="+size);
            // System.out.println("receive: available="+in.available());

            // ipNumber = (int) ((res >> 32));
//            System.out.println("multicast::Protocol::receive::ipNumber is : "
//                    + ipNumber + "");
            // host = getHostByAddr(ipNumber).trim();
            // DEBUG1("multicast::Protocol::receive::host is : ", host + "");
            // port = (int) ((res >> 16)) & 0xffff;
//            System.out.println("multicast::Protocol::receive::port is : "
//                    + locPort + "");
            // addr = "multicast://" + host + ":" + port;
            //
            // if (dgram instanceof DatagramObject) {
            //
            // // save this data for sending back a message
            // DatagramObject dh = (DatagramObject) dgram;
            // dh.address = addr;
            // dh.ipNumber = ipNumber;
            // dh.port = port;
            // dh.host = host;
            // } else {
            // dgram.setAddress("multicast://" + host + ":" + port);
            // }
            // }

            // read full datagram
            byte buf[] = new byte[size];
            int off = 0;
            int nr = 0;
            do {
                nr = in.read(buf, off, size - off);
                off += nr;
            } while (off != size);

            System.arraycopy(buf, offset, data, offset, size - offset);

            // System.out.println ("bytes read: "+offset);
//            System.out
//                    .println("DatagramConnectionImpl.receive: Datagram received - Length: "
//                            + size
//                            + " Message:"
//                            + new String(data, offset, size - offset));
            return (((long) ipNumber) << 32) + (locPort << 16) + size;
        } catch (IOException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    /**
     * Send the ipNumber, locPort and the data contained in the data buffer
     * taking offet and length into account over a raidostream connection.
     */

    public int send(int ipNumber, int locPort, byte[] data, int offset,
            int length) {
        try {
            out.writeInt(ipNumber);
            out.writeInt(locPort);
            out.writeInt(length);
            out.write(data, offset, length);
            out.flush();
//            System.out
//                    .println("DatagramConnectionImpl.send: Datagram sent - Length: "
//                            + length);
        } catch (IOException ex) {
            ex.printStackTrace();
            return 0;
        }
        return length;
    }

    /**
     * Disconnect and close the SocketConnection.
     * 
     * @throws IOException
     */
    private void disconnect() throws IOException {
        if (connected) {
            if (in != null) {
                in.close();
                in = null;
            }

            if (out != null) {
                out.close();
                out = null;
            }

            conn.close();
            conn = null;
            connected = false;
        }
    }

    /**
     * Disconnect and close the SocketConnection.
     * 
     * @throws IOException
     */
    public void close() throws IOException {
//        System.out.println("DatagramConnectionImpl.close connected="
//                + connected + " opens=" + opens);
        if (connected) {
            if (--opens == 0) {
                disconnect();
            }
        }
    }

    public Datagram newDatagram(int size) {
        byte buf[] = new byte[size];
        DatagramObject d = new DatagramObject(buf, size);
        return d;
    }

    public Datagram newDatagram(byte[] buf, int size) {
        DatagramObject d = new DatagramObject(buf, size);
        return d;
    }

}
