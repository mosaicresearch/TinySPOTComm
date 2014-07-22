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

package com.sun.spot.peripheral.ota;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.StreamConnection;

import com.sun.spot.flashmanagement.FlashFile;
import com.sun.spot.io.j2me.remoteprinting.IRemotePrintManager;
import com.sun.spot.peripheral.ChannelBusyException;
import com.sun.spot.peripheral.ConfigPage;
import com.sun.spot.peripheral.IPowerController;
import com.sun.spot.peripheral.IBattery;
import com.sun.spot.peripheral.IRadioControl;
import com.sun.spot.peripheral.ISpot;
import com.sun.spot.peripheral.ITimeoutableConnection;
import com.sun.spot.peripheral.IUSBPowerDaemon;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.radio.RadioPolicy;
import com.sun.spot.service.IService;
import com.sun.spot.util.CrcOutputStream;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

/**
 * This class monitors radiogram communications on port number
 * 8, and responds to commands received. These commands allow flashing the
 * Spot's config page and/or applications remotely, retrieving the config page
 * contents, and restarting the Spot.<br>
 * <br>
 * Applications should never need to create an OTACommandServer explicitly.
 * OTA is enabled or disabled for a SPOT using the ant command line facility.
 * <br>
 * To get access to the OTACommandServer use Spot.getInstance().getOTACommandServer()
 * <br>
 * See {@link IOTACommandServerListener} if you want to run an application in a
 * separate thread concurrently with OTACommandServer, and you need to respond
 * (for example, suspending) when flash operations start.<br>
 * <br>
 */
public class OTACommandServer implements Runnable, ISpotAdminConstants, IOTACommandServer {

    private static final int MAX_HELLO_DELAY = 100;
    private static final int MAX_HELLO_RETRIES = 3;
    private static final int CRC_STREAM_BLOCK_SIZE = 2048;
    private static OTACommandServer theInstance = null;
    private DatagramConnection radiogramConnection;
    private Datagram receivedCommandRadiogram;
    private Datagram sendRadiogram;
    private Vector listeners = new Vector();
    private IPowerController powerController;
    private IBattery battery;
    private IUSBPowerDaemon usbPowerDaemon;
    private ISpot spot;
    private boolean suspended = false;
    private IRemotePrintManager remotePrintManager;
    private IOTACommandProcessor session;
    private int libraryHash;
    private Random random;
    private int status = STOPPED;
    private Thread thread = null;

    /**
     * Startup the OTACommandServer on a SPOT listening for OTA connections.
     * 
     * @param args ignored
     */
    public static void main(String[] args) {
        IOTACommandServer ota = getInstance();
        if (Utils.isOptionSelected("spot.ota.enable", false)) {
            ota.start();
        }
    }

    /**
     * @return Answer the singleton instance of this class
     * @throws IOException
     */
    public synchronized static IOTACommandServer getInstance() {
        if (theInstance == null) {
            theInstance = new OTACommandServer();
            theInstance.initialize();
        }
        return theInstance;
    }

    private OTACommandServer() {
    }

    /**
     * Attach a listener to be notified of the start and stop of flash
     * operations.
     * 
     * @param sml the listener
     */
    public void addListener(IOTACommandServerListener sml) {
        listeners.addElement(sml);
        if (session != null) {
            session.addListener(sml);
        }
    }

    /**
     * Answer the IEEE address of the sender of the last command received.
     * 
     * @return -- the address
     */
    public String getBaseStationAddress() {
        if (session != null) {
            return session.getBasestationAddress().asDottedHex();
        } else {
            return null;
        }
    }

    /**
     * @return Returns true if the server has been suspended by software.
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * @param suspended Suspends or resumes the server (it is initially running).
     */
    public void setSuspended(boolean suspended) {
        if (status == RUNNING) {
            this.suspended = suspended;
            if (session != null) {
                session.setSuspended(suspended);
            }
            if (suspended) {
                status = PAUSED;
            }
        }
    }

    /**
     * @return The time when the server last received a message from the host
     */
    public Date timeOfLastMessageFromHost() {
        if (session != null) {
            return session.timeOfLastMessageFromHost();
        } else {
            return null;
        }
    }

