package org.javenstudio.raptor.paxos.server.quorum;

/**
 * An MBean representing a paxos cluster nodes (aka quorum peers)
 */
public interface QuorumMXBean {
    /**
     * @return the name of the quorum
     */
    public String getName();
    
    /**
     * @return configured number of peers in the quorum
     */
    public int getQuorumSize();
}

