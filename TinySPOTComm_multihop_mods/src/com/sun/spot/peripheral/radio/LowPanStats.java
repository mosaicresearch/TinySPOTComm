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

package com.sun.spot.peripheral.radio;

import com.sun.spot.util.Utils;
import java.util.Date;

/**
 *
 * @author Pete St. Pierre
 */
public class LowPanStats {
    // Time these stats were snapshotted/cloned
    private long timestamp;
    
    // Protocol stats
    /**
     * number of SPOT protocol handlers registered
     */
    protected int protocolCount;
    
    /**
     * number of non-SPOT protocol family handlers registered
     */
    protected int protocolFamilyCount;
    /**
     * number of times we looked for a protocol handler, but failed
     */
    protected int protocolHandlerMissing;
    // Sent packet stats
    /**
     * Number of datagrams sent
     */
    protected int unicastsSent;  
    /**
     * number of datagrams that required fragmentation
     */
    protected int unicastsFragmented;  
    /**
     * non-mesh packets sent
     */
    protected int nonMeshPacketsSent;
    /**
     * mesh packets sent
     */
    protected int meshPacketsSent;
    /**
     * number of packets sent
     **/
    protected int packetsSent;    
    /**
     * number of broadcasts sent
     **/
    protected int broadcastsSent;      
     /**
     * broadcasts received
     */
    protected int broadcastsReceived;      
    /**
     * number of mesh broadcasts sent
     **/
    protected int meshBroadcastsSent;      
     /**
     * broadcasts received
     */
    protected int meshBroadcastsReceived;      
    /**
     * number of mesh broadcasts that required fragmentation (we don't fragment local broadcasts)
     */
    protected int broadcastsFragmented;  
    /**
     * number of packets forwarded through this node
     */
    protected int packetsForwarded;     
    /**
     * number of broadcast packets forwarded
     */
    protected int meshBroadcastsForwarded;    
    /**
     * number of packets intentionally dropped because TTL expired
     */
    protected int ttlExpired;    
    
    /**
     * number of packets intentionally dropped because they didn't meet broadcast seqNo
     * requirements
     */
    protected int droppedBroadcasts;    
    /**
     * number of packets intentionally dropped because we sent them
     */
    protected int broadcastsQueueFull;    
        
    
    // Receive statistics for local node
    /**
     * total mesh packets received
     */
    protected int meshPacketsReceived;
    /**
     * packets received without a mesh routing header (single hop);
     */
    protected int nonMeshPacketsReceived;
    /**
     * number of packets reassembled
     */
    protected int datagramsReassembled;  
    /**
     * number of packets we couldn't reassemble
     */
    protected int reassemblyExpired;         
    /** 
     * number of packet fragments we received
     */
    protected int fragmentsReceived;
    /**
     * full datagrams received
     */
    protected int unicastsReceived;
     
    
    /** Creates a new instance of LowPanStats */
    public LowPanStats() {        
        timestamp = 0;
        unicastsSent = 0;
        unicastsFragmented = 0;
        unicastsReceived = 0;
        broadcastsSent = 0;
        broadcastsReceived = 0;
        meshBroadcastsSent = 0;
        meshBroadcastsReceived = 0;
        broadcastsFragmented = 0;
        meshBroadcastsForwarded = 0;            
        packetsSent = 0;
        packetsForwarded = 0;
        meshPacketsReceived = 0;
        meshPacketsSent = 0;
        nonMeshPacketsReceived = 0;       
        nonMeshPacketsSent = 0;        
        reassemblyExpired = 0;
        ttlExpired = 0;
        datagramsReassembled = 0;
        fragmentsReceived = 0;       
        broadcastsQueueFull = 0;
        droppedBroadcasts = 0;
        protocolCount = 0;
        protocolFamilyCount = 0;
        protocolHandlerMissing = 0;
    }
 /** Creates a new instance of LowPanStats */
    public LowPanStats(byte b[]) {        
        int index = 0;
        timestamp = Utils.readLittleEndLong(b, index); index += 8;
        protocolCount = Utils.readLittleEndInt(b, index); index += 4;
        protocolFamilyCount = Utils.readLittleEndInt(b, index); index += 4;
        protocolHandlerMissing = Utils.readLittleEndInt(b, index); index += 4;
        unicastsSent = Utils.readLittleEndInt(b, index); index += 4;
        unicastsFragmented = Utils.readLittleEndInt(b, index); index += 4;
        nonMeshPacketsSent = Utils.readLittleEndInt(b, index); index += 4;
        meshPacketsSent = Utils.readLittleEndInt(b, index); index += 4;
        packetsSent = Utils.readLittleEndInt(b, index); index += 4;
        broadcastsSent = Utils.readLittleEndInt(b, index); index += 4;
        broadcastsReceived = Utils.readLittleEndInt(b, index); index += 4;
        meshBroadcastsSent = Utils.readLittleEndInt(b, index); index += 4;
        meshBroadcastsReceived = Utils.readLittleEndInt(b, index); index += 4;
        broadcastsFragmented = Utils.readLittleEndInt(b, index); index += 4;
        packetsForwarded = Utils.readLittleEndInt(b, index); index += 4;
        meshBroadcastsForwarded = Utils.readLittleEndInt(b, index); index += 4;
        ttlExpired = Utils.readLittleEndInt(b, index); index += 4;
        droppedBroadcasts = Utils.readLittleEndInt(b, index); index += 4;
        broadcastsQueueFull = Utils.readLittleEndInt(b, index); index += 4;
        meshPacketsReceived = Utils.readLittleEndInt(b, index); index += 4;
        nonMeshPacketsReceived = Utils.readLittleEndInt(b, index); index += 4;
        datagramsReassembled = Utils.readLittleEndInt(b, index); index += 4;
        reassemblyExpired = Utils.readLittleEndInt(b, index); index += 4;
        fragmentsReceived = Utils.readLittleEndInt(b, index); index += 4;
        unicastsReceived = Utils.readLittleEndInt(b, index); index += 4;
}
    public int getUnicastsSent() {
        return unicastsSent;
    }