    /**
     * Should not be invoked from user code - call {@link #initialize()} instead.
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
        status = RUNNING;
        while (status == RUNNING) {
            try {
                String cmd = getCommand();
                processCommand(cmd);
            } catch (Exception e) {
                if (isRunning()) {
                    System.err.println("[OTACommandServer] Got exception handling session request: ");
                    e.printStackTrace();
                }
            }
        }
        status = STOPPED;
    }

    /*
     * Package visible for testing support 
     */
    OTACommandServer(ISpot spot, IUSBPowerDaemon usbPowerDaemon, IPowerController controller, IBattery battery, DatagramConnection connection, Datagram sendRadiogram, Datagram receiveRadiogram, int libraryHash) {
        radiogramConnection = connection;
        this.sendRadiogram = sendRadiogram;
        this.receivedCommandRadiogram = receiveRadiogram;
        this.spot = spot;
        this.powerController = controller;
        this.battery = battery;
        this.usbPowerDaemon = usbPowerDaemon;
        this.libraryHash = libraryHash;
        random = new Random();
    }

    /*
     * Package visibility to aid testing 
     */
    void processCommand(String cmd) throws IOException {
        if (cmd.equals(START_OTA_SESSION_CMD)) {
            IEEEAddress remoteAddress = new IEEEAddress(receivedCommandRadiogram.getAddress());
            if (session != null && session.isAlive()) {
                if (!remoteAddress.equals(session.getBasestationAddress())) {
                    sendErrorDetails("[OTACommandServer] Already has a session with " + remoteAddress);
                } else {
                    session.closedown();
                    startSession(remoteAddress);
                }
            } else {
                startSession(remoteAddress);
            }
        } else if (cmd.equals(HELLO_CMD)) {
            sendHelloResponse();
        }
    }

    /**
     * Start up a OTACommandServer. Sets up its radio connections and then
     * spawns a thread to respond to remote requests.
     */
    private void initialize() {
        spot = Spot.getInstance();
        remotePrintManager = spot.getRemotePrintManager();
        powerController = spot.getPowerController();
        try {
            // Note that the getBattery() method can throw
            // a runtime exception if the SPOT has an old
            // power controller or hardware
            battery = powerController.getBattery();
        } catch (Exception e) {
            
        }
        usbPowerDaemon = spot.getUsbPowerDaemon();
        try {
            libraryHash = Integer.parseInt(new FlashFile(ConfigPage.LIBRARY_URI).getComment(), 16);
        } catch (IOException ex) {
            System.err.println("[OTACommandServer] Got IOException while trying to get library hash " + ex.getMessage());
            ex.printStackTrace();
            libraryHash = -1;
        }
    }

