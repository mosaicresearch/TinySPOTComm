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

package com.sun.spot.peripheral.radio.mhrp.aodv;

import java.util.Enumeration;
import java.util.Vector;

import com.sun.spot.peripheral.radio.ILowPan;
import com.sun.spot.peripheral.radio.IProtocolManager;
import com.sun.spot.peripheral.radio.LowPan;
import com.sun.spot.peripheral.radio.LowPanHeaderInfo;
import com.sun.spot.peripheral.radio.routing.RouteInfo;
import com.sun.spot.peripheral.radio.mhrp.aodv.messages.AODVMessage;
import com.sun.spot.peripheral.radio.mhrp.aodv.messages.RERR;
import com.sun.spot.peripheral.radio.mhrp.aodv.messages.RREP;
import com.sun.spot.peripheral.radio.mhrp.aodv.messages.RREQ;
import com.sun.spot.peripheral.radio.mhrp.aodv.request.RequestTable;
import com.sun.spot.peripheral.radio.mhrp.aodv.routing.RoutingTable;
import com.sun.spot.peripheral.radio.mhrp.interfaces.IMHEventListener;
import com.sun.spot.peripheral.radio.routing.interfaces.IRoutingPolicyManager;
import com.sun.spot.peripheral.radio.routing.interfaces.RouteEventClient;
import com.sun.spot.util.Debug;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Queue;
/**
 * @author Allen Ajit George
 * @version 0.1
 */
public class Receiver extends Thread implements IProtocolManager {
    
    private RoutingTable routingTable = RoutingTable.getInstance();
    private RequestTable requestTable = RequestTable.getInstance();
    
    private static final int MAX_REQUESTS_OUTSTANDING = 500;
    private Sender sender;
    private boolean keepRunning = true;
    private Queue messageQueue;
    private Vector mhRouteListeners;
    private IRoutingPolicyManager rpm;
    private final long ourAddress;
    
    /**
     * constructs a new receiver thread.
     * @param sender
     * @param lowPan
     */
    public Receiver(long ourAddress, Sender sender, ILowPan lowPan, Vector listeners) {
        super("AODVReceiver");
        this.sender = sender;
        this.ourAddress = ourAddress;
        messageQueue = new Queue();
        lowPan.registerProtocol(Constants.AODV_PROTOCOL_NUMBER, this);
        this.mhRouteListeners = listeners;
        rpm = ((LowPan)lowPan).getRoutingPolicyManager();
        
        //Debug.print("receiver: started", 2);
    }
    
    /**
     * incoming packets are evaluated and the appropriate message handler is called
     */
    public void run() {
        ReceivedPacket receivedPacket;
        
        while (keepRunning) {
            receivedPacket = (ReceivedPacket) messageQueue.get();
            if (receivedPacket != null) {
                switch (receivedPacket.message.getType()) {
                    case Constants.RREQ_TYPE:
                        handleRREQMessage((RREQ) receivedPacket.message,
                                receivedPacket.messageSender);
                        break;
                    case Constants.RREP_TYPE:
                        handleRREPMessage((RREP) receivedPacket.message,
                                receivedPacket.messageSender);
                        break;
                    case Constants.RERR_TYPE:
                        handleRERRMessage((RERR) receivedPacket.message,
                                receivedPacket.messageSender);
                    default:
                        break;
                }
            }
        }
    }
    
    public void stopThread() {
        keepRunning = false;
        messageQueue.put(null);
    }
    
