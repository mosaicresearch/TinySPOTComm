/*
 * Copyright 2006-2010 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.peripheral.radio;

import java.util.Random;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.resources.Resource;
import com.sun.spot.resources.transducers.ILed;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.PrettyPrint;
import com.sun.spot.util.Queue;
import com.sun.spot.util.Utils;
import com.sun.squawk.VM;

abstract class MACBase extends Resource implements I802_15_4_MAC, IProprietaryMAC {

    /* In a non-beaconed network, any value for this in the range 34 to 62 symbol periods should be ok.
     * The ACK should arrive after exactly 34 symbols and no other legal ack (for a different SPOT)
     * can arrive before 62 symbols. 
     */
    //private static final int TIME_TO_WAIT_FOR_ACK_MICROSECS = 864; // = 54 symbol periods
    //Ack Polling time increased for TinySPOTComm project
    private static final int TIME_TO_WAIT_FOR_ACK_MICROSECS = 1504; // = 94 symbol periods
    public static final int DEFAULT_MAX_RECEIVE_QUEUE_LENGTH = 1500;
    private static final int DEFAULT_RECEIVE_QUEUE_LENGTH_TO_DROP_BROADCAST_PACKETS = 1000;
    private Thread receiveThread;
    private byte macDSN;
    private Object ackMonitor = new Object();
    private Object sendMonitor = new Object();
    private boolean awaitingAck;
    private byte ackDSN;
    private int discardedAck = 0;
    private int lastDiscardedAckDSN = -1;
    private int lastDiscardedAckDSN2 = -1;
    private int lastAck = -1;
    protected long extendedAddress;
    protected boolean rxOnWhenIdle;
    protected Queue dataQueue;
    private Random random;
    protected int channelAccessFailure = 0;
    private int rxError = 0;
    private int wrongAck = 0;
    private int noAck = 0;
    private int nullPacketAfterAckWait = 0;
    private int maxReceiveQueueLength = DEFAULT_MAX_RECEIVE_QUEUE_LENGTH;
    private int receiveQueueLengthToDropBroadcastPackets = DEFAULT_RECEIVE_QUEUE_LENGTH_TO_DROP_BROADCAST_PACKETS;
    static final int A_MAX_FRAME_RETRIES = 4;  // was 3
    static final int A_MAX_BE = 5;

    private ILed receiveLed;
    private ILed sendLed;
    private boolean showUse = false;

    private boolean filterPackets = false;
    private boolean filterWhitelist = true;
    private long filterList[];
    protected int ticksPerMillisecond;
    
    /*
     * (non-Javadoc)
     * @see com.sun.squawk.peripheral.radio.I802_15_4_MAC#mcpsDataRequest(com.sun.squawk.peripheral.radio.RadioPacket)
     * 
     * This routine sends a packet
     */
    public final int mcpsDataRequest(RadioPacket rp) {
        // TODO Check RadioPacket params (or should the RadioPacket do its own checking?)
        synchronized (sendMonitor) {

            byte myDSN = getDSN();
            rp.setDSN(myDSN);

            int result = I802_15_4_MAC.NO_ACK;

            // Enable RX. Note that we do this *even* if we aren't expecting to receive an ack,
            // as otherwise sendIfChannelClear() will be unable to detect whether the channel is clear
            enableRx();
            for (int i = 0; i <= A_MAX_FRAME_RETRIES; i++) {

                int currentPriority = Thread.currentThread().getPriority();
                VM.setSystemThreadPriority(Thread.currentThread(), VM.MAX_SYS_PRIORITY);
                try {
                    if (!sendIfChannelClear(rp)) {
                        result = I802_15_4_MAC.CHANNEL_ACCESS_FAILURE;
                        VM.setSystemThreadPriority(Thread.currentThread(), currentPriority);
                        break;
                    } else {
                        if (rp.ackRequest()) {
                            if (waitForAckPacket(myDSN & 0x0ff)) {
                                result = I802_15_4_MAC.SUCCESS;
                                VM.setSystemThreadPriority(Thread.currentThread(), currentPriority);
                                break;
                            } else {
                                noAck++;
//							Utils.log("[mcpsDataRequest] Timed out: ACK  " + (myDSN & 0x0ff) + "  retry " + i);
                            }
                            VM.setSystemThreadPriority(Thread.currentThread(), currentPriority);

                            // didn't break out, so didn't find ack: don't bother to sleep if we aren't going around again
                            if (i < A_MAX_FRAME_RETRIES) {
                                int timeBeforeRetry = getTimeBeforeRetry(i);
                                if (timeBeforeRetry != 0) {
                                    int initialDelay = 10;  // was 2 * timeBeforeRetry / 3;
                                    Utils.sleep(initialDelay + random(timeBeforeRetry));
                                }
                            }
                        } else {
                            VM.setSystemThreadPriority(Thread.currentThread(), currentPriority);
                            result = I802_15_4_MAC.SUCCESS;
                            break;
                        }
                    }
                } catch (RuntimeException e) {
                    VM.setSystemThreadPriority(Thread.currentThread(), currentPriority);
                    throw e;
                }
            }
            conditionallyDisableRx();
            if (showUse) {
                if (sendLed == null) {
                    sendLed = Spot.getInstance().getRedLed();
                }
                sendLed.setOn(!sendLed.isOn());
            }
            return result;
        }
    }

    /**
     * return true if the rx queue is full
     */
    protected boolean isRxQueueOverUpperLimit() {
        return rxDataQueue().size() >= maxReceiveQueueLength;
    }

    /**
     * return true if the rx queue has space
     */
    protected boolean isRxQueueUnderLowerLimit() {
        return rxDataQueue().size() < receiveQueueLengthToDropBroadcastPackets;
    }

    private boolean waitForAckPacket(int myDSN) {
        while (isPhysicalActive()); // wait for tx to finish
        long endTime = System.currentTimeMillis() + getMacAckWaitDuration();
        do {
            synchronized (ackMonitor) {
                if (lastAck >= 0) {
                    throw new SpotFatalException("ACK already there when about to wait for it");
                }
                int gotAck = -1;
                if (lastDiscardedAckDSN >= 0 && lastDiscardedAckDSN == myDSN) {
                    gotAck = lastDiscardedAckDSN;
//                    System.out.println("[waitForAckPacket] ACK " + lastDiscardedAckDSN + " arrived before awaitingAck set!");
                } else if (lastDiscardedAckDSN2 >= 0 && lastDiscardedAckDSN2 == myDSN) {
                    gotAck = lastDiscardedAckDSN2;
//                    System.out.println("[waitForAckPacket] ACK " + lastDiscardedAckDSN2 + " arrived 2 ACKs before awaitingAck set!");
                }
                lastDiscardedAckDSN = -1;
                lastDiscardedAckDSN2 = -1;
                if (gotAck < 0) {
                    ackDSN = (byte) myDSN;
                    awaitingAck = true;
                    try {
                        ackMonitor.wait(getMacAckWaitDuration());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    awaitingAck = false;
                    gotAck = lastAck;
                }
                lastAck = -1;
                if (gotAck == myDSN) {
                    return true;
                } else if (gotAck < 0) {
                    // no ACK was heard
                    if (gotAck != -1) {
                        Utils.log("[waitForAckPacket] null ACK: " + gotAck);
                    }
                } else {
                    wrongAck++;
                    Utils.log("[waitForAckPacket] wrong ACK: " + gotAck + "  wanted: " + myDSN);
                    // don't just return false here in case this is an ack that has been
                    // waiting around for a while, and the one we want is just coming
                }
            }
        } while (endTime > System.currentTimeMillis());
        // Utils.log("[waitForAckPacket] ACK not received: " + myDSN);
        return false;
    }

    protected boolean isPhysicalActive() {
        return false;
    }

    protected boolean isPhysicalRxDataWaiting() {
        return false;
    }

    /*
     * Return how long to sleep before retrying a send. retry=0 implies the first retry 
     */
    protected int getTimeBeforeRetry(int retry) {
        // no delay unless overridden
        return 0;
    }

    protected void enableRx() {
        // Noop unless overridden		
    }

    public void mcpsDataIndication(RadioPacket rp) {
        RadioPacket internalRP = (RadioPacket) dataQueue.get();
        if (isRxOnDesired() && isRxQueueUnderLowerLimit()) {
            enableRx();
        }
//		Utils.log("got dsn =" + internalRP.getDataSequenceNumber() + " " + System.currentTimeMillis());
        rp.copyFrom(internalRP);
        if (showUse) {
            if (receiveLed == null) {
                receiveLed = Spot.getInstance().getGreenLed();
            }
            receiveLed.setOn(!receiveLed.isOn());
        }
    }

    public void mlmeStart(short panId, int channel) throws MAC_InvalidParameterException {
        if (receiveThread == null) {
            resetFiltering();
            startReceiveThread();
        }
    }

    public synchronized void mlmeReset(boolean resetAttribs) {
        // empty queues
        while (!dataQueue.isEmpty()) {
            dataQueue.get();
        }
        if (resetAttribs) {
            resetAttributes();
        }
    }

    public void setMaxReceiveQueueLength(int maxPackets) {
        maxReceiveQueueLength = maxPackets;
    }

    public void setReceiveQueueLengthToDropBroadcastPackets(int maxPackets) {
        receiveQueueLengthToDropBroadcastPackets = maxPackets;
    }

    public int getMaxReceiveQueueLength() {
        return maxReceiveQueueLength;
    }

    public int getReceiveQueueLengthToDropBroadcastPackets() {
        return receiveQueueLengthToDropBroadcastPackets;
    }

    public long mlmeGet(int attribute) throws MAC_InvalidParameterException {
        long result;
        switch (attribute) {
            case A_EXTENDED_ADDRESS:
                result = extendedAddress;
                break;
            case MAC_RX_ON_WHEN_IDLE:
                result = rxOnWhenIdle ? TRUE : FALSE;
                break;
            default:
                throw new MAC_InvalidParameterException();
        }
        return result;
    }

    public void mlmeSet(int attribute, long value) throws MAC_InvalidParameterException {
        switch (attribute) {
            case MAC_RX_ON_WHEN_IDLE:
                rxOnWhenIdle = value == TRUE;
                rxOnWhenIdleChanged();
                break;
            default:
                throw new MAC_InvalidParameterException();
        }
    }

    /*
     * return true if the user expects the rx to be on
     */
    protected boolean isRxOnDesired() {
        return rxOnWhenIdle;
    }

    /**
     * Set the IEEE Address. Need to call mlmeStart(short, int) to have it take effect.
     *
     * @param ieeeAddr new radio address set after next call to mlmeStart()
     */
    public void setIEEEAddress(long ieeeAddr) {
        extendedAddress = ieeeAddr;
        Utils.log("My IEEE address modified to be " + IEEEAddress.toDottedHex(extendedAddress));
    }

    protected abstract void dataIndication(RadioPacket recvPacket);

    /**
     * @return time (in millis) to wait for an ack
     */
    protected abstract int getMacAckWaitDuration();

    protected abstract Queue rxDataQueue();

    protected boolean sendIfChannelClear(RadioPacket rp) {
        return true;
    }

    protected abstract void setIEEEAddress();

    protected void validateDestAddr(RadioPacket recvPacket) throws MACException {
    }

    protected void dump() {
    }

    protected void startReceiveThread() {
        receiveThread = new ReceiveThread();
        receiveThread.setPriority(Thread.MAX_PRIORITY - 1);
        setAsDaemonThread(receiveThread);
        receiveThread.start();
        Thread.yield();
    }

    /**
     * Return a random in the range 0 to i
     * 
     * @param i
     * @return
     */
    protected int random(int i) {
        return random.nextInt(i + 1);
    }

    protected void initialize() {
        dataQueue = new Queue();
        setIEEEAddress();
        random = new Random(extendedAddress);
        resetAttributes();
        showUse = "true".equalsIgnoreCase(Utils.getSystemProperty("radio.traffic.show.leds",
                Utils.getManifestProperty("radio-traffic-show-leds", "false")));
    }

    private byte getDSN() {
        return macDSN++;
    }

    /**
     * Reset the MAC attributes
     *
     */
    private void resetAttributes() {
        macDSN = (byte) (random.nextInt() & 0xFF);
        rxOnWhenIdle = false;
    }

    /**
     * Setup whether to filter packets based on a whitelist and
     * if so parse the whitelist. Uses the system properties:
     * radio.filter & radio.whitelist. The whitelist is a comma
     * separated list of radio addresses, where only the low part
     * of the addresses need to be specified, 
     * e.g. 1234 = 0014.4F01.0000.1234
     */
    public void resetFiltering() {
        filterPackets = false;
        if ("true".equalsIgnoreCase(Utils.getSystemProperty("radio.filter",
                                    Utils.getManifestProperty("radio-filter", "false")))) {
            String addrList = Utils.getSystemProperty("radio.whitelist",
                                    Utils.getManifestProperty("radio-whitelist", null));
                filterWhitelist = true;
            if (addrList == null || addrList.length() < 1) {
                filterWhitelist = false;
                addrList = Utils.getSystemProperty("radio.blacklist",
                                    Utils.getManifestProperty("radio-blacklist", null));
            }
            // comma separated list of LSBs: 0117, 29e2, 51.047A
            if (addrList != null && addrList.trim().length() > 1) {
                System.out.println("*** Radio will " + (filterWhitelist ? "only handle" : "ignore") + " packets received from: ");
                String addresses[] = Utils.split(addrList, ',');
                filterList = new long[addresses.length];
                for (int i = 0; i < addresses.length; i++) {
                    String addr = addresses[i].trim();
                    try {
                        filterList[i] = IEEEAddress.toLong("0014.4F01.0000.0000".substring(0, 19 - addr.length()) + addr);
                        System.out.println("***    " + IEEEAddress.toDottedHex(filterList[i]));
                        filterPackets = true;
                    } catch (IllegalArgumentException ex) {
                        System.out.println("Error: radio.whitelist badly formed: " + addr);
                        filterList[i] = 0;
                    }
                }
            } else {
                filterPackets = false;
            }
        }
    }
    
    /**
     * Make the dispatcher thread a daemon.
     * @param dispatcherThread
     */
    protected abstract void setAsDaemonThread(Thread dispatcherThread);

    /**
     * @author Syntropy
     *
     * The ReceiveThread class loops around reading packets from the physical layer
     * and queuing them for despatch to our clients. It copies their contents into
     * RadioPackets supplied by our clients and manages a pool locally so that we
     * minimise our memory allocations.
     */
    private class ReceiveThread extends Thread {

        public ReceiveThread() {
            super("MAC ReceiveThread");
        }

        public void run() {
            if (filterPackets) {            // call different routines so that
                receiveWithFilter();        // if we are not filtering packets
            } else {                        // we do not need an extra "if" in
                receiveAll();               // the inner loop of the radio code
            }
        }
        
        private void receiveAll() {  // make sure any changes get mirrored in receiveWithFilter()
            while (true) {
                RadioPacket recvPacket = RadioPacket.getDataPacket();
                try {
                    dataIndication(recvPacket);
                    try {
                        recvPacket.decodeFrameControl();
                        if (recvPacket.isData()) {
                            if (awaitingAck) {
//                                Utils.log("Received data packet when awaiting ACK");
//                                Utils.log("Size = " + recvPacket.getLength());
//                                Utils.log(Utils.stringify(recvPacket.buffer));
                            }
                            validateDestAddr(recvPacket);
//				Utils.log("rx dsn =" + recvPacket.getDataSequenceNumber() + " " + System.currentTimeMillis() + " " + Thread.currentThread().getPriority());
                            if (recvPacket.getDestinationAddress() == extendedAddress || isRxQueueUnderLowerLimit()) {
                                rxDataQueue().put(recvPacket);
                            }
                            if (isRxQueueOverUpperLimit()) {
                                disableRx();
                            }
                        } else if (recvPacket.isAck()) {
                            synchronized (ackMonitor) {
                                if (awaitingAck) {
                                    if (recvPacket.getDataSequenceNumber() == ackDSN) {
                                        lastAck = ackDSN & 0x0ff;
                                        awaitingAck = false;
                                        ackMonitor.notify();
                                    } else {
                                        wrongAck++;
                                    }
                                } else {
                                    discardedAck++;
                                    lastDiscardedAckDSN2 = lastDiscardedAckDSN;
                                    lastDiscardedAckDSN = recvPacket.getDataSequenceNumber() & 0x0ff;
//                                    Utils.log("Discarding an ack with dsn " + lastDiscardedAckDSN);
                                }
                            }
                        } else {
                            rxError("RX error: Unknown packet type received: frame type =" +
                                    Integer.toHexString(recvPacket.getFrameControl()));
                        }
                    } catch (IllegalStateException badlyFormattedPacketException) {
                        rxError("RX error: " + badlyFormattedPacketException.getMessage());
                    } catch (MACException e) {
                        rxError("RX error: " + e.getMessage());
                    }
                } catch (Throwable e) {
                    System.err.println("RX thread error: " + e.getMessage());
                    rxError++;
                }
            }
        }

        private void rxError(String msg) {
            resetRx();
            rxError++;
            Utils.log(msg);
            dump();
        }

        /**
         * Same as receiveAll() except before queuing a received packet first check
         * that it was sent by a SPOT whose address is on our whitelist.
         */
        private void receiveWithFilter() {
            while (true) {
                RadioPacket recvPacket = RadioPacket.getDataPacket();
                try {
                    dataIndication(recvPacket);
                    try {
                        recvPacket.decodeFrameControl();
                        if (recvPacket.isData()) {
                            if (awaitingAck) {
//				Utils.log("Received data packet when awaiting ACK");
                            }
                            validateDestAddr(recvPacket);
//				Utils.log("rx dsn =" + recvPacket.getDataSequenceNumber() + " " + System.currentTimeMillis() + " " + Thread.currentThread().getPriority());
                            if (recvPacket.getDestinationAddress() == extendedAddress || isRxQueueUnderLowerLimit()) {
                                if (filterPackets) {
                                    long sourceAddr = recvPacket.getSourceAddress();
                                    boolean match = false;
                                    for (int i = 0; i < filterList.length; i++) {
                                        if (filterList[i] == sourceAddr) {
                                            match = true;
                                            break;
                                        }
                                    }
                                    if (!(filterWhitelist ^ match)) {
                                        rxDataQueue().put(recvPacket);
                                    }
                                } else {
                                    rxDataQueue().put(recvPacket);
                                }
                            }
                            if (isRxQueueOverUpperLimit()) {
                                disableRx();
                            }
                        } else if (recvPacket.isAck()) {
                            synchronized (ackMonitor) {
                                if (awaitingAck) {
                                    if (recvPacket.getDataSequenceNumber() == ackDSN) {
                                        lastAck = ackDSN & 0x0ff;;
                                        awaitingAck = false;
                                        ackMonitor.notify();
                                    } else {
                                        wrongAck++;
//                                  Utils.log("[RX] Received ack with dsn " + (recvPacket.getDataSequenceNumber() & 0x0ff) + " waiting for " + (ackDSN & 0x0ff));
                                    }
                                } else {
                                    discardedAck++;
                                    lastDiscardedAckDSN2 = lastDiscardedAckDSN;
                                    lastDiscardedAckDSN = recvPacket.getDataSequenceNumber() & 0x0ff;
//                                  Utils.log("[RX] Discarding an ack with dsn " + recvPacket.getDataSequenceNumber());
                                }
                            }
                        } else {
                            rxError("RX error: Unknown packet type received: frame type =" +
                                    Integer.toHexString(recvPacket.getFrameControl()));
                        }
                    } catch (IllegalStateException badlyFormattedPacketException) {
                        rxError("RX error: " + badlyFormattedPacketException.getMessage());
                    } catch (MACException e) {
                        rxError("RX error: " + e.getMessage());
                    }
                } catch (Throwable e) {
                    System.err.println("RX thread error: " + e.getMessage());
                    rxError++;
                }
            }
        }
    }

    public int getAckQueueJunk() {
        return 0;
    }

    public int getChannelAccessFailure() {
        return channelAccessFailure;
    }

    public int getNoAck() {
        return noAck;
    }

    public int getWrongAck() {
        return wrongAck;
    }

	public int getRxError() {
		return rxError;
	}

    public int getNullPacketAfterAckWait() {
        return nullPacketAfterAckWait;
    }

    public void resetErrorCounters() {
        nullPacketAfterAckWait = 0;
		channelAccessFailure = 0;
        noAck = 0;
        wrongAck = 0;
        rxError = 0;
	}

    protected abstract void disableRx();

    protected abstract void resetRx();

    protected void rxOnWhenIdleChanged() throws PHY_InvalidParameterException {
        if (isRxOnDesired() && !isRxQueueOverUpperLimit()) {
            enableRx();
        } else {
            disableRx();
        }
    }

    protected void conditionallyDisableRx() {
        //Utils.log("[conditionallyDisableRx] called...");
        if (!isRxOnDesired() || isRxQueueOverUpperLimit()) {
            //Utils.log("[conditionallyDisableRx] turning off rx...");
            disableRx();
        }
    }
}