    private void sendHelloResponse() throws IOException {
        Utils.log("[OTACommandServer] Processing hello request from " + receivedCommandRadiogram.getAddress());
        sendRadiogram.reset();
        sendRadiogram.setAddress(receivedCommandRadiogram);
        sendRadiogram.writeByte(HELLO_COMMAND_VERSION);
        sendRadiogram.writeByte(HELLO_COMMAND_MINOR_VERSION);
        sendRadiogram.writeUTF(System.getProperty("spot.sdk.version"));
        sendRadiogram.writeInt(libraryHash);
        byte[] publicKey = spot.getPublicKey();
        for (int i = 1; i <= 3; i++) {
            sendRadiogram.writeByte(i >= publicKey.length ? 0 : publicKey[i]);
        }
        sendRadiogram.writeLong(System.currentTimeMillis());
        sendRadiogram.writeByte(HARDWARE_MAJOR_REV_ESPOT);
        sendRadiogram.writeByte(spot.getHardwareType());
        boolean externallyPowered = usbPowerDaemon.isUsbPowered();
        sendRadiogram.writeShort((externallyPowered ? 1 << 15 : 0) | powerController.getVbatt());
        String name = System.getProperty("name");
        if (name == null) {
            name = "";
        } else if (name.length() > 20) {
            name = name.substring(0, 20);
        }
        sendRadiogram.writeUTF(name);
        sendRadiogram.writeBoolean(publicKey.length != 0);
        
        byte battStatus = 127; // choose an invalid value
        if (battery != null) {
            battStatus = (byte) (battery.getBatteryLevel() |
                ((battery.getState() == IBattery.CHARGING) ? 128 : 0));
        }
        sendRadiogram.writeByte(battStatus);
        String[] coordinates = new String[3];
        coordinates[0] = System.getProperty("spot.latitude");
        coordinates[1] = System.getProperty("spot.longitude");
        coordinates[2] = System.getProperty("spot.altitude");
        if ((coordinates[0] != null) ||
                (coordinates[1] != null) ||
                (coordinates[2] != null)) {
            // 1 indicates uncompressed latitude, longitude and 
            // altitude expressed as floats.
            sendRadiogram.writeByte(1);
            float tmp = (float) 0.0;
            for (int i = 0; i < coordinates.length; i++) {
                try {
                    tmp = (float) 0.0;
                    tmp = Float.parseFloat(coordinates[i]);
                } catch (Exception e) {
                    System.err.println("Trouble parsing " +
                            "geoCoordinate " + coordinates[i]);
                }
                sendRadiogram.writeFloat(tmp);
            }
        } else {
            coordinates[0] = System.getProperty("spot.x");
            coordinates[1] = System.getProperty("spot.y");
            coordinates[2] = System.getProperty("spot.z");
            if ((coordinates[0] != null) ||
                    (coordinates[1] != null) ||
                    (coordinates[2] != null)) {
                // 2 indicates cartesian coordinates expressed as ints
                sendRadiogram.writeByte(2);
                float tmp = -1;
                for (int i = 0; i < coordinates.length; i++) {
                    try {
                        tmp = Float.MIN_VALUE;
                        tmp = Float.parseFloat(coordinates[i]);
                    } catch (Exception e) {
                        System.err.println("Trouble parsing " +
                                "cartesian coordinate " + coordinates[i]);
                    }
                    sendRadiogram.writeFloat(tmp);
                } 
            } else {
                // this byte says how location is encoded, 0 => no location
                sendRadiogram.writeByte(0);
            }
        }

        for (int i = 0; i < MAX_HELLO_RETRIES; i++) {
            Utils.sleep(Math.abs(random.nextInt(MAX_HELLO_DELAY)));
            try {
                radiogramConnection.send(sendRadiogram);
                break;
            } catch (ChannelBusyException e) {
            }
        }
    }

    private void startSession(IEEEAddress remoteAddress) throws IOException {
        session = new OTACommandProcessor(remoteAddress, powerController, spot, remotePrintManager);
        StreamConnection conn = (StreamConnection) Connector.open("radiostream://" + remoteAddress + ":" + PORT);
        ((ITimeoutableConnection) conn).setTimeout(20000);
        ((IRadioControl) conn).setRadioPolicy(RadioPolicy.AUTOMATIC);
        CrcOutputStream crcOutputStream = new CrcOutputStream(conn.openOutputStream(), conn.openInputStream(), CRC_STREAM_BLOCK_SIZE);
        DataOutputStream dataOutputStream = new DataOutputStream(crcOutputStream);
        DataInputStream dataInputStream = new DataInputStream(crcOutputStream.getInputStream());
        initializeSession(conn, dataInputStream, dataOutputStream);
    }

