package org.javenstudio.raptor.bigdb.master;

import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.DBServerAddress;
import org.javenstudio.raptor.bigdb.DBServerInfo;
import org.javenstudio.raptor.bigdb.client.Put;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.util.Bytes;

import java.io.IOException;

/**
 * ProcessRegionOpen is instantiated when a region server reports that it is
 * serving a region. This applies to all meta and user regions except the
 * root region which is handled specially.
 */
public class ProcessRegionOpen extends ProcessRegionStatusChange {
  protected final DBServerInfo serverInfo;

  /**
   * @param master
   * @param info
   * @param regionInfo
   */
  public ProcessRegionOpen(DBMaster master, DBServerInfo info,
      DBRegionInfo regionInfo) {
    super(master, regionInfo);
    if (info == null) {
      throw new NullPointerException("DBServerInfo cannot be null; " +
        "bigdb-958 debugging");
    }
    this.serverInfo = info;
  }

  @Override
  public String toString() {
    return "PendingOpenOperation from " + serverInfo.getServerName();
  }

  @Override
  protected boolean process() throws IOException {
    // TODO: The below check is way too convoluted!!!
    if (!metaRegionAvailable()) {
      // We can't proceed unless the meta region we are going to update
      // is online. metaRegionAvailable() has put this operation on the
      // delayedToDoQueue, so return true so the operation is not put
      // back on the toDoQueue
      return true;
    }
    DBRegionInterface server =
        master.getServerConnection().getDBRegionConnection(getMetaRegion().getServer());
    LOG.info(regionInfo.getRegionNameAsString() + " open on " +
      serverInfo.getServerName());

    // Register the newly-available Region's location.
    Put p = new Put(regionInfo.getRegionName());
    p.add(DBConstants.CATALOG_FAMILY, DBConstants.SERVER_QUALIFIER,
      Bytes.toBytes(serverInfo.getHostnamePort()));
    p.add(DBConstants.CATALOG_FAMILY, DBConstants.STARTCODE_QUALIFIER,
      Bytes.toBytes(serverInfo.getStartCode()));
    server.put(metaRegionName, p);
    LOG.info("Updated row " + regionInfo.getRegionNameAsString() +
      " in region " + Bytes.toString(metaRegionName) + " with startcode=" +
      serverInfo.getStartCode() + ", server=" + serverInfo.getHostnamePort());
    synchronized (master.getRegionManager()) {
      if (isMetaTable) {
        // It's a meta region.
        MetaRegion m =
            new MetaRegion(new DBServerAddress(serverInfo.getServerAddress()),
                regionInfo);
        if (!master.getRegionManager().isInitialMetaScanComplete()) {
          // Put it on the queue to be scanned for the first time.
          if (LOG.isDebugEnabled()) {
            LOG.debug("Adding " + m.toString() + " to regions to scan");
          }
          master.getRegionManager().addMetaRegionToScan(m);
        } else {
          // Add it to the online meta regions
          if (LOG.isDebugEnabled()) {
            LOG.debug("Adding to onlineMetaRegions: " + m.toString());
          }
          master.getRegionManager().putMetaRegionOnline(m);
          // Interrupting the Meta Scanner sleep so that it can
          // process regions right away
          master.getRegionManager().metaScannerThread.triggerNow();
        }
      }
      // If updated successfully, remove from pending list if the state
      // is consistent. For example, a disable could be called before the
      // synchronization.
      if(master.getRegionManager().
          isOfflined(regionInfo.getRegionNameAsString())) {
        LOG.warn("We opened a region while it was asked to be closed.");
      } else {
        master.getRegionManager().removeRegion(regionInfo);
      }
      return true;
    }
  }

  @Override
  protected int getPriority() {
    return 0; // highest priority
  }
}

