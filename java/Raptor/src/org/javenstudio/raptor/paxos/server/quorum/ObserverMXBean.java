package org.javenstudio.raptor.paxos.server.quorum;

import org.javenstudio.raptor.paxos.server.PaxosServerMXBean;

/**
 * Observer MX Bean interface, implemented by ObserverBean
 *
 */
public interface ObserverMXBean extends PaxosServerMXBean {
    /**
     * @return count of pending revalidations
     */
    public int getPendingRevalidationCount();
    
    /**
     * @return socket address
     */
    public String getQuorumAddress();
}

