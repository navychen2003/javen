package org.javenstudio.raptor.paxos.server.quorum;

/**
 * A proxy for a remote quorum peer.
 */
public interface RemotePeerMXBean {
    /**
     * @return name of the peer
     */
    public String getName();
    /**
     * @return IP address of the quorum peer 
     */
    public String getQuorumAddress();
}

