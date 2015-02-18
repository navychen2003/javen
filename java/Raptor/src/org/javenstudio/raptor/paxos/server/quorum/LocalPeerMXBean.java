package org.javenstudio.raptor.paxos.server.quorum;


/**
 * A local paxos server MBean interface. Unlike the remote peer, the local
 * peer provides complete state/statistics at runtime and can be managed (just 
 * like a standalone paxos server).
 */
public interface LocalPeerMXBean extends ServerMXBean {
    
    /**
     * @return the number of milliseconds of each tick
     */
    public int getTickTime();
    
    /** Current maxClientCnxns allowed from a particular host */
    public int getMaxClientCnxnsPerHost();

    /**
     * @return the minimum number of milliseconds allowed for a session timeout
     */
    public int getMinSessionTimeout();
    
    /**
     * @return the maximum number of milliseconds allowed for a session timeout
     */
    public int getMaxSessionTimeout();
    
    /**
     * @return the number of ticks that the initial sync phase can take
     */
    public int getInitLimit();
    
    /**
     * @return the number of ticks that can pass between sending a request
     * and getting a acknowledgment
     */
    public int getSyncLimit();
    
    /**
     * @return the current tick
     */
    public int getTick();
    
    /**
     * @return the current server state
     */
    public String getState();
    
    /**
     * @return the quorum address
     */
    public String getQuorumAddress();
    
    /**
     * @return the election type
     */
    public int getElectionType();
}