    public int getUnicastsFragmented() {
        return unicastsFragmented;
    }

    public int getUnicastsReceived() {
        return unicastsReceived;
    }

    public int getBroadcastsSent() {
        return broadcastsSent;
    }

    public int getBroadcastsFragmented() {
        return broadcastsFragmented;
    }
    
    public int getBroadcastsReceived() {
        return broadcastsReceived;
    }

    public int getPacketsSent() {
        return packetsSent;
    }
    
    public int getPacketsForwarded() {
        return packetsForwarded;
    }

    public int getBroadcastsForwarded() {
        return meshBroadcastsForwarded;
    }

    public int getMeshPacketsReceived() {
        return meshPacketsReceived;
    }
    
    public int getMeshPacketsSent() {
        return meshPacketsSent;
    }

    public int getNonMeshPacketsReceived() {
        return nonMeshPacketsReceived;
    }
    
    public int getNonMeshPacketsSent() {
        return nonMeshPacketsSent;
    }
        
    public int getReassemblyExpired() {
        return reassemblyExpired;
    }

    public int getTTLExpired() {
        return ttlExpired;
    }

    public int getDatagramsReassembled() {
        return datagramsReassembled;
    }

    public int getFragmentsReceived() {
        return fragmentsReceived;
    }

    public int getProtocolCount() {
        return protocolCount;
    }

    public int getProtocolFamilyCount() {
        return protocolFamilyCount;
    }

    public int getProtocolHandlerMissing() {
        return protocolHandlerMissing;
    }
    
    public int getDroppedBroadcasts() {
        return droppedBroadcasts;
    }

    public int getDroppedOwnBroadcasts() {
        return broadcastsQueueFull;
    }


    public LowPanStats clone() {
        LowPanStats newObj = new LowPanStats();
        newObj.timestamp = System.currentTimeMillis();
        newObj.unicastsSent = this.unicastsSent;
        newObj.unicastsFragmented = this.unicastsFragmented;
        newObj.unicastsReceived = this.unicastsReceived;
        newObj.broadcastsSent = this.broadcastsSent;
        newObj.broadcastsFragmented = this.broadcastsFragmented;
        newObj.broadcastsReceived = this.broadcastsReceived;
        newObj.packetsSent = this.packetsSent;
        newObj.packetsForwarded = this.packetsForwarded;
        newObj.meshBroadcastsSent = this.meshBroadcastsSent;
        newObj.meshBroadcastsReceived = this.meshBroadcastsReceived;
        newObj.meshBroadcastsForwarded = this.meshBroadcastsForwarded;            
        newObj.meshPacketsReceived = this.meshPacketsReceived;
        newObj.meshPacketsSent = this.meshPacketsSent;
        newObj.nonMeshPacketsReceived = this.nonMeshPacketsReceived;       
        newObj.nonMeshPacketsSent = this.nonMeshPacketsSent;        
        newObj.reassemblyExpired = this.reassemblyExpired;
        newObj.ttlExpired = this.ttlExpired;
        newObj.droppedBroadcasts = this.droppedBroadcasts;
        newObj.broadcastsQueueFull = this.broadcastsQueueFull;
        newObj.datagramsReassembled = this.datagramsReassembled;
        newObj.fragmentsReceived = this.fragmentsReceived;
        newObj.protocolCount = this.protocolCount;
        newObj.protocolFamilyCount = this.protocolFamilyCount;
        newObj.protocolHandlerMissing = this.protocolHandlerMissing;
        
        return newObj;
    }
    
