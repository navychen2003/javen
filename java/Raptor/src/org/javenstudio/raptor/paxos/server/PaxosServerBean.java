package org.javenstudio.raptor.paxos.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

//import org.javenstudio.raptor.paxos.Version;
import org.javenstudio.raptor.paxos.jmx.PaxosMBeanInfo;

/**
 * This class implements the Paxos server MBean interface.
 */
public class PaxosServerBean implements PaxosServerMXBean, PaxosMBeanInfo {
    private final Date startTime;
    private final String name;
    
    protected final PaxosServer zks;
    
    public PaxosServerBean(PaxosServer zks) {
        startTime = new Date();
        this.zks = zks;
        name = "StandaloneServer_port" + zks.getClientPort();
    }
    
    public String getClientPort() {
        try {
            return InetAddress.getLocalHost().getHostAddress() + ":"
                + zks.getClientPort();
        } catch (UnknownHostException e) {
            return "localhost:" + zks.getClientPort();
        }
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isHidden() {
        return false;
    }
    
    public String getStartTime() {
        return startTime.toString();
    }
    
    public String getVersion() {
        return "0.1"; //Version.getFullVersion();
    }
    
    public long getAvgRequestLatency() {
        return zks.serverStats().getAvgLatency();
    }
    
    public long getMaxRequestLatency() {
        return zks.serverStats().getMaxLatency();
    }
    
    public long getMinRequestLatency() {
        return zks.serverStats().getMinLatency();
    }
    
    public long getOutstandingRequests() {
        return zks.serverStats().getOutstandingRequests();
    }

    public int getTickTime() {
        return zks.getTickTime();
    }

    public void setTickTime(int tickTime) {
        zks.setTickTime(tickTime);
    }

    public int getMaxClientCnxnsPerHost() {
        NIOServerCnxn.Factory fac = zks.getServerCnxnFactory();
        if (fac == null) {
            return -1;
        }
        return fac.getMaxClientCnxns();
    }

    public void setMaxClientCnxnsPerHost(int max) {
        // if fac is null the exception will be propagated to the client
        zks.getServerCnxnFactory().maxClientCnxns = max;
    }

    public int getMinSessionTimeout() {
        return zks.getMinSessionTimeout();
    }

    public void setMinSessionTimeout(int min) {
        zks.setMinSessionTimeout(min);
    }

    public int getMaxSessionTimeout() {
        return zks.getMaxSessionTimeout();
    }

    public void setMaxSessionTimeout(int max) {
        zks.setMaxSessionTimeout(max);
    }

    
    public long getPacketsReceived() {
        return zks.serverStats().getPacketsReceived();
    }
    
    public long getPacketsSent() {
        return zks.serverStats().getPacketsSent();
    }
    
    public void resetLatency() {
        zks.serverStats().resetLatency();
    }
    
    public void resetMaxLatency() {
        zks.serverStats().resetMaxLatency();
    }

    public void resetStatistics() {
        ServerStats serverStats = zks.serverStats();
        serverStats.resetRequestCounters();
        serverStats.resetLatency();
    }
}

