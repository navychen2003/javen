package org.javenstudio.raptor.paxos.server.quorum;

import java.util.Date;

import org.javenstudio.raptor.paxos.jmx.PaxosMBeanInfo;

/**
 * An abstract base class for the leader and follower MBeans.
 */
public abstract class ServerBean implements ServerMXBean, PaxosMBeanInfo {
    private final Date startTime=new Date();
    
    public boolean isHidden() {
        return false;
    }

    public String getStartTime() {
        return startTime.toString();
    }
}