    /**
     * Handler for route error messages, deactivates the broken route.
     * If necessary the message is forwarded.
     *
     * @param message
     * @param lastHop the node that this message was sent from
     */
    private void handleRERRMessage(RERR message, long lastHop) {
//        Debug.print("[AODV] handle RERRMessage from " + new IEEEAddress(lastHop) + " for route to "
//                + new IEEEAddress(message.getDestAddress()) + " as requested by "
//                + new IEEEAddress(message.getOrigAddress()) + " at " + System.currentTimeMillis());
        
        if (!mhRouteListeners.isEmpty()) {
            Enumeration en = mhRouteListeners.elements();
            while (en.hasMoreElements()) {
                ((IMHEventListener) en.nextElement()).RERRReceived(message.getOrigAddress(), message.getDestAddress());
            }
        }
        routingTable.deactivateRoute(ourAddress, message.getDestAddress());
        if (message.getOrigAddress() == ourAddress) {
            // Debug.print("handleRERRMessage: RERR for this node", 1);
        } else {
            routingTable.deactivateRoute(message.getOrigAddress(), message.getDestAddress());
            sender.forwardAODVMessage(message);
        }
    }
    
    /**
     * Handler for route reply messages. A new route is added, if necessary the
     * message is forwarded.
     *
     * @param message
     * @param lastHop
     *            the node we got this message from
     */
    private void handleRREPMessage(RREP message, long lastHop) {
        
//        Debug.print("[AODV] handle RREPMessage from " + new IEEEAddress(lastHop)
//        + " for route to " + new IEEEAddress(message.getDestAddress())
//        + " as requested by " + new IEEEAddress(message.getOrigAddress())
//        + " at " + System.currentTimeMillis());
        
        if (!mhRouteListeners.isEmpty()) {
            Enumeration en = mhRouteListeners.elements();
            while (en.hasMoreElements()) {
                ((IMHEventListener)
                en.nextElement()).RREPReceived(message.getOrigAddress(),
                        message.getDestAddress(),
                        message.getHopCount());
                
            }
        }
        message.incrementHopCount();
        
        if (message.getOrigAddress() == (long)0xffff) {
//            Debug.print("[AODV] Processing Neighbor Advertisement");
            // getNextHopInfo will freshen the route if it exists
            RouteInfo ri = routingTable.getNextHopInfo(message.getDestAddress());
            if (ri.nextHop == Constants.INVALID_NEXT_HOP) {
                routingTable.addRoute(lastHop, message);
            } 
            
            return;
        }
        //Debug.print("handleRREPMessage: RREP from "
        //+ new IEEEAddress(lastHop).asDottedHex(), 1);
        
        if (message.getOrigAddress() == ourAddress) {
            routingTable.addRoute(lastHop, lastHop, 1, 1,
                    Constants.UNKNOWN_SEQUENCE_NUMBER);
            if (requestTable.hasActiveRequest(message)) {
                routingTable.addRoute(lastHop, message);
            }
            while (requestTable.hasActiveRequest(message)) {
                RouteEventClient client = requestTable.getCallback(message);
                if (client != null) {
                    Object uniqueKey = requestTable.removeOutstandingRequest(
                            message.getDestAddress(), message.getOrigAddress());
                    RouteInfo info = new RouteInfo(message.getDestAddress(),
                            lastHop, message.getHopCount());
                    client.routeFound(info, uniqueKey);
                } else {
                    Debug.print("handleRREPMessage: null client asssociated"
                            + " Destination " + IEEEAddress.toDottedHex(message.getDestAddress())
                            + " Originator " + IEEEAddress.toDottedHex(message.getOrigAddress()), 1);
                }
            }
        } else {
            if (!rpm.isEndNode()) { // Only forward RREP if not an end node
                routingTable.addRoute(lastHop, lastHop, 1, 1, Constants.UNKNOWN_SEQUENCE_NUMBER);
                routingTable.addRoute(lastHop, message);
                sender.forwardAODVMessage(message);
            }
        }
    }
    
