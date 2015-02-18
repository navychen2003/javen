package org.javenstudio.raptor.bigdb.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.bigdb.DBColumnDescriptor;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.client.Delete;
import org.javenstudio.raptor.bigdb.client.Get;
import org.javenstudio.raptor.bigdb.client.DBTable;
import org.javenstudio.raptor.bigdb.client.Put;
import org.javenstudio.raptor.bigdb.client.Result;
import org.javenstudio.raptor.bigdb.client.Scan;
import org.javenstudio.raptor.bigdb.regionserver.DBRegion;
import org.javenstudio.raptor.bigdb.regionserver.InternalScanner;
import org.javenstudio.raptor.bigdb.regionserver.Store;
import org.javenstudio.raptor.bigdb.regionserver.wal.DBLog;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.KeyValue;

/**
 * Contains utility methods for manipulating HBase meta tables.
 * Be sure to call {@link #shutdown()} when done with this class so it closes
 * resources opened during meta processing (ROOT, META, etc.).  Be careful
 * how you use this class.  If used during migrations, be careful when using
 * this class to check whether migration is needed.
 */
public class MetaUtils {
  private static final Logger LOG = Logger.getLogger(MetaUtils.class);
  private final Configuration conf;
  private FileSystem fs;
  private Path rootdir;
  private DBLog log;
  private DBRegion rootRegion;
  private Map<byte [], DBRegion> metaRegions = Collections.synchronizedSortedMap(
    new TreeMap<byte [], DBRegion>(Bytes.BYTES_COMPARATOR));

  /** Default constructor
   * @throws IOException e
   */
  public MetaUtils() throws IOException {
    this(ConfigurationFactory.get());
  }

  /**
   * @param conf Configuration
   * @throws IOException e
   */
  public MetaUtils(Configuration conf) throws IOException {
    this.conf = conf;
    conf.setInt("bigdb.client.retries.number", 1);
    this.rootRegion = null;
    initialize();
  }

  /**
   * Verifies that DFS is available and that HBase is off-line.
   * @throws IOException e
   */
  private void initialize() throws IOException {
    this.fs = FSUtils.getFs(this.conf);
    // Get root directory of HBase installation
    this.rootdir = FSUtils.getRootDir(this.conf);
  }

  /**
   * @return the DBLog
   * @throws IOException e
   */
  public synchronized DBLog getLog() throws IOException {
    if (this.log == null) {
      Path logdir = new Path(this.fs.getHomeDirectory(),
          DBConstants.DBREGION_LOGDIR_NAME + "_" + System.currentTimeMillis());
      Path oldLogDir = new Path(this.fs.getHomeDirectory(),
          DBConstants.DBREGION_OLDLOGDIR_NAME);
      this.log = new DBLog(this.fs, logdir, oldLogDir, this.conf, null);
    }
    return this.log;
  }

  /**
   * @return DBRegion for root region
   * @throws IOException e
   */
  public DBRegion getRootRegion() throws IOException {
    if (this.rootRegion == null) {
      openRootRegion();
    }
    return this.rootRegion;
  }

  /**
   * Open or return cached opened meta region
   *
   * @param metaInfo DBRegionInfo for meta region
   * @return meta DBRegion
   * @throws IOException e
   */
  public DBRegion getMetaRegion(DBRegionInfo metaInfo) throws IOException {
    DBRegion meta = metaRegions.get(metaInfo.getRegionName());
    if (meta == null) {
      meta = openMetaRegion(metaInfo);
      LOG.info("OPENING META " + meta.toString());
      this.metaRegions.put(metaInfo.getRegionName(), meta);
    }
    return meta;
  }