    public byte[] toByteArray() {
        byte b[] = new byte[104];
        int index = 0;
        Utils.writeLittleEndLong(b, index, timestamp); index += 8;
        Utils.writeLittleEndInt(b, index, protocolCount); index += 4;
        Utils.writeLittleEndInt(b, index, protocolFamilyCount); index += 4;
        Utils.writeLittleEndInt(b, index, protocolHandlerMissing); index += 4;
        Utils.writeLittleEndInt(b, index, unicastsSent); index += 4;
        Utils.writeLittleEndInt(b, index, unicastsFragmented); index += 4;
        Utils.writeLittleEndInt(b, index, nonMeshPacketsSent); index += 4;
        Utils.writeLittleEndInt(b, index, meshPacketsSent); index += 4;
        Utils.writeLittleEndInt(b, index, packetsSent); index += 4;
        Utils.writeLittleEndInt(b, index, broadcastsSent); index += 4;
        Utils.writeLittleEndInt(b, index, broadcastsReceived); index += 4;
        Utils.writeLittleEndInt(b, index, meshBroadcastsSent); index += 4;
        Utils.writeLittleEndInt(b, index, meshBroadcastsReceived); index += 4;
        Utils.writeLittleEndInt(b, index, broadcastsFragmented); index += 4;
        Utils.writeLittleEndInt(b, index, packetsForwarded); index += 4;
        Utils.writeLittleEndInt(b, index, meshBroadcastsForwarded); index += 4;
        Utils.writeLittleEndInt(b, index, ttlExpired); index += 4;
        Utils.writeLittleEndInt(b, index, droppedBroadcasts); index += 4;
        Utils.writeLittleEndInt(b, index, broadcastsQueueFull); index += 4;
        Utils.writeLittleEndInt(b, index, meshPacketsReceived); index += 4;
        Utils.writeLittleEndInt(b, index, nonMeshPacketsReceived); index += 4;
        Utils.writeLittleEndInt(b, index, datagramsReassembled); index += 4;
        Utils.writeLittleEndInt(b, index, reassemblyExpired); index += 4;
        Utils.writeLittleEndInt(b, index, fragmentsReceived); index += 4;
        Utils.writeLittleEndInt(b, index, unicastsReceived); index += 4;

        return b;
    }
    
    public String toString() {
        String s = "Timestamp: " + new Date(timestamp).toString() + "\n";
        s+= "unicastsSent: " + unicastsSent + "\n";
        s+= "unicastsFragmented: " + unicastsFragmented + "\n";  
        s+= "unicastsReceived: " + unicastsReceived + "\n";
               
        s+= "broadcastsSent: " + broadcastsSent + "\n";        
        s+= "broadcastsFragmented: " + broadcastsFragmented + "\n";          
        s+= "broadcastsReceived: " + broadcastsReceived + "\n";        

        s+= "meshBroadcastsSent: " + meshBroadcastsSent + "\n";
        s+= "meshBroadcastsReceived: " + meshBroadcastsReceived + "\n";        
        s+= "meshBroadcastsForwarded: " + meshBroadcastsForwarded + "\n";


        s+= "packetsSent: " + packetsSent + "\n";
        s+= "packetsForwarded: " + packetsForwarded + "\n";
       
        s+= "nonMeshPacketsSent: " + nonMeshPacketsSent + "\n";
        s+= "nonMeshPacketsReceived: " + nonMeshPacketsReceived + "\n";
        s+= "meshPacketsSent: " + meshPacketsSent + "\n";
        s+= "meshPacketsReceived: " + meshPacketsReceived + "\n";
        
        s+= "reassemblyExpired: " + reassemblyExpired + "\n";
        s+= "datagramsReassembled: " + datagramsReassembled + "\n";
        s+= "fragmentsRecevied: " + fragmentsReceived + "\n";
        
        s+= "ttlExpired: " + ttlExpired + "\n";       
        s+= "broadcastQueueFull: " + broadcastsQueueFull + "\n";
        s+= "droppedBroadcasts: " + droppedBroadcasts + "\n";  
        
        s+= "protocolCount: " + protocolCount + "\n";
        s+= "protocolfamilyCount: " + protocolFamilyCount + "\n";
        s+= "protocolHandlerMissing: " + protocolHandlerMissing + "\n";
        return s;
    }
}
