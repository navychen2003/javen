package org.javenstudio.raptor.paxos.server;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Date;

import javax.management.ObjectName;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.paxos.jmx.MBeanRegistry;
import org.javenstudio.raptor.paxos.jmx.PaxosMBeanInfo;
import org.javenstudio.raptor.paxos.server.NIOServerCnxn.CnxnStats;

/**
 * Implementation of connection MBean interface.
 */
public class ConnectionBean implements ConnectionMXBean, PaxosMBeanInfo {
    private static final Logger LOG = Logger.getLogger(ConnectionBean.class);

    private final ServerCnxn connection;
    private final CnxnStats stats;

    private final PaxosServer zk;
    
    private final String remoteIP;
    private final long sessionId;

    public ConnectionBean(ServerCnxn connection,PaxosServer zk){
        this.connection = connection;
        this.stats = (CnxnStats)connection.getStats();
        this.zk = zk;
        
        InetSocketAddress sockAddr = connection.getRemoteAddress();
        if (sockAddr == null) {
            remoteIP = "Unknown";
        } else {
            InetAddress addr = sockAddr.getAddress();
            if (addr instanceof Inet6Address) {
                remoteIP = ObjectName.quote(addr.getHostAddress());
            } else {
                remoteIP = addr.getHostAddress();
            }
        }
        sessionId = connection.getSessionId();
    }
    
    public String getSessionId() {
        return "0x" + Long.toHexString(sessionId);
    }

    public String getSourceIP() {
        InetSocketAddress sockAddr = connection.getRemoteAddress();
        if (sockAddr == null) {
            return null;
        }
        return sockAddr.getAddress().getHostAddress()
            + ":" + sockAddr.getPort();
    }

    public String getName() {
        return MBeanRegistry.getInstance().makeFullPath("Connections", remoteIP,
                getSessionId());
    }
    
    public boolean isHidden() {
        return false;
    }
    
    public String[] getEphemeralNodes() {
        if(zk.getPaxosDatabase()  !=null){
            String[] res= zk.getPaxosDatabase().getEphemerals(sessionId)
                .toArray(new String[0]);
            Arrays.sort(res);
            return res;
        }
        return null;
    }
    
    public String getStartedTime() {
        return stats.getEstablished().toString();
    }
    
    public void terminateSession() {
        try {
            zk.closeSession(sessionId);
        } catch (Exception e) {
            LOG.warn("Unable to closeSession() for session: 0x" 
                    + getSessionId(), e);
        }
    }
    
    public void terminateConnection() {
        connection.sendCloseSession();
    }

    public void resetCounters() {
        stats.reset();
    }

    @Override
    public String toString() {
        return "ConnectionBean{ClientIP=" + ObjectName.quote(getSourceIP())
            + ",SessionId=0x" + getSessionId() + "}";
    }
    
    public long getOutstandingRequests() {
        return stats.getOutstandingRequests();
    }
    
    public long getPacketsReceived() {
        return stats.getPacketsReceived();
    }
    
    public long getPacketsSent() {
        return stats.getPacketsSent();
    }
    
    public int getSessionTimeout() {
        return connection.getSessionTimeout();
    }

    public long getMinLatency() {
        return stats.getMinLatency();
    }

    public long getAvgLatency() {
        return stats.getAvgLatency();
    }

    public long getMaxLatency() {
        return stats.getMaxLatency();
    }
    
    public String getLastOperation() {
        return stats.getLastOperation();
    }

    public String getLastCxid() {
        return "0x" + Long.toHexString(stats.getLastCxid());
    }

    public String getLastZxid() {
        return "0x" + Long.toHexString(stats.getLastZxid());
    }

    public String getLastResponseTime() {
        return new Date(stats.getLastResponseTime()).toString();
    }

    public long getLastLatency() {
        return stats.getLastLatency();
    }
}

