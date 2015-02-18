package org.javenstudio.raptor.paxos.server.quorum;

import org.javenstudio.raptor.paxos.server.PaxosServer;
import org.javenstudio.raptor.paxos.server.PaxosServerBean;

/**
 * Follower MBean inteface implementation
 */
public class FollowerBean extends PaxosServerBean implements FollowerMXBean {
    private final Follower follower;

    public FollowerBean(Follower follower, PaxosServer zks) {
        super(zks);
        this.follower = follower;
    }
    
    public String getName() {
        return "Follower";
    }

    public String getQuorumAddress() {
        return follower.sock.toString();
    }
    
    public String getLastQueuedZxid() {
        return "0x" + Long.toHexString(follower.getLastQueued());
    }
    
    public int getPendingRevalidationCount() {
        return follower.getPendingRevalidationsCount();
    }
}

