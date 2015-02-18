package org.javenstudio.raptor.bigdb.master;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.client.Delete;
import org.javenstudio.raptor.bigdb.client.Get;
import org.javenstudio.raptor.bigdb.client.Put;
import org.javenstudio.raptor.bigdb.client.Result;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.util.Writables;

/**
 * Instantiated to enable or disable a table
 */
class ChangeTableState extends TableOperation {
  private static final Logger LOG = Logger.getLogger(ChangeTableState.class);
  private boolean online;
  // Do in order.
  protected final Map<String, HashSet<DBRegionInfo>> servedRegions =
    new TreeMap<String, HashSet<DBRegionInfo>>();
  protected long lockid;

  ChangeTableState(final DBMaster master, final byte [] tableName,
    final boolean onLine)
  throws IOException {
    super(master, tableName);
    this.online = onLine;
  }

  @Override
  protected void processScanItem(String serverName, DBRegionInfo info) {
    if (isBeingServed(serverName)) {
      HashSet<DBRegionInfo> regions = this.servedRegions.get(serverName);
      if (regions == null) {
        regions = new HashSet<DBRegionInfo>();
      }
      regions.add(info);
      this.servedRegions.put(serverName, regions);
    }
  }

  @Override
  protected void postProcessMeta(MetaRegion m, DBRegionInterface server)
  throws IOException {
    // Process regions not being served
    if (LOG.isDebugEnabled()) {
      LOG.debug("Processing unserved regions");
    }
    for (DBRegionInfo i: this.unservedRegions) {
      if (i.isOffline() && i.isSplit()) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Skipping region " + i.toString() +
            " because it is offline and split");
        }
        continue;
      }

      if(!this.online && this.master.getRegionManager().
          isPendingOpen(i.getRegionNameAsString())) {
        LOG.debug("Skipping region " + i.toString() +
          " because it is pending open, will tell it to close later");
        continue;
      }

      // If it's already offline then don't set it a second/third time, skip
      // Same for online, don't set again if already online
      if (!(i.isOffline() && !online) && !(!i.isOffline() && online)) {
        // Update meta table
        Put put = updateRegionInfo(i);
        server.put(m.getRegionName(), put);
        Delete delete = new Delete(i.getRegionName());
        delete.deleteColumns(DBConstants.CATALOG_FAMILY, DBConstants.SERVER_QUALIFIER);
        delete.deleteColumns(DBConstants.CATALOG_FAMILY, DBConstants.STARTCODE_QUALIFIER);
        server.delete(m.getRegionName(), delete);
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Removed server and startcode from row and set online=" +
          this.online + ": " + i.getRegionNameAsString());
      }
      synchronized (master.getRegionManager()) {
        if (this.online) {
          // Bring offline regions on-line
          if (!this.master.getRegionManager().regionIsOpening(i.getRegionNameAsString())) {
            this.master.getRegionManager().setUnassigned(i, false);
          }
        } else {
          // Prevent region from getting assigned.
          this.master.getRegionManager().removeRegion(i);
        }
      }
    }

    // Process regions currently being served
    if (LOG.isDebugEnabled()) {
      LOG.debug("Processing regions currently being served");
    }
    synchronized (this.master.getRegionManager()) {
      for (Map.Entry<String, HashSet<DBRegionInfo>> e:
          this.servedRegions.entrySet()) {
        String serverName = e.getKey();
        if (this.online) {
          LOG.debug("Already online");
          continue;                             // Already being served
        }

        // Cause regions being served to be taken off-line and disabled
        for (DBRegionInfo i: e.getValue()) {
          // The scan we did could be totally staled, get the freshest data
          Get get = new Get(i.getRegionName());
          get.addColumn(DBConstants.CATALOG_FAMILY, DBConstants.SERVER_QUALIFIER);
          Result values = server.get(m.getRegionName(), get);
          String serverAddress = BaseScanner.getServerAddress(values);
          // If this region is unassigned, skip!
          if(serverAddress.length() == 0) {
            continue;
          }
          if (LOG.isDebugEnabled()) {
            LOG.debug("Adding region " + i.getRegionNameAsString() +
              " to setClosing list");
          }
          // this marks the regions to be closed
          this.master.getRegionManager().setClosing(serverName, i, true);
        }
      }
    }
    this.servedRegions.clear();
  }

  protected Put updateRegionInfo(final DBRegionInfo i)
  throws IOException {
    i.setOffline(!online);
    Put put = new Put(i.getRegionName());
    put.add(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER, Writables.getBytes(i));
    return put;
  }
}