    /**
     * Handler for route request messages. Eventually new routes are added. If necessary
     * message is forwarded, or a route reply is sent.
     * @param message
     * @param lastHop
     */
    private void handleRREQMessage(RREQ message, long lastHop) {
//        Debug.print("[AODV] handle RREQMessage from " + new IEEEAddress(lastHop) + " for route to "
//                + new IEEEAddress(message.getDestAddress()) + " as requested by "
//                + new IEEEAddress(message.getOrigAddress()) + " at " + System.currentTimeMillis());
        
        if (!mhRouteListeners.isEmpty()) {
            Enumeration en = mhRouteListeners.elements();
            while (en.hasMoreElements()) {
                ((IMHEventListener) en.nextElement()).RREQReceived(message.getOrigAddress(), message.getDestAddress(),
                        message.getHopCount());
            }
        }
        
        if (message.getOrigAddress() != ourAddress) {
            message.incrementHopCount();
            if (message.getHopCount() > Constants.NET_DIAMETER) return;  // Don't forward request
            
            routingTable.addRoute(lastHop, lastHop, 1, 1, Constants.UNKNOWN_SEQUENCE_NUMBER);
            routingTable.addRoute(lastHop, message);
            
            
            // Don't need to check if we have this request, we did this on reception.        
            int currentSequenceNumber = routingTable.getDestinationSequenceNumber(message.getDestAddress());
            int receivedSequenceNumber = message.getDestSeqNum();
            
            if (currentSequenceNumber > receivedSequenceNumber) {
                message.setDestinationSequenceNumber(currentSequenceNumber);
            }
            
            if (message.getDestAddress() == ourAddress) {
                // Debug.print("handleRREQMessage: RREQ for this node", 1);
                if (requestTable.hasActiveRequest(message)) {
                    RREP routeFoundMessage = new RREP(message);
                    sender.sendNewRREP(routeFoundMessage);
                }
            } else {
                if (!rpm.isEndNode()) {// Only forward RREQ if not an end node
                    sender.forwardAODVMessage(message);
                }
                
            }
            
            
            
        }
    }
    
// public void stop() {
// keepRunning = false;
// }
    /**
     * This method is called whenever the low pan layer receives a packet that
     * carries this routing manager's protocol number. It implements the
     * protocol managers interface.
     *
     * @param payload
     * @param headerInfo
     */
    public void processIncomingData(byte[] payload, LowPanHeaderInfo headerInfo) {
        byte AODVMessageType = payload[0];
        switch (AODVMessageType) {
            case Constants.RREQ_TYPE:
//                Debug.print("receiveData: RREQ from "
//                        + new IEEEAddress(headerInfo.sourceAddress).asDottedHex(), 1);
                RREQ rreqMessage = new RREQ(payload);
                if (!requestTable.hasRequest(rreqMessage)) {
                    if (messageQueue.size() < MAX_REQUESTS_OUTSTANDING) {
                        requestTable.addRREQ(rreqMessage, null, null);
                        messageQueue.put(new ReceivedPacket(rreqMessage, headerInfo.sourceAddress));
                    }
                }
                break;
            case Constants.RREP_TYPE:
//                Debug.print("receiveData: RREP from "
//                        + new IEEEAddress(headerInfo.sourceAddress).asDottedHex(), 1);
                RREP rrepMessage = new RREP(payload);
                messageQueue.put(new ReceivedPacket(rrepMessage, headerInfo.sourceAddress));
                break;
            case Constants.RERR_TYPE:
//                Debug.print("receiveData: RERR from "
//                        + new IEEEAddress(headerInfo.sourceAddress).asDottedHex(), 1);
                RERR rerrMessage = new RERR(payload);
                messageQueue.put(new ReceivedPacket(rerrMessage, headerInfo.sourceAddress));
                break;
            default:
                System.err.println("receiveData: bad packet from"
                        + new IEEEAddress(headerInfo.sourceAddress).asDottedHex());
                break;
        }
    }
    
    private class ReceivedPacket {
        public ReceivedPacket(AODVMessage message, long messageSender) {
            this.message = message;
            this.messageSender = messageSender;
        }
        
        public AODVMessage message;
        public long messageSender;
    }
}
