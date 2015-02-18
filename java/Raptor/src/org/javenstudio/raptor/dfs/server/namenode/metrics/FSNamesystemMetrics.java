package org.javenstudio.raptor.dfs.server.namenode.metrics;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.dfs.server.namenode.FSNamesystem;
import org.javenstudio.raptor.metrics.*;
import org.javenstudio.raptor.metrics.util.MetricsBase;
import org.javenstudio.raptor.metrics.util.MetricsIntValue;
import org.javenstudio.raptor.metrics.util.MetricsLongValue;
import org.javenstudio.raptor.metrics.util.MetricsRegistry;

/**
 * 
 * This class is for maintaining  the various FSNamesystem status metrics
 * and publishing them through the metrics interfaces.
 * The SNamesystem creates and registers the JMX MBean.
 * <p>
 * This class has a number of metrics variables that are publicly accessible;
 * these variables (objects) have methods to update their values;
 *  for example:
 *  <p> {@link #filesTotal}.set()
 *
 */
public class FSNamesystemMetrics implements Updater {
  private static Logger log = Logger.getLogger(FSNamesystemMetrics.class);
  final MetricsRecord metricsRecord;
  public MetricsRegistry registry = new MetricsRegistry();

  final MetricsIntValue filesTotal = new MetricsIntValue("FilesTotal", registry);
  final MetricsLongValue blocksTotal = new MetricsLongValue("BlocksTotal", registry);
  final MetricsIntValue capacityTotalGB = new MetricsIntValue("CapacityTotalGB", registry);
  final MetricsIntValue capacityUsedGB = new MetricsIntValue("CapacityUsedGB", registry);
  final MetricsIntValue capacityRemainingGB = new MetricsIntValue("CapacityRemainingGB", registry);
  final MetricsIntValue totalLoad = new MetricsIntValue("TotalLoad", registry);
  final MetricsIntValue pendingDeletionBlocks = new MetricsIntValue("PendingDeletionBlocks", registry);
  final MetricsIntValue corruptBlocks = new MetricsIntValue("CorruptBlocks", registry);
  final MetricsIntValue excessBlocks = new MetricsIntValue("ExcessBlocks", registry);
  final MetricsIntValue pendingReplicationBlocks = new MetricsIntValue("PendingReplicationBlocks", registry);
  final MetricsIntValue underReplicatedBlocks = new MetricsIntValue("UnderReplicatedBlocks", registry);
  final MetricsIntValue scheduledReplicationBlocks = new MetricsIntValue("ScheduledReplicationBlocks", registry);
  final MetricsIntValue missingBlocks = new MetricsIntValue("MissingBlocks", registry);    
  final MetricsIntValue blockCapacity = new MetricsIntValue("BlockCapacity", registry);
   
  public FSNamesystemMetrics(Configuration conf) {
    String sessionId = conf.get("session.id");
     
    // Create a record for FSNamesystem metrics
    MetricsContext metricsContext = MetricsUtil.getContext("dfs");
    metricsRecord = MetricsUtil.createRecord(metricsContext, "FSNamesystem");
    metricsRecord.setTag("sessionId", sessionId);
    metricsContext.registerUpdater(this);
    log.info("Initializing FSNamesystemMetrics using context object:" +
              metricsContext.getClass().getName());
  }

  private int roundBytesToGBytes(long bytes) {
    return Math.round(((float)bytes/(1024 * 1024 * 1024)));
  }
      
  /**
   * Since this object is a registered updater, this method will be called
   * periodically, e.g. every 5 seconds.
   * We set the metrics value within  this function before pushing it out. 
   * FSNamesystem updates its own local variables which are
   * light weight compared to Metrics counters. 
   *
   * Some of the metrics are explicity casted to int. Few metrics collectors
   * do not handle long values. It is safe to cast to int for now as all these
   * values fit in int value.
   * Metrics related to DFS capacity are stored in bytes which do not fit in 
   * int, so they are rounded to GB
   */
  public void doUpdates(MetricsContext unused) {
    /** 
     * ToFix
     * If the metrics counter were instead stored in the metrics objects themselves
     * we could avoid copying the values on each update.
     */
    synchronized (this) {
      FSNamesystem fsNameSystem = FSNamesystem.getFSNamesystem();
      filesTotal.set((int)fsNameSystem.getFilesTotal());
      blocksTotal.set((int)fsNameSystem.getBlocksTotal());
      capacityTotalGB.set(roundBytesToGBytes(fsNameSystem.getCapacityTotal()));
      capacityUsedGB.set(roundBytesToGBytes(fsNameSystem.getCapacityUsed()));
      capacityRemainingGB.set(roundBytesToGBytes(fsNameSystem.
                                               getCapacityRemaining()));
      totalLoad.set(fsNameSystem.getTotalLoad());
      corruptBlocks.set((int)fsNameSystem.getCorruptReplicaBlocks());
      excessBlocks.set((int)fsNameSystem.getExcessBlocks());
      pendingDeletionBlocks.set((int)fsNameSystem.getPendingDeletionBlocks());
      pendingReplicationBlocks.set((int)fsNameSystem.
                                   getPendingReplicationBlocks());
      underReplicatedBlocks.set((int)fsNameSystem.getUnderReplicatedBlocks());
      scheduledReplicationBlocks.set((int)fsNameSystem.
                                      getScheduledReplicationBlocks());
      missingBlocks.set((int)fsNameSystem.getMissingBlocksCount());
      blockCapacity.set(fsNameSystem.getBlockCapacity());

      for (MetricsBase m : registry.getMetricsList()) {
        m.pushMetric(metricsRecord);
      }
    }
    metricsRecord.update();
  }
}

