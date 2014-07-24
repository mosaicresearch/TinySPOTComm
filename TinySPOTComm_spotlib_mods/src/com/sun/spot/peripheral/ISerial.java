/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.peripheral;

import com.sun.spot.resources.IResource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.StreamConnection;

/**
 * Interface for Serial devices
 */
public interface ISerial extends IResource, StreamConnection {

    /**
     * Set the USART line parameteres.
     * @param baudrate baud speed to set module to
     * @param databits supported values are 5,6,7,8
     * @param parity supported options are none, odd, even, space & mark
     * @param stopbits supported options are 1, 1.5 or 2
     */
    public void setUSARTParams(int baudrate, int databits, String parity, float stopbits) throws IOException;
    

	public InputStream openInputStream() throws IOException;

	public OutputStream openOutputStream() throws IOException;

    public void closeInputStream() throws IOException;

    public void closeOutputStream() throws IOException;

    public int read() throws IOException;

    public int read(byte b[], int off, int len) throws IOException;

    public int available() throws IOException;

    public void write(int b) throws IOException;

    public void write(byte b[], int off, int len) throws IOException;
}
