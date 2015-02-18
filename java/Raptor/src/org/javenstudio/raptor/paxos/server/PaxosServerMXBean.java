package org.javenstudio.raptor.paxos.server;

/**
 * Paxos server MBean.
 */
public interface PaxosServerMXBean {
    /**
     * @return the server socket port number
     */
    public String getClientPort();
    /**
     * @return the paxos server version
     */
    public String getVersion();
    /**
     * @return time the server was started
     */
    public String getStartTime();
    /**
     * @return min request latency in ms
     */
    public long getMinRequestLatency();
    /**
     * @return average request latency in ms
     */
    public long getAvgRequestLatency();
    /**
     * @return max request latency in ms
     */
    public long getMaxRequestLatency();
    /**
     * @return number of packets received so far
     */
    public long getPacketsReceived();
    /**
     * @return number of packets sent so far
     */
    public long getPacketsSent();
    /**
     * @return number of outstanding requests.
     */
    public long getOutstandingRequests();
    /**
     * Current TickTime of server in milliseconds
     */
    public int getTickTime();
    /**
     * Set TickTime of server in milliseconds
     */
    public void setTickTime(int tickTime);

    /** Current maxClientCnxns allowed from a particular host */
    public int getMaxClientCnxnsPerHost();

    /** Set maxClientCnxns allowed from a particular host */
    public void setMaxClientCnxnsPerHost(int max);

    /**
     * Current minSessionTimeout of the server in milliseconds
     */
    public int getMinSessionTimeout();
    /**
     * Set minSessionTimeout of server in milliseconds
     */
    public void setMinSessionTimeout(int min);

    /**
     * Current maxSessionTimeout of the server in milliseconds
     */
    public int getMaxSessionTimeout();
    /**
     * Set maxSessionTimeout of server in milliseconds
     */
    public void setMaxSessionTimeout(int max);

    /**
     * Reset packet and latency statistics 
     */
    public void resetStatistics();
    /**
     * Reset min/avg/max latency statistics
     */
    public void resetLatency();
    /**
     * Reset max latency statistics only.
     */
    public void resetMaxLatency();
}

