package org.javenstudio.raptor.paxos.server.quorum;

import org.javenstudio.raptor.paxos.jmx.PaxosMBeanInfo;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeer;

/**
 * A remote peer bean only provides limited information about the remote peer,
 * and the peer cannot be managed remotely. 
 */
public class RemotePeerBean implements RemotePeerMXBean,PaxosMBeanInfo {
    private QuorumPeer.QuorumServer peer;
    
    public RemotePeerBean(QuorumPeer.QuorumServer peer){
        this.peer=peer;
    }
    public String getName() {
        return "replica."+peer.id;
    }
    public boolean isHidden() {
        return false;
    }

    public String getQuorumAddress() {
        return peer.addr.getHostName()+":"+peer.addr.getPort();
    }

}