  /**
   * Closes catalog regions if open. Also closes and deletes the DBLog. You
   * must call this method if you want to persist changes made during a
   * MetaUtils edit session.
   */
  public void shutdown() {
    if (this.rootRegion != null) {
      try {
        this.rootRegion.close();
      } catch (IOException e) {
        LOG.error("closing root region", e);
      } finally {
        this.rootRegion = null;
      }
    }
    try {
      for (DBRegion r: metaRegions.values()) {
        LOG.info("CLOSING META " + r.toString());
        r.close();
      }
    } catch (IOException e) {
      LOG.error("closing meta region", e);
    } finally {
      metaRegions.clear();
    }
    try {
      if (this.log != null) {
        this.log.rollWriter();
        this.log.closeAndDelete();
      }
    } catch (IOException e) {
      LOG.error("closing DBLog", e);
    } finally {
      this.log = null;
    }
  }

  /**
   * Used by scanRootRegion and scanMetaRegion to call back the caller so it
   * can process the data for a row.
   */
  public interface ScannerListener {
    /**
     * Callback so client of scanner can process row contents
     *
     * @param info DBRegionInfo for row
     * @return false to terminate the scan
     * @throws IOException e
     */
    public boolean processRow(DBRegionInfo info) throws IOException;
  }

  /**
   * Scans the root region. For every meta region found, calls the listener with
   * the DBRegionInfo of the meta region.
   *
   * @param listener method to be called for each meta region found
   * @throws IOException e
   */
  public void scanRootRegion(ScannerListener listener) throws IOException {
    // Open root region so we can scan it
    if (this.rootRegion == null) {
      openRootRegion();
    }
    scanMetaRegion(this.rootRegion, listener);
  }

  /**
   * Scan the passed in metaregion <code>m</code> invoking the passed
   * <code>listener</code> per row found.
   * @param r region
   * @param listener scanner listener
   * @throws IOException e
   */
  public void scanMetaRegion(final DBRegion r, final ScannerListener listener)
  throws IOException {
    Scan scan = new Scan();
    scan.addColumn(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER);
    InternalScanner s = r.getScanner(scan);
    try {
      List<KeyValue> results = new ArrayList<KeyValue>();
      boolean hasNext = true;
      do {
        hasNext = s.next(results);
        DBRegionInfo info = null;
        for (KeyValue kv: results) {
          info = Writables.getDBRegionInfoOrNull(kv.getValue());
          if (info == null) {
            LOG.warn("Region info is null for row " +
              Bytes.toString(kv.getRow()) + " in table " +
              r.getTableDesc().getNameAsString());
          }
          continue;
        }
        if (!listener.processRow(info)) {
          break;
        }
        results.clear();
      } while (hasNext);
    } finally {
      s.close();
    }
  }

  /**
   * Scans a meta region. For every region found, calls the listener with
   * the DBRegionInfo of the region.
   * TODO: Use Visitor rather than Listener pattern.  Allow multiple Visitors.
   * Use this everywhere we scan meta regions: e.g. in metascanners, in close
   * handling, etc.  Have it pass in the whole row, not just DBRegionInfo.
   * <p>Use for reading meta only.  Does not close region when done.
   * Use {@link #getMetaRegion(DBRegionInfo)} instead if writing.  Adds
   * meta region to list that will get a close on {@link #shutdown()}.
   *
   * @param metaRegionInfo DBRegionInfo for meta region
   * @param listener method to be called for each meta region found
   * @throws IOException e
   */
  public void scanMetaRegion(DBRegionInfo metaRegionInfo,
    ScannerListener listener)
  throws IOException {
    // Open meta region so we can scan it
    DBRegion metaRegion = openMetaRegion(metaRegionInfo);
    scanMetaRegion(metaRegion, listener);
  }

  private synchronized DBRegion openRootRegion() throws IOException {
    if (this.rootRegion != null) {
      return this.rootRegion;
    }
    this.rootRegion = DBRegion.openDBRegion(DBRegionInfo.ROOT_REGIONINFO,
      this.rootdir, getLog(), this.conf);
    this.rootRegion.compactStores();
    return this.rootRegion;
  }

