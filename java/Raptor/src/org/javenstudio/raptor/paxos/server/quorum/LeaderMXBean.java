package org.javenstudio.raptor.paxos.server.quorum;

import org.javenstudio.raptor.paxos.server.PaxosServerMXBean;

/**
 * Leader MBean.
 */
public interface LeaderMXBean extends PaxosServerMXBean {
    /**
     * Current zxid of cluster.
     */
    public String getCurrentZxid();

    /**
     * @return information on current followers
     */
    public String followerInfo();
}

