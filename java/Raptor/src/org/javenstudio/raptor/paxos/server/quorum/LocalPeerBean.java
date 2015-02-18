package org.javenstudio.raptor.paxos.server.quorum;

import org.javenstudio.raptor.paxos.server.NIOServerCnxn;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeer;

/**
 * Implementation of the local peer MBean interface.
 */
public class LocalPeerBean extends ServerBean implements LocalPeerMXBean {
    private final QuorumPeer peer;
    
    public LocalPeerBean(QuorumPeer peer) {
        this.peer = peer;
    }

    public String getName() {
        return "replica." + peer.getId();
    }

    public boolean isHidden() {
        return false;
    }

    public int getTickTime() {
        return peer.getTickTime();
    }
    
    public int getMaxClientCnxnsPerHost() {
        NIOServerCnxn.Factory fac = peer.getCnxnFactory();
        if (fac == null) {
            return -1;
        }
        return fac.getMaxClientCnxns();
    }

    public int getMinSessionTimeout() {
        return peer.getMinSessionTimeout();
    }
    
    public int getMaxSessionTimeout() {
        return peer.getMaxSessionTimeout();
    }
    
    public int getInitLimit() {
        return peer.getInitLimit();
    }
    
    public int getSyncLimit() {
        return peer.getSyncLimit();
    }
    
    public int getTick() {
        return peer.getTick();
    }
    
    public String getState() {
        return peer.getState().toString();
    }
    
    public String getQuorumAddress() {
        return peer.getQuorumAddress().toString();
    }
    
    public int getElectionType() {
        return peer.getElectionType();
    }
}