  private DBRegion openMetaRegion(DBRegionInfo metaInfo) throws IOException {
    DBRegion meta =
      DBRegion.openDBRegion(metaInfo, this.rootdir, getLog(), this.conf);
    meta.compactStores();
    return meta;
  }

  /**
   * Set a single region on/offline.
   * This is a tool to repair tables that have offlined tables in their midst.
   * Can happen on occasion.  Use at your own risk.  Call from a bit of java
   * or jython script.  This method is 'expensive' in that it creates a
   * {@link DBTable} instance per invocation to go against <code>.META.</code>
   * @param c A configuration that has its <code>bigdb.master</code>
   * properly set.
   * @param row Row in the catalog .META. table whose DBRegionInfo's offline
   * status we want to change.
   * @param onlineOffline Pass <code>true</code> to OFFLINE the region.
   * @throws IOException e
   */
  public static void changeOnlineStatus (final Configuration c,
      final byte [] row, final boolean onlineOffline)
  throws IOException {
    DBTable t = new DBTable(c, DBConstants.META_TABLE_NAME);
    Get get = new Get(row);
    get.addColumn(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER);
    Result res = t.get(get);
    KeyValue [] kvs = res.raw();
    if(kvs.length <= 0) {
      throw new IOException("no information for row " + Bytes.toString(row));
    }
    byte [] value = kvs[0].getValue();
    if (value == null) {
      throw new IOException("no information for row " + Bytes.toString(row));
    }
    DBRegionInfo info = Writables.getDBRegionInfo(value);
    Put put = new Put(row);
    info.setOffline(onlineOffline);
    put.add(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER,
        Writables.getBytes(info));
    t.put(put);

    Delete delete = new Delete(row);
    delete.deleteColumns(DBConstants.CATALOG_FAMILY, DBConstants.SERVER_QUALIFIER);
    delete.deleteColumns(DBConstants.CATALOG_FAMILY,
        DBConstants.STARTCODE_QUALIFIER);

    t.delete(delete);
  }

  /**
   * Offline version of the online TableOperation,
   * org.javenstudio.raptor.bigdb.master.AddColumn.
   * @param tableName table name
   * @param hcd Add this column to <code>tableName</code>
   * @throws IOException e
   */
  public void addColumn(final byte [] tableName,
      final DBColumnDescriptor hcd)
  throws IOException {
    List<DBRegionInfo> metas = getMETARows(tableName);
    for (DBRegionInfo hri: metas) {
      final DBRegion m = getMetaRegion(hri);
      scanMetaRegion(m, new ScannerListener() {
        private boolean inTable = true;

        @SuppressWarnings("synthetic-access")
        public boolean processRow(DBRegionInfo info) throws IOException {
          LOG.debug("Testing " + Bytes.toString(tableName) + " against " +
            Bytes.toString(info.getTableDesc().getName()));
          if (Bytes.equals(info.getTableDesc().getName(), tableName)) {
            this.inTable = false;
            info.getTableDesc().addFamily(hcd);
            updateMETARegionInfo(m, info);
            return true;
          }
          // If we got here and we have not yet encountered the table yet,
          // inTable will be false.  Otherwise, we've passed out the table.
          // Stop the scanner.
          return this.inTable;
        }});
    }
  }

  /**
   * Offline version of the online TableOperation,
   * org.javenstudio.raptor.bigdb.master.DeleteColumn.
   * @param tableName table name
   * @param columnFamily Name of column name to remove.
   * @throws IOException e
   */
  public void deleteColumn(final byte [] tableName,
      final byte [] columnFamily) throws IOException {
    List<DBRegionInfo> metas = getMETARows(tableName);
    for (DBRegionInfo hri: metas) {
      final DBRegion m = getMetaRegion(hri);
      scanMetaRegion(m, new ScannerListener() {
        private boolean inTable = true;

        @SuppressWarnings("synthetic-access")
        public boolean processRow(DBRegionInfo info) throws IOException {
          if (Bytes.equals(info.getTableDesc().getName(), tableName)) {
            this.inTable = false;
            info.getTableDesc().removeFamily(columnFamily);
            updateMETARegionInfo(m, info);
            Path tabledir = new Path(rootdir,
              info.getTableDesc().getNameAsString());
            Path p = Store.getStoreHomedir(tabledir, info.getEncodedName(),
              columnFamily);
            if (!fs.delete(p, true)) {
              LOG.warn("Failed delete of " + p);
            }
            return false;
          }
          // If we got here and we have not yet encountered the table yet,
          // inTable will be false.  Otherwise, we've passed out the table.
          // Stop the scanner.
          return this.inTable;
        }});
    }
  }

