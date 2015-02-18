package org.javenstudio.raptor.ipc.metrics;

import javax.management.ObjectName;

import org.javenstudio.raptor.ipc.Server;
import org.javenstudio.raptor.metrics.util.MBeanUtil;


/**
 * This class implements the RpcMgt MBean
 *
 */
class RpcMgt implements RpcMgtMBean {
  private RpcMetrics myMetrics;
  private Server myServer;
  private ObjectName mbeanName;
  
  RpcMgt(final String serviceName, final String port,
                final RpcMetrics metrics, Server server) {
    myMetrics = metrics;
    myServer = server;
    mbeanName = MBeanUtil.registerMBean(serviceName,
                    "RpcStatisticsForPort" + port, this);
  }

  public void shutdown() {
    if (mbeanName != null)
      MBeanUtil.unregisterMBean(mbeanName);
  }
  
  /**
   * @inheritDoc
   */
  public long getRpcOpsAvgProcessingTime() {
    return myMetrics.rpcProcessingTime.getPreviousIntervalAverageTime();
  }
  
  /**
   * @inheritDoc
   */
  public long getRpcOpsAvgProcessingTimeMax() {
    return myMetrics.rpcProcessingTime.getMaxTime();
  }

  /**
   * @inheritDoc
   */
  public long getRpcOpsAvgProcessingTimeMin() {
    return myMetrics.rpcProcessingTime.getMinTime();
  }

  /**
   * @inheritDoc
   */
  public long getRpcOpsAvgQueueTime() {
    return myMetrics.rpcQueueTime.getPreviousIntervalAverageTime();
  }
  
  /**
   * @inheritDoc
   */
  public long getRpcOpsAvgQueueTimeMax() {
    return myMetrics.rpcQueueTime.getMaxTime();
  }

  /**
   * @inheritDoc
   */
  public long getRpcOpsAvgQueueTimeMin() {
    return myMetrics.rpcQueueTime.getMinTime();
  }

  /**
   * @inheritDoc
   */
  public int getRpcOpsNumber() {
    return myMetrics.rpcProcessingTime.getPreviousIntervalNumOps() ;
  }

  /**
   * @inheritDoc
   */
  public int getNumOpenConnections() {
    return myServer.getNumOpenConnections();
  }
  
  /**
   * @inheritDoc
   */
  public int getCallQueueLen() {
    return myServer.getCallQueueLen();
  }

  /**
   * @inheritDoc
   */
  public void resetAllMinMax() {
    myMetrics.rpcProcessingTime.resetMinMax();
    myMetrics.rpcQueueTime.resetMinMax();
  }
}

