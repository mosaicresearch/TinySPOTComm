/*
 * Copyright 2010 Oracle. All Rights Reserved.
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
 * Please contact Oracle, 16 Network Circle, Menlo Park, CA 94025 or
 * visit www.oracle.com if you need additional information or have
 * any questions.
 */

package com.sun.spot.service;

import com.sun.spot.resources.Resources;
import com.sun.squawk.CrossIsolateThread;
import com.sun.squawk.Isolate;
import com.sun.squawk.VM;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;

/**
 * Simple service to listen to the serial input over the USB connection and
 * pass control to the bootloader. Means you do not have to push the reset
 * button on the SPOT when downloading new code.
 * <p>
 * To use (simplest way):
 * <pre>
 *     BootloaderListenerService.getInstance().start();
 * </pre>
 * the BootloaderListenerService will exit the VM when a bootloader command is detected.
 * <p>
 * If you want to take your own action:
 * <pre>
 *     BootloaderListenerService bls = BootloaderListenerService.getInstance();
 *     bls.start();
 *     bls.addBootloaderListener(myCallbackObject);
 * </pre>
 * where myCallbackObject implements {@link com.sun.spot.service.IBootloaderListener IBootloaderListener}.
 *<p>
 * You can cancel the bootloader listener service by calling:
 * <pre>
 *     BootloaderListenerService.getInstance().stop();
 * </pre>
 * 
 * @author Ron Goldman
 */
public class BootloaderListenerService extends BasicService implements Runnable {   // Used to monitor the USB serial line

    private int status = STOPPED;
    private InputStream in;
    private Vector listeners = new Vector();
    private Hashtable isolates = new Hashtable();

    public static BootloaderListenerService getInstance() {
        BootloaderListenerService bls = (BootloaderListenerService) Resources.lookup(BootloaderListenerService.class);
        if (bls == null) {
            bls = new BootloaderListenerService();
            bls.addTag("service=" + bls.getServiceName());
            Resources.add(bls);
        }
        return bls;
    }

    public synchronized void addBootloaderListener(final IBootloaderListener who) {
        if (!listeners.contains(who)) {
            listeners.addElement(who);
            isolates.put(who, Isolate.currentIsolate());
        }
    }

    /**
     * Removes the specified switch listener so that it no longer receives
     * callbacks from this switch. This method performs no function, nor does
     * it throw an exception, if the listener specified by the argument was not
     * previously added to this switch.
     *
     * @param who the switch listener to remove.
     */
    public synchronized void removeBootloaderListener(IBootloaderListener who) {
        if (listeners.removeElement(who)) {
            isolates.remove(who);
        }
    }

    /**
     * Returns an array of all the Bootloader listeners registered.
     *
     * @return all of the BootloaderListeners or an empty array if none are currently registered.
     */
    public IBootloaderListener[] getBootloaderListeners() {
        IBootloaderListener[] list = new IBootloaderListener[listeners.size()];
        for (int i = 0; i < listeners.size(); i++) {
            list[i] = (IBootloaderListener) listeners.elementAt(i);
        }
        return list;
    }

    private void runCallbacks() {
        for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
            final IBootloaderListener who = (IBootloaderListener) e.nextElement();
            Isolate iso = (Isolate) isolates.get(who);
            if (iso != null || !iso.isExited()) {
                // run in proper context
                new CrossIsolateThread(iso, "Bootloader Listener") {
                    public void run() {
                        who.prepareToExit();
                    }
                }.start();
            }
        }
    }

    /**
     * Loop reading characters sent over USB connection and dispatch to bootloader when requested.
     */
    public void run() {
        try {
            in = Connector.openInputStream("serial://usb");

            while (status == RUNNING) {
                char c = (char) in.read();
                if ('A' <= c && c <= 'P') {
                    runCallbacks();
                    System.out.println("Exiting - detected bootloader command");
                    VM.stopVM(0);         // return control to bootloader
                }
            }
        } catch (IOException ex) {
            System.err.println("Exception while listening to serial line: " + ex);
        }
    }

    /**
     * Start the BootloaderListener service, if not already running.
     *
     * @return true if the service was successfully started
     */
    public boolean start() {
        if (status == STOPPED) {
            status = RUNNING;
            Thread th = new Thread(this, "BootloaderListener thread");
            VM.setAsDaemonThread(th);
            th.start();
            return true;
        }
        return false;   // already running
    }

    /**
     * Stop the heartbeat service, if it is currently running.
     *
     * @return true if the service was successfully stopped
     */
    public boolean stop() {
        if (status == RUNNING) {
            status = STOPPED;
            try {
                in.close();
            } catch (IOException ex) {
                // ignore any exceptions
            }
            return true;
        }
        return false;
    }

    /**
     * Same as calling stop().
     */
    public boolean pause() {
        return stop();
    }

    /**
     * Same as calling start().
     */
    public boolean resume() {
        return start();
    }

    /**
     * Return the current status of the BootloaderListener service.
     *
     * @return the current status of this service, e.g. STOPPED or RUNNING
     */
    public int getStatus() {
        return status;
    }

    /**
     * Return whether the BootloaderListener service is currently running.
     *
     * @return true if the BootloaderListener service is currently running
     */
    public boolean isRunning() {
        return status == RUNNING;
    }

    /**
     * Return the name of the BootloaderListener service.
     *
     * @return "BootloaderListener" the name of this service
     */
    public String getServiceName() {
        return "BootloaderListener";
    }
}
