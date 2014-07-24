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

package com.sun.spot.peripheral.radio.mhrp.lqrp.linkParams;

import com.sun.spot.peripheral.radio.I802_15_4_MAC;
import com.sun.spot.peripheral.radio.IPacketQualityListener;
import com.sun.spot.peripheral.radio.IRadioPacketDispatcher;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.RadioPacketDispatcher;
import com.sun.spot.peripheral.radio.mhrp.lqrp.Sender;
import com.sun.spot.peripheral.radio.mhrp.lqrp.messages.LQREQ;
import com.sun.spot.peripheral.radio.mhrp.lqrp.routing.RoutingTable;
import com.sun.spot.resources.Resource;
import com.sun.spot.resources.Resources;
import com.sun.spot.util.IEEEAddress;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 *
 * @author pradip de, modified by Ron Goldman
 */
public class NodeLifeAndLinkMonitor extends Resource implements IPacketQualityListener {
    
    private long updateTime = 0;
    //private Vector neighborLinks;
    private Hashtable neighborLinks;
    private static NodeLifeAndLinkMonitor instance;
    private NodeLifetime nodeLifetime;
    private long ourAddress;
    private long basestationAddress;
    private Sender sender;
    private boolean onHost;
    private boolean onSpot;
    private IRadioPacketDispatcher rpd;
    private I802_15_4_MAC socketMAC;

    private RoutingTable routingTable = RoutingTable.getInstance();


    private NodeLifeAndLinkMonitor() {
        neighborLinks = new Hashtable();
        nodeLifetime = new NodeLifetime(this);
        updateTime = System.currentTimeMillis() + ConfigLinkParams.SLOT_SIZE;
    }
    
    /**
     * @return lqrpManager instance of this singleton
     */
    public static synchronized NodeLifeAndLinkMonitor getInstance() {
        if (instance == null) {
            instance = (NodeLifeAndLinkMonitor) Resources.lookup(NodeLifeAndLinkMonitor.class);
            if (instance == null) {
                instance = new NodeLifeAndLinkMonitor();
                Resources.add(instance);
            }
        }
        return instance;
    }
    
    
    public void initialize(long ourAddress) {
        this.ourAddress = ourAddress;
        socketMAC = RadioFactory.getSocketMAC();
        if (socketMAC != null) {
            basestationAddress = socketMAC.mlmeGet(I802_15_4_MAC.A_EXTENDED_ADDRESS);
        } else {
            basestationAddress = ourAddress;
        }
        // check if running on host, maybe in Emulator
        onHost = RadioFactory.isRunningOnHost();
        onSpot = !(onHost || RadioFactory.isRunningInEmulator());
        rpd = RadioPacketDispatcher.getInstance();
    }

    public void setSender(Sender sender) {
        this.sender = sender;
        if (sender != null) {
            rpd.addPacketQualityListener(this);
        } else {
            rpd.removePacketQualityListener(this);
        }
    }
    
    /**
     * Update LQI Sum for each packet received. Called by RadioPacketDispatcher.
     *
     */
    public void notifyPacket(long srcAddress, long dest, int rssi, int corr, int lq, int packetSize) {
        // first check if past time to update info for all neighboring links
        long now = System.currentTimeMillis();
        if (updateTime <= now) {
            updateLinkInfo();
            updateTime = System.currentTimeMillis() + ConfigLinkParams.SLOT_SIZE;
        }
        // now update link info for this packet
        if (srcAddress != ourAddress && srcAddress != basestationAddress) { // Received Packet
//            System.out.println("[notifyPacket] received packet from " + IEEEAddress.toDottedHex(srcAddress) +
//                    "  to " + IEEEAddress.toDottedHex(dest));
            NbrLinkInfo nlInfo = getNbrLinkInfoWithAddress(srcAddress);
            if (nlInfo == null) {
                double lqI = (double)lq / (double)ConfigLinkParams.MAX_LQ;
                nlInfo = addLinkWithAddress(srcAddress, lqI); //Configure an initial lqI with the value just seen
                if (sender != null) {
                    // don't bother sending an LQREQ if on host and not talking to a free range SPOT
                    if (onSpot || (onHost && (rpd.getMAC(srcAddress) != socketMAC))) {
                        sender.sendNewLQREQ(new LQREQ(ourAddress, srcAddress, lqI));
                    }
                }
            } else if (nlInfo.getNbrLastLQREP() >= 0 && nlInfo.getNbrLastLQREQ() > 0 &&
                    nlInfo.getNbrLastLQREP() < nlInfo.getNbrLastLQREQ() &&
                    (nlInfo.getNbrLastLQREQ() + 500) < now) {
                double nbrLQ = nlInfo.getNbrLQ();
                boolean badRoute = false;
                if (nbrLQ < 0) {
                    nbrLQ -= 1.0;
                    badRoute = nbrLQ < -3.0;    // 3 misses and deactivate the route
                } else {
                    nbrLQ *= 3.0/4.0;           // reduce quality by 75%
                    badRoute = nbrLQ < 0.4;     // & deactivate route if below threshold (= 3 misses)
                }
                nlInfo.setNbrLQ(nbrLQ);     // hasn't replied to our last ping so decrease link quality
                nlInfo.setOurNbrLQ(nlInfo.getCurrNormalizedLQ());
                nlInfo.setNbrLastLQREP(-1);
                if (badRoute) {
                    routingTable.deactivateRoutesUsing(srcAddress);
                    System.out.println("[notifyPacket] deactivate routes for " + IEEEAddress.toDottedHex(srcAddress));
                }
                if (sender != null) {    // but give neighbor another chance
                    sender.sendNewLQREQ(new LQREQ(ourAddress, srcAddress, nlInfo.getCurrNormalizedLQ()));
                }
//                System.out.println("[NodeLifeAndLinkMonitor] LQREP timeout with " + IEEEAddress.toDottedHex(srcAddress));
            }
            nlInfo.incSumLQ(lq);
            nlInfo.incNumOfPktsInSlot();
            nlInfo.setNbrLastHeard(now);
            nodeLifetime.incBytesRecvdCount(packetSize);
        } else {// Sent Packet
            nodeLifetime.incBytesTransCount(packetSize);
        }
    }
        
