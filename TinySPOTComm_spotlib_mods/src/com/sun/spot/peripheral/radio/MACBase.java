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

import com.sun.spot.peripheral.ISpot;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Queue;
import com.sun.spot.util.Utils;
import com.sun.squawk.VM;

abstract class MACBase implements I802_15_4_MAC, IProprietaryMAC {

    /* In a non-beaconed network, any value for this in the range 34 to 62 symbol periods should be ok.
     * The ACK should arrive after exactly 34 symbols and no other legal ack (for a different SPOT)
     * can arrive before 62 symbols. 
     */
    //private static final int TIME_TO_WAIT_FOR_ACK_MICROSECS = 864; // = 54 symbol periods
	//Ack Polling time increased for TinySPOTComm project
    private static final int TIME_TO_WAIT_FOR_ACK_MICROSECS = 992; // = 62 symbol periods
    public static final int DEFAULT_MAX_RECEIVE_QUEUE_LENGTH = 1500;
    private static final int DEFAULT_RECEIVE_QUEUE_LENGTH_TO_DROP_BROADCAST_PACKETS = 1000;
    private Thread receiveThread;
    private byte macDSN;
    private Object ackMonitor = new Object();
    private boolean awaitingAck;
    private RadioPacket lastAck;
    protected long extendedAddress;
    protected boolean rxOnWhenIdle;
    protected Queue dataQueue;
    private Random random;
    protected int channelAccessFailure = 0;
    private int wrongAck = 0;
    private int noAck = 0;
    private int nullPacketAfterAckWait = 0;
    private int maxReceiveQueueLength = DEFAULT_MAX_RECEIVE_QUEUE_LENGTH;
    private int receiveQueueLengthToDropBroadcastPackets = DEFAULT_RECEIVE_QUEUE_LENGTH_TO_DROP_BROADCAST_PACKETS;
    static final int A_MAX_FRAME_RETRIES = 3;
    static final int A_MAX_BE = 5;

    /*
     * (non-Javadoc)
     * @see com.sun.squawk.peripheral.radio.I802_15_4_MAC#mcpsDataRequest(com.sun.squawk.peripheral.radio.RadioPacket)
     * 
     * This routine sends a packet
     */
    public final synchronized int mcpsDataRequest(RadioPacket rp) {
        // TODO Check RadioPacket params (or should the RadioPacket do its own checking?)

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
                        if (pollForAckPacket(myDSN)) {
                            result = I802_15_4_MAC.SUCCESS;
                            VM.setSystemThreadPriority(Thread.currentThread(), currentPriority);
                            break;
                        } else {
                            noAck++;
//							Utils.log("Timed out waiting for ack of my packet with DSN " + myDSN + " for retry (i)=" + i);
                        }
                        VM.setSystemThreadPriority(Thread.currentThread(), currentPriority);

                        // didn't break out, so didn't find ack: don't bother to sleep if we aren't going around again
                        if (i < A_MAX_FRAME_RETRIES) {
                            long timeBeforeRetry = getTimeBeforeRetry(i);
                            if (timeBeforeRetry != 0) {
                                Utils.sleep(timeBeforeRetry);
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
        return result;
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

    private boolean pollForAckPacket(byte myDSN) {
        ISpot spot = Spot.getInstance();
        while (isPhysicalActive()); // wait for tx to finish
        int startTicks = spot.getSystemTicks();
        int now = startTicks;
        int targetTicks = startTicks + ((ISpot.SYSTEM_TICKER_TICKS_PER_MILLISECOND * TIME_TO_WAIT_FOR_ACK_MICROSECS) / 1000);
        do {
            if (isPhysicalRxDataWaiting()) {
                RadioPacket ackPacket = waitForAckPacket();
                if (ackPacket == null) {
                    // whatever it was that came, it wasn't our ACK
                    // waitForAckPacket waits for longer than the ACK wait period, so
                    // we can just give up
                    nullPacketAfterAckWait++;
                    return false;
                }
                if (ackPacket.getDataSequenceNumber() == myDSN) {
                    return true;
                } else {
                    wrongAck++;
                // don't just return false here in case this is an ack that has been
                // waiting around for a while, and the one we want is just coming
                }
            }
            now = spot.getSystemTicks();
            if (now < startTicks) {
                now += ISpot.SYSTEM_TICKER_TICKS_PER_MILLISECOND;
            }
        } while (now < targetTicks || isPhysicalRxDataWaiting());
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
    protected long getTimeBeforeRetry(int retry) {
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
    }

    public void mlmeStart(short panId, int channel) throws MAC_InvalidParameterException {
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
     * Set the IEEE Address
     * @param i
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
        startReceiveThread();
    }

    private synchronized byte getDSN() {
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

        public void run() {
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
                                rxDataQueue().put(recvPacket);
                            }
                            if (isRxQueueOverUpperLimit()) {
                                disableRx();
                            }
                        } else if (!recvPacket.isAck()) {
                            dump();
                            throw new MACException("Unknown packet type received: frame type =" + recvPacket.getFrameControl());
                        } else {
                            synchronized (ackMonitor) {
                                if (awaitingAck) {
                                    lastAck = recvPacket;
                                    awaitingAck = false;
                                    ackMonitor.notify();
                                } else {
//                                  Utils.log("Discarding an ack with dsn " + recvPacket.getDataSequenceNumber());
                                }
                            }
                        }
                    } catch (IllegalStateException badlyFormattedPacketException) {
                        System.err.println("RX error: " + badlyFormattedPacketException.getMessage());
                    } catch (MACException e) {
                        System.err.println("RX error: " + e.getMessage());
                    }
                } catch (Throwable e) {
                    System.err.println("RX thread error: " + e.getMessage());
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

    private RadioPacket waitForAckPacket() {
        synchronized (ackMonitor) {
            if (lastAck != null) {
                throw new SpotFatalException("ACK already there when about to wait for it");
            }
            awaitingAck = true;
            try {
                ackMonitor.wait(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            awaitingAck = false;
        }
        RadioPacket result = lastAck;
        lastAck = null;
        return result;
    }

    public int getNullPacketAfterAckWait() {
        return nullPacketAfterAckWait;
    }

    protected abstract void disableRx();

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
