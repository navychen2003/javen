package org.javenstudio.raptor.paxos.server;

/**
 * Paxos data tree MBean.
 */
public interface DataTreeMXBean {
    /**
     * @return number of znodes in the data tree.
     */
    public int getNodeCount();
    /**
     * @return the most recent zxid processed by the data tree.
     */
    public String getLastZxid();
    /**
     * @return number of watches set.
     */
    public int getWatchCount();
    
    /**
     * @return data tree size in bytes. The size includes the znode path and 
     * its value.
     */
    public long approximateDataSize();
    /**
     * @return number of ephemeral nodes in the data tree
     */
    public int countEphemerals();
}