    void initializeSession(StreamConnection conn, DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {
        Enumeration enumeration = listeners.elements();
        while (enumeration.hasMoreElements()) {
            session.addListener((IOTACommandServerListener) enumeration.nextElement());
        }
        session.setSuspended(suspended);
        session.initialize(dataInputStream, dataOutputStream, (IRadioControl) conn);
    }

    private void sendErrorDetails(String msg) throws IOException {
        sendUTF(BOOTLOADER_CMD_HEADER + ":E " + msg.substring(0, Math.min(70, msg.length())));
    }

    private void sendUTF(String stringToSend) throws IOException {
        DataOutputStream dataOutputStream = Connector.openDataOutputStream("radiostream://" + receivedCommandRadiogram.getAddress() + ":" + PORT);
        dataOutputStream.writeUTF(stringToSend);
        dataOutputStream.close();
    }

    private String getCommand() throws IOException {
        radiogramConnection.receive(receivedCommandRadiogram);
        return receivedCommandRadiogram.readUTF();
    }

    /*
     * Testing support
     */
    void setSession(IOTACommandProcessor session) {
        this.session = session;
    }

    void setStatus(int status) {
        this.status = status;
    }
    
    /*
     * IService interface methods
     */
    
    /**
     * Start the service, and return whether successful.
     *
     * @return true if the service was successfully started
     */
    public boolean start() {
        if (!isRunning()) {
            Utils.log("[OTACommandServer] Starting");
            try {
                radiogramConnection = (DatagramConnection) Connector.open("radiogram://:" + PORT);
                ((IRadioControl) radiogramConnection).setRadioPolicy(RadioPolicy.AUTOMATIC);
                receivedCommandRadiogram = radiogramConnection.newDatagram(radiogramConnection.getMaximumLength());
                sendRadiogram = radiogramConnection.newDatagram(radiogramConnection.getMaximumLength());
                if (random == null) {
                    random = new Random(Spot.getInstance().getRadioPolicyManager().getIEEEAddress());
                }
            } catch (IOException ex) {
                System.err.println("[OTACommandServer] Got IOException while starting OTACommandServer " + ex.getMessage());
                ex.printStackTrace();
                return false;
            }
            thread = new Thread(this, "OTACommandServer");
            thread.setPriority(Thread.MAX_PRIORITY - 1);
            thread.start();
            status = STARTING;
            return true;
        } else {
            return false;
        }

    }

    /**
     * Stop the service, and return whether successful.
     * Stops all running threads. Closes any open IO connections.
     *
     * @return true if the service was successfully stopped
     */
    public boolean stop() {
        if (status == RUNNING || status == PAUSED) {
            Utils.log("[OTACommandServer] Stopping");
            try {
                status = STOPPING;
                if (session != null && session.isAlive()) {
                    session.closedown();
                }
                radiogramConnection.close();
                thread.interrupt();
                return true;
            } catch (IOException ex) {
                System.err.println("[OTACommandServer] Got IOException while stopping OTACommandServer " + ex.getMessage());
                ex.printStackTrace();
                return false;
            }
        } else {
            return status == STOPPED;
        }
    }

    /**
     * Pause the service, and return whether successful.
     * Preserve any current state, but do not handle new requests.
     * Any running threads should block or sleep. 
     * Any open IO connections may be kept open.
     *
     * If there is no particular state associated with this service
     * then pause() can be implemented by calling stop().
     *
     * @return true if the service was successfully paused
     */
    public boolean pause() {
        if (isRunning()) {
            status = PAUSED;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Resume the service, and return whether successful.
     * Picks up from state when service was paused.
     *
     * If there was no particular state associated with this service
     * then resume() can be implemented by calling start().
     *
     * @return true if the service was successfully resumed
     */
    public boolean resume() {
        if (status == PAUSED) {
            setSuspended(false);
            status = RUNNING;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Return the current status of this service.
     *
     * @return the current status of this service, e.g. STOPPED, STARTING, RUNNING, PAUSED, STOPPING, etc.
     */
    public int getStatus() {
        return status;
    }
    
    /**
     * Return whether the service is currently running.
     *
     * @return true if the service is currently running
     */
    public boolean isRunning() {
        return status == RUNNING;
    }

    /**
     * Return the name of this service.
     *
     * @return the name of this service
     */
    public String getServiceName() {
        return "OTA Command Server";
    }

    /**
     * Assign a name to this service. For some fixed services this may not apply and
     * any new name will just be ignored.
     *
     * @param who the name for this service
     */
    public void setServiceName(String who) {
        // ignore new name
    }

    /**
     * Return whether service is started automatically on reboot.
     * This may not apply to some services and for those services it will always return false.
     *
     * @return true if the service is started automatically on reboot
     */
    public boolean getEnabled() {
        return Utils.isOptionSelected("spot.ota.enable", false);
    }

    /**
     * Enable/disable whether service is started automatically. 
     * This may not apply to some services and calls to setEnabled() may be ignored.
     *
     * @param enable true if the service should be started automatically on reboot
     */
    public void setEnabled(boolean enable) {
        Spot.getInstance().setPersistentProperty("spot.ota.enable", (enable ? "true" : "false"));
    }
}
