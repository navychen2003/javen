package org.javenstudio.raptor.paxos.server.quorum;

import org.javenstudio.raptor.paxos.server.PaxosServerMXBean;

/**
 * Follower MBean
 */
public interface FollowerMXBean extends PaxosServerMXBean {
    /**
     * @return socket address
     */
    public String getQuorumAddress();
    
    /**
     * @return last queued zxid
     */
    public String getLastQueuedZxid();
    
    /**
     * @return count of pending revalidations
     */
    public int getPendingRevalidationCount();
}

