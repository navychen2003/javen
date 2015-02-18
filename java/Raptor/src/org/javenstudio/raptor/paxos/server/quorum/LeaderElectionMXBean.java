package org.javenstudio.raptor.paxos.server.quorum;


/**
 * Leader election protocol MBean. 
 */
public interface LeaderElectionMXBean {
    /**
     * 
     * @return the time when the leader election started
     */
    public String getStartTime();
}

