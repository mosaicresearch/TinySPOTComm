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

package com.sun.squawk.io.j2me.serversocket;

import java.io.IOException;

import javax.microedition.io.Connection;
import javax.microedition.io.ServerSocketConnection;
import javax.microedition.io.StreamConnection;

import com.sun.spot.io.j2me.socket.ProxyInitializer;
import com.sun.squawk.io.ConnectionBase;

/**
 * Support for socket connections on device. This allows a generic stream connection with an external host (on a TCP network).
 * This must be used in conjunction with a SocketProxy on a host computer.
 *
 * @author  Martin Morissette
 */
public class Protocol extends ConnectionBase implements ServerSocketConnection {

    private ProtocolServerSocketConnection conn;
    protected int port;
    
    /**
     * Open the connection
     * 
     * @param name <name or IP number>:<port number>
     * @throws IOException 
     */
     public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException  {

        if (protocol == null || protocol.length()==0) {
            throw new IllegalArgumentException("Protocol cannot be null or empty");
        }

        if (name == null || name.length()==0) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        
        if(name.charAt(0) != '/' || name.charAt(1) != '/') {
            throw new IllegalArgumentException("Protocol must start with \"//\" " + name);
        }

        name = name.substring(2);

        /* Host name or IP number */
        String nameOrIP;

        /* Look for the : */
        int colon = name.indexOf(':');

        if (colon == -1) {
            throw new IllegalArgumentException("Bad protocol specification in " + name);
        }

        /* Strip off the protocol name */
        nameOrIP = name.substring(0, colon);

        if (nameOrIP.length() != 0) {
            /*
             * If the open string is "socket://:nnnn" then we regard this as
             * "serversocket://:nnnn"
             */
            throw new RuntimeException("Seemed to have specified a non-server socket");
        }

        try {
            /* Get the port number */
            port = Integer.parseInt(name.substring(colon + 1));
        } catch (NumberFormatException x) {
            throw new IllegalArgumentException("Invalid port number for socket connection at " + name);
        }

        conn = new ProtocolServerSocketConnection(new ProxyInitializer(nameOrIP,String.valueOf(port)), false);

        return this;

    }

    /**
     * Close the connection.
     *
     * @exception  IOException  if an I/O error occurs when closing the
     *                          connection.
     */
    public void close() throws IOException {
        conn.close();
    }

	public StreamConnection acceptAndOpen() throws IOException {
	    return conn.acceptAndOpen();
    }

	public String getLocalAddress() throws IOException {
	    return "127.0.0.1";
    }

	public int getLocalPort() throws IOException {
	    return port;
    }

}
