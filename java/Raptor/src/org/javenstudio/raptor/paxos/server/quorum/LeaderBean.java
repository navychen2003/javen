package org.javenstudio.raptor.paxos.server.quorum;

import org.javenstudio.raptor.paxos.server.PaxosServerBean;
import org.javenstudio.raptor.paxos.server.PaxosServer;
import org.javenstudio.raptor.paxos.server.quorum.LearnerHandler;
import org.javenstudio.raptor.paxos.server.quorum.Leader;

/**
 * Leader MBean interface implementation.
 */
public class LeaderBean extends PaxosServerBean implements LeaderMXBean {
    private final Leader leader;
    
    public LeaderBean(Leader leader, PaxosServer zks) {
        super(zks);
        this.leader = leader;
    }
    
    public String getName() {
        return "Leader";
    }

    public String getCurrentZxid() {
        return "0x" + Long.toHexString(zks.getZxid());
    }
    
    public String followerInfo() {
        StringBuilder sb = new StringBuilder();
        for (LearnerHandler handler : leader.learners) {
            sb.append(handler.toString()).append("\n");
        }
        return sb.toString();
    }

}

