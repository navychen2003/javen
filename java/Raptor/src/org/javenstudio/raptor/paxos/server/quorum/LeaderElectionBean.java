package org.javenstudio.raptor.paxos.server.quorum;

import java.util.Date;

import org.javenstudio.raptor.paxos.jmx.PaxosMBeanInfo;

/**
 * Leader election MBean interface implementation
 */
public class LeaderElectionBean implements LeaderElectionMXBean, PaxosMBeanInfo {
    private final Date startTime = new Date();

    public String getName() {
        return "LeaderElection";
    }

    public boolean isHidden() {
        return false;
    }

    public String getStartTime() {
        return startTime.toString();
    }
}

