package org.javenstudio.raptor.dfs.server.namenode;

import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.util.CyclicIteration;

/**
 * Manage node decommissioning.
 */
class DecommissionManager {
  static final Logger LOG = Logger.getLogger(DecommissionManager.class);

  private final FSNamesystem fsnamesystem;

  DecommissionManager(FSNamesystem namesystem) {
    this.fsnamesystem = namesystem;
  }

  /** Periodically check decommission status. */
  class Monitor implements Runnable {
    /** recheckInterval is how often namenode checks
     *  if a node has finished decommission
     */
    private final long recheckInterval;
    /** The number of decommission nodes to check for each interval */
    private final int numNodesPerCheck;
    /** firstkey can be initialized to anything. */
    private String firstkey = "";

    Monitor(int recheckIntervalInSecond, int numNodesPerCheck) {
      this.recheckInterval = recheckIntervalInSecond * 1000L;
      this.numNodesPerCheck = numNodesPerCheck;
    }

    /**
     * Check decommission status of numNodesPerCheck nodes
     * for every recheckInterval milliseconds.
     */
    public void run() {
      for(; fsnamesystem.isRunning(); ) {
        synchronized(fsnamesystem) {
          check();
        }
  
        try {
          Thread.sleep(recheckInterval);
        } catch (InterruptedException ie) {
          LOG.info("Interrupted " + this.getClass().getSimpleName(), ie);
        }
      }
    }
    
    private void check() {
      int count = 0;
      for(Map.Entry<String, DatanodeDescriptor> entry
          : new CyclicIteration<String, DatanodeDescriptor>(
              fsnamesystem.datanodeMap, firstkey)) {
        final DatanodeDescriptor d = entry.getValue();
        firstkey = entry.getKey();

        if (d.isDecommissionInProgress()) {
          try {
            fsnamesystem.checkDecommissionStateInternal(d);
          } catch(Exception e) {
            LOG.warn("entry=" + entry, e);
          }
          if (++count == numNodesPerCheck) {
            return;
          }
        }
      }
    }
  }
}
