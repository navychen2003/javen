package org.javenstudio.raptor.bigdb.master;

import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.DBServerInfo;
import org.javenstudio.raptor.bigdb.MasterNotRunningException;
import org.javenstudio.raptor.bigdb.RemoteExceptionHandler;
import org.javenstudio.raptor.bigdb.TableNotFoundException;
import org.javenstudio.raptor.bigdb.client.Result;
import org.javenstudio.raptor.bigdb.client.Scan;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Abstract base class for operations that need to examine all DBRegionInfo
 * objects in a table. (For a table, operate on each of its rows
 * in .META.).
 */
abstract class TableOperation {
  private final Set<MetaRegion> metaRegions;
  protected final byte [] tableName;
  // Do regions in order.
  protected final Set<DBRegionInfo> unservedRegions = new TreeSet<DBRegionInfo>();
  protected DBMaster master;

  protected TableOperation(final DBMaster master, final byte [] tableName)
  throws IOException {
    this.master = master;
    if (!this.master.isMasterRunning()) {
      throw new MasterNotRunningException();
    }
    // add the delimiters.
    // TODO maybe check if this is necessary?
    this.tableName = tableName;

    // Don't wait for META table to come on line if we're enabling it
    if (!Bytes.equals(DBConstants.META_TABLE_NAME, this.tableName)) {
      // We can not access any meta region if they have not already been
      // assigned and scanned.
      if (master.getRegionManager().metaScannerThread.waitForMetaRegionsOrClose()) {
        // We're shutting down. Forget it.
        throw new MasterNotRunningException();
      }
    }
    this.metaRegions = master.getRegionManager().getMetaRegionsForTable(tableName);
  }

  private class ProcessTableOperation extends RetryableMetaOperation<Boolean> {
    ProcessTableOperation(MetaRegion m, DBMaster master) {
      super(m, master);
    }

    public Boolean call() throws IOException {
      boolean tableExists = false;

      // Open a scanner on the meta region
      byte [] tableNameMetaStart =
        Bytes.toBytes(Bytes.toString(tableName) + ",,");
      final Scan scan = new Scan(tableNameMetaStart)
        .addFamily(DBConstants.CATALOG_FAMILY);
      long scannerId = this.server.openScanner(m.getRegionName(), scan);
      int rows = this.master.getConfiguration().
        getInt("bigdb.meta.scanner.caching", 100);
      scan.setCaching(rows);
      List<byte []> emptyRows = new ArrayList<byte []>();
      try {
        while (true) {
          Result values = this.server.next(scannerId);
          if (values == null || values.isEmpty()) {
            break;
          }
          DBRegionInfo info = this.master.getDBRegionInfo(values.getRow(), values);
          if (info == null) {
            emptyRows.add(values.getRow());
            LOG.error(Bytes.toString(DBConstants.CATALOG_FAMILY) + ":"
                + Bytes.toString(DBConstants.REGIONINFO_QUALIFIER)
                + " not found on "
                + Bytes.toStringBinary(values.getRow()));
            continue;
          }
          final String serverAddress = BaseScanner.getServerAddress(values);
          String serverName = null;
          if (serverAddress != null && serverAddress.length() > 0) {
            long startCode = BaseScanner.getStartCode(values);
            serverName = DBServerInfo.getServerName(serverAddress, startCode);
          }
          if (Bytes.compareTo(info.getTableDesc().getName(), tableName) > 0) {
            break; // Beyond any more entries for this table
          }

          tableExists = true;
          if (!isBeingServed(serverName) || !isEnabled(info)) {
            unservedRegions.add(info);
          }
          processScanItem(serverName, info);
        }
      } finally {
        if (scannerId != -1L) {
          try {
            this.server.close(scannerId);
          } catch (IOException e) {
            e = RemoteExceptionHandler.checkIOException(e);
            LOG.error("closing scanner", e);
          }
        }
        scannerId = -1L;
      }

      // Get rid of any rows that have a null DBRegionInfo

      if (emptyRows.size() > 0) {
        LOG.warn("Found " + emptyRows.size() +
            " rows with empty DBRegionInfo while scanning meta region " +
            Bytes.toString(m.getRegionName()));
        master.deleteEmptyMetaRows(server, m.getRegionName(), emptyRows);
      }

      if (!tableExists) {
        throw new TableNotFoundException(Bytes.toString(tableName));
      }

      postProcessMeta(m, server);
      unservedRegions.clear();
      return Boolean.TRUE;
    }
  }

  void process() throws IOException {
    // Prevent meta scanner from running
    synchronized(master.getRegionManager().metaScannerThread.scannerLock) {
      for (MetaRegion m: metaRegions) {
        new ProcessTableOperation(m, master).doWithRetries();
      }
    }
  }

  protected boolean isBeingServed(String serverName) {
    boolean result = false;
    if (serverName != null && serverName.length() > 0) {
      DBServerInfo s = master.getServerManager().getServerInfo(serverName);
      result = s != null;
    }
    return result;
  }

  protected boolean isEnabled(DBRegionInfo info) {
    return !info.isOffline();
  }

  protected abstract void processScanItem(String serverName, DBRegionInfo info)
  throws IOException;

  protected abstract void postProcessMeta(MetaRegion m,
    DBRegionInterface server) throws IOException;
}

