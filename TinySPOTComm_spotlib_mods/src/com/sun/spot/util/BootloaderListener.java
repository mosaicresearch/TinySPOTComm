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

package com.sun.spot.util;

import com.sun.spot.service.BootloaderListenerService;

import com.sun.spot.service.IBootloaderListener;

/**
 * Simple class to listen to the serial input over the USB connection and
 * pass control to the bootloader. Means you do not have to push the reset
 * button on the SPOT when downloading new code.
 * <p>
 * To use (simplest way):
 * <pre>
 *     new BootloaderListener().start();
 * </pre>
 * the BootloaderListener will exit the VM when a bootloader command is detected.
 * <p>
 * If you want to take your own action:
 * <pre>
 * new BootloaderListener(myCallbackObject).start();
 * </pre>
 * where myCallbackObject implements {@link com.sun.spot.util.IBootloaderListenerCallback IBootloaderListenerCallback}. 
 *<p>
 * You can cancel the bootloader listener by calling cancel()
 * 
 * @author Ron Goldman / Syntropy
 * @deprecated Please use the new BootloaderListenerService directly.
 */
public class BootloaderListener {   // Used to monitor the USB serial line

    private BootloaderListenerService bls;

    public BootloaderListener() {
        bls = BootloaderListenerService.getInstance();
    }
    
    public BootloaderListener(final IBootloaderListenerCallback callback) {
        bls = BootloaderListenerService.getInstance();
        bls.addBootloaderListener(new IBootloaderListener() {
            public void prepareToExit() {
                callback.prepareToExit();
            }
        });
    }

    public void start() {
        bls.start();
    }

    /**
     * Cleanup after ourself and stop running.
     */
    public void cancel() {
    	bls.stop();
    }

}        