  /**
   * Update COL_REGIONINFO in meta region r with DBRegionInfo hri
   *
   * @param r region
   * @param hri region info
   * @throws IOException e
   */
  public void updateMETARegionInfo(DBRegion r, final DBRegionInfo hri)
  throws IOException {
    if (LOG.isDebugEnabled()) {
      Get get = new Get(hri.getRegionName());
      get.addColumn(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER);
      Result res = r.get(get, null);
      KeyValue [] kvs = res.raw();
      if(kvs.length <= 0) {
        return;
      }
      byte [] value = kvs[0].getValue();
      if (value == null) {
        return;
      }
      DBRegionInfo h = Writables.getDBRegionInfoOrNull(value);

      LOG.debug("Old " + Bytes.toString(DBConstants.CATALOG_FAMILY) + ":" +
          Bytes.toString(DBConstants.REGIONINFO_QUALIFIER) + " for " +
          hri.toString() + " in " + r.toString() + " is: " + h.toString());
    }

    Put put = new Put(hri.getRegionName());
    put.add(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER,
        Writables.getBytes(hri));
    r.put(put);

    if (LOG.isDebugEnabled()) {
      Get get = new Get(hri.getRegionName());
      get.addColumn(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER);
      Result res = r.get(get, null);
      KeyValue [] kvs = res.raw();
      if(kvs.length <= 0) {
        return;
      }
      byte [] value = kvs[0].getValue();
      if (value == null) {
        return;
      }
      DBRegionInfo h = Writables.getDBRegionInfoOrNull(value);
        LOG.debug("New " + Bytes.toString(DBConstants.CATALOG_FAMILY) + ":" +
            Bytes.toString(DBConstants.REGIONINFO_QUALIFIER) + " for " +
            hri.toString() + " in " + r.toString() + " is: " +  h.toString());
    }
  }

  /**
   * @return List of {@link DBRegionInfo} rows found in the ROOT or META
   * catalog table.
   * @param tableName Name of table to go looking for.
   * @throws IOException e
   * @see #getMetaRegion(DBRegionInfo)
   */
  public List<DBRegionInfo> getMETARows(final byte [] tableName)
  throws IOException {
    final List<DBRegionInfo> result = new ArrayList<DBRegionInfo>();
    // If passed table name is META, then  return the root region.
    if (Bytes.equals(DBConstants.META_TABLE_NAME, tableName)) {
      result.add(openRootRegion().getRegionInfo());
      return result;
    }
    // Return all meta regions that contain the passed tablename.
    scanRootRegion(new ScannerListener() {
      private final Logger SL_LOG = Logger.getLogger(this.getClass());

      public boolean processRow(DBRegionInfo info) throws IOException {
        SL_LOG.debug("Testing " + info);
        if (Bytes.equals(info.getTableDesc().getName(),
            DBConstants.META_TABLE_NAME)) {
          result.add(info);
          return false;
        }
        return true;
      }});
    return result;
  }

  /**
   * @param n Table name.
   * @return True if a catalog table, -ROOT- or .META.
   */
  public static boolean isMetaTableName(final byte [] n) {
    return Bytes.equals(n, DBConstants.ROOT_TABLE_NAME) ||
      Bytes.equals(n, DBConstants.META_TABLE_NAME);
  }
}

