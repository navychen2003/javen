package org.javenstudio.raptor.dfs.server.datanode.metrics;

import java.io.IOException;

/**
 * 
 * This Interface defines the methods to get the status of a the FSDataset of
 * a data node.
 * It is also used for publishing via JMX (hence we follow the JMX naming
 * convention.) 
 *  * Note we have not used the MetricsDynamicMBeanBase to implement this
 * because the interface for the FSDatasetMBean is stable and should
 * be published as an interface.
 * 
 * <p>
 * Data Node runtime statistic  info is report in another MBean
 * @see org.javenstudio.raptor.dfs.server.datanode.metrics.DataNodeStatisticsMBean
 *
 */
public interface FSDatasetMBean {
  
  /**
   * Returns the total space (in bytes) used by dfs datanode
   * @return  the total space used by dfs datanode
   * @throws IOException
   */  
  public long getDfsUsed() throws IOException;
    
  /**
   * Returns total capacity (in bytes) of storage (used and unused)
   * @return  total capacity of storage (used and unused)
   * @throws IOException
   */
  public long getCapacity() throws IOException;

  /**
   * Returns the amount of free storage space (in bytes)
   * @return The amount of free storage space
   * @throws IOException
   */
  public long getRemaining() throws IOException;
  
  /**
   * Returns the storage id of the underlying storage
   */
  public String getStorageInfo();

}