    /* Calculate LQ and RSSI Average using history weights over whole window */
    
    
    /* Add a new link for a neighbor with a given address */
    public NbrLinkInfo addLinkWithAddress(long address, double lq) {
        Long addressKey = new Long(address);
        NbrLinkInfo nbrLink = new NbrLinkInfo(this, address);
        nbrLink.setCurrNormalizedLQ(lq);
       // neighborLinks.addElement(nbrLink);
        neighborLinks.put(addressKey, nbrLink);
        //System.out.println("Added Link with Address " + IEEEAddress.toDottedHex(address) + " with Link Quality " + lq);
        return nbrLink;
    }
    
       
    public NbrLinkInfo getNbrLinkInfoWithAddress(long address) {
        Long addressKey = new Long(address);
        NbrLinkInfo returnLink = null;
        if (neighborLinks.containsKey(addressKey)) {
            returnLink = (NbrLinkInfo)neighborLinks.get(addressKey);
        }
        return returnLink;
    }
    
       
    /* Remove a link for a neighbor with a given address */
    public void removeLinkWithAddress(long address) {
        Long addressKey = new Long(address);
        if (neighborLinks.containsKey(addressKey)) {
            neighborLinks.remove(addressKey);
        }
    }
    
    
    /* To be called by Routing Manager */
    public double getCurrNormLQForAddress(long address) {
        Long addressKey = new Long(address);
        NbrLinkInfo nbrLInfo = (NbrLinkInfo)neighborLinks.get(addressKey);
        double normLQI = 0.93;   // A typical value for links that are non-existent since
                                 // we will receive the RREQ/RREP before notifyPacket() is called
        if (nbrLInfo != null) {
            normLQI = nbrLInfo.getCurrentLinkCost();
        }
        return normLQI;
    }
    
    
    /* !!! For Testing */
    public void setCurrNormLQForAddress(long address, double normLQ) {
        NbrLinkInfo nbrLInfo;
        Long addressKey = new Long(address);
        if (neighborLinks.containsKey(addressKey)) {
            nbrLInfo = (NbrLinkInfo)neighborLinks.get(addressKey);
            nbrLInfo.setCurrNormalizedLQ(normLQ);
        }
        else {
            addLinkWithAddress(address, normLQ); 
        }
        
    }
    
    /* To be called by Receiver upon receiving an LQREP packet */
    public void setNbrLQ(long address, double cost) {
        Long addressKey = new Long(address);
        NbrLinkInfo nbrLInfo = (NbrLinkInfo)neighborLinks.get(addressKey);
        if (nbrLInfo != null) {
            nbrLInfo.setNbrLQ(cost);
            nbrLInfo.setOurNbrLQ(nbrLInfo.getCurrNormalizedLQ());
            nbrLInfo.setNbrLastLQREP(System.currentTimeMillis());
        }
    }

    /* Function executed at the end of each slot duration expiry */
    private void updateLinkInfo() {
        long prevWindow = System.currentTimeMillis() - ConfigLinkParams.TIME_WINDOW;
        NbrLinkInfo nbrLInfo;
        nodeLifetime.updateBytesProcInSlot();
        nodeLifetime.calcNodeEnergyBurnRate();
        /* Update all the avgslot values for the links */
        if (!neighborLinks.isEmpty()) {
            Enumeration en = neighborLinks.elements();
            while (en.hasMoreElements()) {
                nbrLInfo = (NbrLinkInfo)en.nextElement();
                nbrLInfo.updateSlotAvgLQ();
                //Change nextIndex after writing into Slot and firstIndex if wraparound occurs
                
                nbrLInfo.calcTimeWeightedLQ();
//                System.out.println(IEEEAddress.toDottedHex(nbrLInfo.getNbrAddress()) +
//                        " cost = " + (1.0/nbrLInfo.getCurrNormalizedLQ()) +
//                        "  lqi = " + nbrLInfo.getCurrNormalizedLQ());
                // if haven't heard from neighbor or neighbor's LQ has changed by >5% check on them
                if (sender != null && nbrLInfo.getNbrLastLQREQ() > 0 &&
                    ((nbrLInfo.getNbrLastHeard() < prevWindow &&
                      nbrLInfo.getNbrLastHeard() > (prevWindow - ConfigLinkParams.SLOT_SIZE)) ||
                     Math.abs(nbrLInfo.getCurrNormalizedLQ() - nbrLInfo.getOurNbrLQ()) > 0.05)) {
                    sender.sendNewLQREQ(new LQREQ(ourAddress, nbrLInfo.getNbrAddress(), nbrLInfo.getCurrNormalizedLQ()));
                }
            }
        }

        //System.out.println("slotAvgLQ is  " + slotAvgLQ[endIndex] + "\n");
        
    }
    
    public Hashtable getNeighborLinks() {
        return neighborLinks;
    }
    
    public NodeLifetime getNodeLifetime() {
        return nodeLifetime;
    }
    
}
