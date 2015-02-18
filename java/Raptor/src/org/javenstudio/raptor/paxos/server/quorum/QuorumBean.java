package org.javenstudio.raptor.paxos.server.quorum;

import org.javenstudio.raptor.paxos.jmx.PaxosMBeanInfo;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeer;

public class QuorumBean implements QuorumMXBean, PaxosMBeanInfo {
    private final QuorumPeer peer;
    private final String name;
    
    public QuorumBean(QuorumPeer peer){
        this.peer = peer;
        name = "ReplicatedServer_id" + peer.getMyid();
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isHidden() {
        return false;
    }
    
    public int getQuorumSize() {
        return peer.getQuorumSize();
    }
}

