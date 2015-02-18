package org.javenstudio.raptor.bigdb.client;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CopyOnWriteArraySet;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.ipc.RemoteException;
import org.javenstudio.raptor.bigdb.DoNotRetryIOException;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.DBRegionLocation;
import org.javenstudio.raptor.bigdb.DBServerAddress;
import org.javenstudio.raptor.bigdb.DBTableDescriptor;
import org.javenstudio.raptor.bigdb.KeyValue;
import org.javenstudio.raptor.bigdb.MasterNotRunningException;
import org.javenstudio.raptor.bigdb.NotServingRegionException;
import org.javenstudio.raptor.bigdb.RemoteExceptionHandler;
import org.javenstudio.raptor.bigdb.TableNotFoundException;
import org.javenstudio.raptor.bigdb.client.MetaScanner.MetaScannerVisitor;
import org.javenstudio.raptor.bigdb.ipc.DBRPC;
import org.javenstudio.raptor.bigdb.ipc.DBRPCProtocolVersion;
import org.javenstudio.raptor.bigdb.ipc.DBMasterInterface;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.bigdb.util.MetaUtils;
import org.javenstudio.raptor.bigdb.util.SoftValueSortedMap;
import org.javenstudio.raptor.bigdb.util.Writables;
import org.javenstudio.raptor.bigdb.paxos.PaxosWrapper;
import org.javenstudio.raptor.paxos.WatchedEvent;
import org.javenstudio.raptor.paxos.Watcher;
import org.javenstudio.raptor.paxos.Watcher.Event.PaxosState;

/**
 * A non-instantiable class that manages connections to multiple tables in
 * multiple HBase instances.
 *
 * Used by {@link HTable} and {@link HBaseAdmin}
 */
@SuppressWarnings("serial")
public class DBConnectionManager {
  // Register a shutdown hook, one that cleans up RPC and closes zk sessions.
  static {
    Runtime.getRuntime().addShutdownHook(new Thread("HCM.shutdownHook") {
      @Override
      public void run() {
        DBConnectionManager.deleteAllConnections(true);
      }
    });
  }

  /*
   * Not instantiable.
   */
  protected DBConnectionManager() {
    super();
  }

  private static final int MAX_CACHED_BIGDB_INSTANCES=31;
  // A LRU Map of master Configuration -> connection information for that
  // instance. The objects it contains are mutable and hence require
  // synchronized access to them.  We set instances to 31.  The zk default max
  // connections is 30 so should run into zk issues before hit this value of 31.
  private static
  final Map<Integer, TableServers> BIGDB_INSTANCES =
    new LinkedHashMap<Integer, TableServers>
      ((int) (MAX_CACHED_BIGDB_INSTANCES/0.75F)+1, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<Integer, TableServers> eldest) {
        return size() > MAX_CACHED_BIGDB_INSTANCES;
      }
  };

  private static final Map<String, ClientPaxosWatcher> PAXOS_WRAPPERS =
    new HashMap<String, ClientPaxosWatcher>();

  /**
   * Get the connection object for the instance specified by the configuration
   * If no current connection exists, create a new connection for that instance
   * @param conf configuration
   * @return DBConnection object for the instance specified by the configuration
   */
  @SuppressWarnings("deprecation")
  public static DBConnection getConnection(Configuration conf) {
    TableServers connection;
    Integer key = conf.hashCode();
    synchronized (BIGDB_INSTANCES) {
      connection = BIGDB_INSTANCES.get(key);
      if (connection == null) {
        connection = new TableServers(conf);
        BIGDB_INSTANCES.put(key, connection);
      }
    }
    return connection;
  }

  /**
   * Delete connection information for the instance specified by configuration
   * @param conf configuration
   * @param stopProxy stop the proxy as well
   */
  @SuppressWarnings("deprecation")
  public static void deleteConnectionInfo(Configuration conf,
      boolean stopProxy) {
    synchronized (BIGDB_INSTANCES) {
      Integer key = conf.hashCode();
      TableServers t = BIGDB_INSTANCES.remove(key);
      if (t != null) {
        t.close(stopProxy);
      }
    }
  }

  /**
   * Delete information for all connections.
   * @param stopProxy stop the proxy as well
   */
  public static void deleteAllConnections(boolean stopProxy) {
    synchronized (BIGDB_INSTANCES) {
      for (TableServers t : BIGDB_INSTANCES.values()) {
        if (t != null) {
          t.close(stopProxy);
        }
      }
    }
    synchronized (PAXOS_WRAPPERS) {
      for (ClientPaxosWatcher watch : PAXOS_WRAPPERS.values()) {
        watch.resetPaxos();
      }
    }
  }

  /**
   * Get a watcher of a paxos connection for a given quorum address.
   * If the connection isn't established, a new one is created.
   * This acts like a multiton.
   * @param conf configuration
   * @return ZKW watcher
   * @throws IOException if a remote or network exception occurs
   */
  public static synchronized ClientPaxosWatcher getClientPaxosWatcher(
      Configuration conf) throws IOException {
    if (!PAXOS_WRAPPERS.containsKey(
        PaxosWrapper.getPaxosClusterKey(conf))) {
      PAXOS_WRAPPERS.put(PaxosWrapper.getPaxosClusterKey(conf),
          new ClientPaxosWatcher(conf));
    }
    return PAXOS_WRAPPERS.get(PaxosWrapper.getPaxosClusterKey(conf));
  }

  /**
   * This class is responsible to handle connection and reconnection
   * to a paxos quorum.
   *
   */
  public static class ClientPaxosWatcher implements Watcher {

    static final Logger LOG = Logger.getLogger(ClientPaxosWatcher.class);
    private PaxosWrapper paxosWrapper;
    private Configuration conf;

    /**
     * Takes a configuration to pass it to ZKW but won't instanciate it
     * @param conf configuration
     */
    public ClientPaxosWatcher(Configuration conf) {
      this.conf = conf;
    }

    /**
     * Called by Paxos when an event occurs on our connection. We use this to
     * detect our session expiring. When our session expires, we have lost our
     * connection to Paxos. Our handle is dead, and we need to recreate it.
     *
     * See http://hadoop.apache.org/paxos/docs/current/paxosProgrammers.html#ch_zkSessions
     * for more information.
     *
     * @param event WatchedEvent witnessed by Paxos.
     */
    public void process(final WatchedEvent event) {
      final PaxosState state = event.getState();
      if (!state.equals(PaxosState.SyncConnected)) {
        LOG.warn("No longer connected to Paxos, current state: " + state);
        resetPaxos();
      }
    }

    /**
     * Get this watcher's ZKW, instantiate it if necessary.
     * @return ZKW
     * @throws java.io.IOException if a remote or network exception occurs
     */
    public synchronized PaxosWrapper getPaxosWrapper() throws IOException {
      if (paxosWrapper == null) {
        paxosWrapper =
            PaxosWrapper.createInstance(conf, DBConnectionManager.class.getName());
        paxosWrapper.registerListener(this);
      }
      return paxosWrapper;
    }

    /**
     * Clear this connection to paxos.
     */
    private synchronized void resetPaxos() {
      if (paxosWrapper != null) {
        paxosWrapper.close();
        paxosWrapper = null;
      }
    }
  }

  /**
   * It is provided for unit test cases which verify the behavior of region
   * location cache prefetch.
   * @return Number of cached regions for the table.
   */
  static int getCachedRegionCount(Configuration conf,
      byte[] tableName) {
    TableServers connection = (TableServers)getConnection(conf);
    return connection.getNumberOfCachedRegionLocations(tableName);
  }

  /**
   * It's provided for unit test cases which verify the behavior of region
   * location cache prefetch.
   * @return true if the region where the table and row reside is cached.
   */
  static boolean isRegionCached(Configuration conf,
      byte[] tableName, byte[] row) {
    TableServers connection = (TableServers)getConnection(conf);
    return connection.isRegionCached(tableName, row);
  }

  /* Encapsulates finding the servers for an HBase instance */
  static class TableServers implements ServerConnection {
    static final Logger LOG = Logger.getLogger(TableServers.class);
    private final Class<? extends DBRegionInterface> serverInterfaceClass;
    private final long pause;
    private final int numRetries;
    private final int maxRPCAttempts;
    private final long rpcTimeout;
    private final int prefetchRegionLimit;

    private final Object masterLock = new Object();
    private volatile boolean closed;
    private volatile DBMasterInterface master;
    private volatile boolean masterChecked;

    private final Object rootRegionLock = new Object();
    private final Object metaRegionLock = new Object();
    private final Object userRegionLock = new Object();

    private volatile Configuration conf;

    // Known region DBServerAddress.toString() -> DBRegionInterface
    private final Map<String, DBRegionInterface> servers =
      new ConcurrentHashMap<String, DBRegionInterface>();

    // Used by master and region servers during safe mode only
    private volatile DBRegionLocation rootRegionLocation;

    private final Map<Integer, SoftValueSortedMap<byte [], DBRegionLocation>>
      cachedRegionLocations =
        new HashMap<Integer, SoftValueSortedMap<byte [], DBRegionLocation>>();

    // region cache prefetch is enabled by default. this set contains all
    // tables whose region cache prefetch are disabled.
    private final Set<Integer> regionCachePrefetchDisabledTables =
      new CopyOnWriteArraySet<Integer>();

    /**
     * constructor
     * @param conf Configuration object
     */
    @SuppressWarnings("unchecked")
    public TableServers(Configuration conf) {
      this.conf = conf;

      String serverClassName =
        conf.get(DBConstants.REGION_SERVER_CLASS,
            DBConstants.DEFAULT_REGION_SERVER_CLASS);

      this.closed = false;

      try {
        this.serverInterfaceClass =
          (Class<? extends DBRegionInterface>) Class.forName(serverClassName);

      } catch (ClassNotFoundException e) {
        throw new UnsupportedOperationException(
            "Unable to find region server interface " + serverClassName, e);
      }

      this.pause = conf.getLong("bigdb.client.pause", 1000);
      this.numRetries = conf.getInt("bigdb.client.retries.number", 10);
      this.maxRPCAttempts = conf.getInt("bigdb.client.rpc.maxattempts", 1);
      this.rpcTimeout = conf.getLong(
          DBConstants.BIGDB_REGIONSERVER_LEASE_PERIOD_KEY,
          DBConstants.DEFAULT_BIGDB_REGIONSERVER_LEASE_PERIOD);

      this.prefetchRegionLimit = conf.getInt("bigdb.client.prefetch.limit",
          10);

      this.master = null;
      this.masterChecked = false;
    }

    private long getPauseTime(int tries) {
      int ntries = tries;
      if (ntries >= DBConstants.RETRY_BACKOFF.length)
        ntries = DBConstants.RETRY_BACKOFF.length - 1;
      return this.pause * DBConstants.RETRY_BACKOFF[ntries];
    }

    // Used by master and region servers during safe mode only
    public void unsetRootRegionLocation() {
      this.rootRegionLocation = null;
    }

    // Used by master and region servers during safe mode only
    public void setRootRegionLocation(DBRegionLocation rootRegion) {
      if (rootRegion == null) {
        throw new IllegalArgumentException(
            "Cannot set root region location to null.");
      }
      this.rootRegionLocation = rootRegion;
    }

    public DBMasterInterface getMaster() throws MasterNotRunningException {
      PaxosWrapper zk;
      try {
        zk = getPaxosWrapper();
      } catch (IOException e) {
        throw new MasterNotRunningException(e);
      }

      DBServerAddress masterLocation = null;
      Exception exception = null;
      
      synchronized (this.masterLock) {
        for (int tries = 0;
          !this.closed &&
          !this.masterChecked && this.master == null &&
          tries < numRetries;
        tries++) {

          try {
            masterLocation = zk.readMasterAddressOrThrow();

            DBMasterInterface tryMaster = (DBMasterInterface)DBRPC.getProxy(
                DBMasterInterface.class, DBRPCProtocolVersion.versionID,
                masterLocation.getInetSocketAddress(), this.conf);

            if (tryMaster.isMasterRunning()) {
              this.master = tryMaster;
              this.masterLock.notifyAll();
              break;
            }

          } catch (IOException e) {
            if (tries == numRetries - 1) {
              // This was our last chance - don't bother sleeping
              if (LOG.isWarnEnabled()) {
                LOG.warn("getMaster attempt " + tries + " of " + this.numRetries +
                  " failed; no more retrying.", e);
              }
              exception = e;
              break;
            }
            if (LOG.isWarnEnabled()) {
              LOG.info("getMaster attempt " + tries + " of " + this.numRetries +
                " failed; retrying after sleep of " + getPauseTime(tries), e);
            }
          }

          // Cannot connect to master or it is not running. Sleep & retry
          try {
            this.masterLock.wait(getPauseTime(tries));
          } catch (InterruptedException e) {
            // continue
          }
        }
        this.masterChecked = true;
      }
      if (this.master == null) {
        if (masterLocation == null) 
          throw new MasterNotRunningException(exception);
        else
          throw new MasterNotRunningException(masterLocation.toString(), exception);
      }
      return this.master;
    }

    public boolean isMasterRunning() {
      if (this.master == null) {
        try {
          getMaster();

        } catch (MasterNotRunningException e) {
          return false;
        }
      }
      return true;
    }

    public boolean tableExists(final byte [] tableName)
    throws MasterNotRunningException {
      getMaster();
      if (tableName == null) {
        throw new IllegalArgumentException("Table name cannot be null");
      }
      if (isMetaTableName(tableName)) {
        return true;
      }
      boolean exists = false;
      try {
        DBTableDescriptor[] tables = listTables();
        for (DBTableDescriptor table : tables) {
          if (Bytes.equals(table.getName(), tableName)) {
            exists = true;
          }
        }
      } catch (IOException e) {
        LOG.warn("Testing for table existence threw exception", e);
      }
      return exists;
    }

    /**
     * @param n
     * @return Truen if passed tablename <code>n</code> is equal to the name
     * of a catalog table.
     */
    private static boolean isMetaTableName(final byte [] n) {
      return MetaUtils.isMetaTableName(n);
    }

    public DBRegionLocation getRegionLocation(final byte [] name,
        final byte [] row, boolean reload)
    throws IOException {
      return reload? relocateRegion(name, row): locateRegion(name, row);
    }

    public DBTableDescriptor[] listTables() throws IOException {
      getMaster();
      final TreeSet<DBTableDescriptor> uniqueTables =
        new TreeSet<DBTableDescriptor>();
      MetaScannerVisitor visitor = new MetaScannerVisitor() {
        public boolean processRow(Result result) throws IOException {
          try {
            byte[] value = result.getValue(DBConstants.CATALOG_FAMILY,
                DBConstants.REGIONINFO_QUALIFIER);
            DBRegionInfo info = null;
            if (value != null) {
              info = Writables.getDBRegionInfo(value);
            }
            // Only examine the rows where the startKey is zero length
            if (info != null && info.getStartKey().length == 0) {
              uniqueTables.add(info.getTableDesc());
            }
            return true;
          } catch (RuntimeException e) {
            LOG.error("Result=" + result);
            throw e;
          }
        }
      };
      MetaScanner.metaScan(conf, visitor);

      return uniqueTables.toArray(new DBTableDescriptor[uniqueTables.size()]);
    }

    public boolean isTableEnabled(byte[] tableName) throws IOException {
      return testTableOnlineState(tableName, true);
    }

    public boolean isTableDisabled(byte[] tableName) throws IOException {
      return testTableOnlineState(tableName, false);
    }

    public boolean isTableAvailable(final byte[] tableName) throws IOException {
      final AtomicBoolean available = new AtomicBoolean(true);
      MetaScannerVisitor visitor = new MetaScannerVisitor() {
        @Override
        public boolean processRow(Result row) throws IOException {
          byte[] value = row.getValue(DBConstants.CATALOG_FAMILY,
              DBConstants.REGIONINFO_QUALIFIER);
          DBRegionInfo info = Writables.getDBRegionInfoOrNull(value);
          if (info != null) {
            if (Bytes.equals(tableName, info.getTableDesc().getName())) {
              value = row.getValue(DBConstants.CATALOG_FAMILY,
                  DBConstants.SERVER_QUALIFIER);
              if (value == null) {
                available.set(false);
                return false;
              }
            }
          }
          return true;
        }
      };
      MetaScanner.metaScan(conf, visitor);
      return available.get();
    }

    /**
     * If online == true
     *   Returns true if all regions are online
     *   Returns false in any other case
     * If online == false
     *   Returns true if all regions are offline
     *   Returns false in any other case
     */
    private boolean testTableOnlineState(byte[] tableName, boolean online)
    throws IOException {
      if (!tableExists(tableName)) {
        throw new TableNotFoundException(Bytes.toString(tableName));
      }
      if (Bytes.equals(tableName, DBConstants.ROOT_TABLE_NAME)) {
        // The root region is always enabled
        return true;
      }
      int rowsScanned = 0;
      int rowsOffline = 0;
      byte[] startKey =
        DBRegionInfo.createRegionName(tableName, null, DBConstants.ZEROES, false);
      byte[] endKey;
      DBRegionInfo currentRegion;
      Scan scan = new Scan(startKey);
      scan.addColumn(DBConstants.CATALOG_FAMILY,
          DBConstants.REGIONINFO_QUALIFIER);
      int rows = this.conf.getInt("bigdb.meta.scanner.caching", 100);
      scan.setCaching(rows);
      ScannerCallable s = new ScannerCallable(this,
          (Bytes.equals(tableName, DBConstants.META_TABLE_NAME) ?
              DBConstants.ROOT_TABLE_NAME : DBConstants.META_TABLE_NAME), scan);
      try {
        // Open scanner
        getRegionServerWithRetries(s);
        do {
          currentRegion = s.getDBRegionInfo();
          Result r;
          Result [] rrs;
          while ((rrs = getRegionServerWithRetries(s)) != null && rrs.length > 0) {
            r = rrs[0];
            byte [] value = r.getValue(DBConstants.CATALOG_FAMILY,
              DBConstants.REGIONINFO_QUALIFIER);
            if (value != null) {
              DBRegionInfo info = Writables.getDBRegionInfoOrNull(value);
              if (info != null) {
                if (Bytes.equals(info.getTableDesc().getName(), tableName)) {
                  rowsScanned += 1;
                  rowsOffline += info.isOffline() ? 1 : 0;
                }
              }
            }
          }
          endKey = currentRegion.getEndKey();
        } while (!(endKey == null ||
            Bytes.equals(endKey, DBConstants.EMPTY_BYTE_ARRAY)));
      } finally {
        s.setClose();
        // Doing below will call 'next' again and this will close the scanner
        // Without it we leave scanners open.
        getRegionServerWithRetries(s);
      }
      LOG.debug("Rowscanned=" + rowsScanned + ", rowsOffline=" + rowsOffline);
      boolean onOffLine = online? rowsOffline == 0: rowsOffline == rowsScanned;
      return rowsScanned > 0 && onOffLine;
    }

    private static class DBTableDescriptorFinder
    implements MetaScanner.MetaScannerVisitor {
        byte[] tableName;
        DBTableDescriptor result;
        protected DBTableDescriptorFinder(byte[] tableName) {
          this.tableName = tableName;
        }
        public boolean processRow(Result rowResult) throws IOException {
          DBRegionInfo info = Writables.getDBRegionInfo(
              rowResult.getValue(DBConstants.CATALOG_FAMILY,
                  DBConstants.REGIONINFO_QUALIFIER));
          DBTableDescriptor desc = info.getTableDesc();
          if (Bytes.compareTo(desc.getName(), tableName) == 0) {
            result = desc;
            return false;
          }
          return true;
        }
        DBTableDescriptor getResult() {
          return result;
        }
    }

    public DBTableDescriptor getDBTableDescriptor(final byte[] tableName)
    throws IOException {
      if (Bytes.equals(tableName, DBConstants.ROOT_TABLE_NAME)) {
        return new UnmodifyableDBTableDescriptor(DBTableDescriptor.ROOT_TABLEDESC);
      }
      if (Bytes.equals(tableName, DBConstants.META_TABLE_NAME)) {
        return DBTableDescriptor.META_TABLEDESC;
      }
      DBTableDescriptorFinder finder = new DBTableDescriptorFinder(tableName);
      MetaScanner.metaScan(conf, finder, tableName);
      DBTableDescriptor result = finder.getResult();
      if (result == null) {
        throw new TableNotFoundException(Bytes.toString(tableName));
      }
      return result;
    }

    public DBRegionLocation locateRegion(final byte [] tableName,
        final byte [] row)
    throws IOException{
      return locateRegion(tableName, row, true);
    }

    public DBRegionLocation relocateRegion(final byte [] tableName,
        final byte [] row)
    throws IOException{
      return locateRegion(tableName, row, false);
    }

    private DBRegionLocation locateRegion(final byte [] tableName,
      final byte [] row, boolean useCache)
    throws IOException{
      if (tableName == null || tableName.length == 0) {
        throw new IllegalArgumentException(
            "table name cannot be null or zero length");
      }

      if (Bytes.equals(tableName, DBConstants.ROOT_TABLE_NAME)) {
        synchronized (rootRegionLock) {
          // This block guards against two threads trying to find the root
          // region at the same time. One will go do the find while the
          // second waits. The second thread will not do find.

          if (!useCache || rootRegionLocation == null) {
            this.rootRegionLocation = locateRootRegion();
          }
          return this.rootRegionLocation;
        }
      } else if (Bytes.equals(tableName, DBConstants.META_TABLE_NAME)) {
        return locateRegionInMeta(DBConstants.ROOT_TABLE_NAME, tableName, row,
            useCache, metaRegionLock);
      } else {
        // Region not in the cache - have to go to the meta RS
        return locateRegionInMeta(DBConstants.META_TABLE_NAME, tableName, row,
            useCache, userRegionLock);
      }
    }

    /**
     * Search .META. for the DBRegionLocation info that contains the table and
     * row we're seeking. It will prefetch certain number of regions info and
     * save them to the global region cache.
     */
    private void prefetchRegionCache(final byte[] tableName,
        final byte[] row) {
      // Implement a new visitor for MetaScanner, and use it to walk through
      // the .META.
      MetaScannerVisitor visitor = new MetaScannerVisitor() {
        public boolean processRow(Result result) throws IOException {
          try {
            byte[] value = result.getValue(DBConstants.CATALOG_FAMILY,
                DBConstants.REGIONINFO_QUALIFIER);
            DBRegionInfo regionInfo = null;

            if (value != null) {
              // convert the row result into the DBRegionLocation we need!
              regionInfo = Writables.getDBRegionInfo(value);

              // possible we got a region of a different table...
              if (!Bytes.equals(regionInfo.getTableDesc().getName(),
                  tableName)) {
                return false; // stop scanning
              }
              if (regionInfo.isOffline()) {
                // don't cache offline regions
                return true;
              }
              value = result.getValue(DBConstants.CATALOG_FAMILY,
                  DBConstants.SERVER_QUALIFIER);
              if (value == null) {
                return true;  // don't cache it
              }
              final String serverAddress = Bytes.toString(value);

              // instantiate the location
              DBRegionLocation loc = new DBRegionLocation(regionInfo,
                new DBServerAddress(serverAddress));
              // cache this meta entry
              cacheLocation(tableName, loc);
            }
            return true;
          } catch (RuntimeException e) {
            throw new IOException(e);
          }
        }
      };
      try {
        // pre-fetch certain number of regions info at region cache.
        MetaScanner.metaScan(conf, visitor, tableName, row,
            this.prefetchRegionLimit);
      } catch (IOException e) {
        LOG.warn("Encounted problems when prefetch META table: ", e);
      }
    }

    /**
     * Search one of the meta tables (-ROOT- or .META.) for the DBRegionLocation
     * info that contains the table and row we're seeking.
     */
    private DBRegionLocation locateRegionInMeta(final byte [] parentTable,
      final byte [] tableName, final byte [] row, boolean useCache,
      Object regionLockObject)
    throws IOException {
      DBRegionLocation location;
      // If we are supposed to be using the cache, look in the cache to see if
      // we already have the region.
      if (useCache) {
        location = getCachedLocation(tableName, row);
        if (location != null) {
          return location;
        }
      }

      // build the key of the meta region we should be looking for.
      // the extra 9's on the end are necessary to allow "exact" matches
      // without knowing the precise region names.
      byte [] metaKey = DBRegionInfo.createRegionName(tableName, row,
        DBConstants.NINES, false);
      for (int tries = 0; true; tries++) {
        if (tries >= numRetries) {
          throw new NoServerForRegionException("Unable to find region for "
            + Bytes.toStringBinary(row) + " after " + numRetries + " tries.");
        }

        try {
          // locate the root or meta region
          DBRegionLocation metaLocation = locateRegion(parentTable, metaKey);
          DBRegionInterface server =
            getDBRegionConnection(metaLocation.getServerAddress());

          Result regionInfoRow = null;
          // This block guards against two threads trying to load the meta
          // region at the same time. The first will load the meta region and
          // the second will use the value that the first one found.
          synchronized (regionLockObject) {
            // If the parent table is META, we may want to pre-fetch some
            // region info into the global region cache for this table.
            if (Bytes.equals(parentTable, DBConstants.META_TABLE_NAME) &&
                (getRegionCachePrefetch(tableName)) )  {
              prefetchRegionCache(tableName, row);
            }

            // Check the cache again for a hit in case some other thread made the
            // same query while we were waiting on the lock. If not supposed to
            // be using the cache, delete any existing cached location so it won't
            // interfere.
            if (useCache) {
              location = getCachedLocation(tableName, row);
              if (location != null) {
                return location;
              }
            } else {
              deleteCachedLocation(tableName, row);
            }

          // Query the root or meta region for the location of the meta region
            regionInfoRow = server.getClosestRowBefore(
            metaLocation.getRegionInfo().getRegionName(), metaKey,
            DBConstants.CATALOG_FAMILY);
          }
          if (regionInfoRow == null) {
            throw new TableNotFoundException(Bytes.toString(tableName));
          }
          byte[] value = regionInfoRow.getValue(DBConstants.CATALOG_FAMILY,
              DBConstants.REGIONINFO_QUALIFIER);
          if (value == null || value.length == 0) {
            throw new IOException("DBRegionInfo was null or empty in " +
              Bytes.toString(parentTable) + ", row=" + regionInfoRow);
          }
          // convert the row result into the DBRegionLocation we need!
          DBRegionInfo regionInfo = (DBRegionInfo) Writables.getWritable(
              value, new DBRegionInfo());
          // possible we got a region of a different table...
          if (!Bytes.equals(regionInfo.getTableDesc().getName(), tableName)) {
            throw new TableNotFoundException(
              "Table '" + Bytes.toString(tableName) + "' was not found.");
          }
          if (regionInfo.isOffline()) {
            throw new RegionOfflineException("region offline: " +
              regionInfo.getRegionNameAsString());
          }

          value = regionInfoRow.getValue(DBConstants.CATALOG_FAMILY,
              DBConstants.SERVER_QUALIFIER);
          String serverAddress = "";
          if(value != null) {
            serverAddress = Bytes.toString(value);
          }
          if (serverAddress.equals("")) {
            throw new NoServerForRegionException("No server address listed " +
              "in " + Bytes.toString(parentTable) + " for region " +
              regionInfo.getRegionNameAsString());
          }

          // instantiate the location
          location = new DBRegionLocation(regionInfo,
            new DBServerAddress(serverAddress));
          cacheLocation(tableName, location);
          return location;
        } catch (TableNotFoundException e) {
          // if we got this error, probably means the table just plain doesn't
          // exist. rethrow the error immediately. this should always be coming
          // from the HTable constructor.
          throw e;
        } catch (IOException e) {
          if (e instanceof RemoteException) {
            e = RemoteExceptionHandler.decodeRemoteException(
                (RemoteException) e);
          }
          if (tries < numRetries - 1) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("locateRegionInMeta attempt " + tries + " of " +
                this.numRetries + " failed; retrying after sleep of " +
                getPauseTime(tries) + " because: " + e.getMessage());
            }
          } else {
            throw e;
          }
          // Only relocate the parent region if necessary
          if(!(e instanceof RegionOfflineException ||
              e instanceof NoServerForRegionException)) {
            relocateRegion(parentTable, metaKey);
          }
        }
        try{
          Thread.sleep(getPauseTime(tries));
        } catch (InterruptedException e){
          // continue
        }
      }
    }

    /**
     * Search the cache for a location that fits our table and row key.
     * Return null if no suitable region is located. TODO: synchronization note
     *
     * <p>TODO: This method during writing consumes 15% of CPU doing lookup
     * into the Soft Reference SortedMap.  Improve.
     *
     * @param tableName
     * @param row
     * @return Null or region location found in cache.
     */
    DBRegionLocation getCachedLocation(final byte [] tableName,
        final byte [] row) {
      SoftValueSortedMap<byte [], DBRegionLocation> tableLocations =
        getTableLocations(tableName);

      // start to examine the cache. we can only do cache actions
      // if there's something in the cache for this table.
      if (tableLocations.isEmpty()) {
        return null;
      }

      DBRegionLocation rl = tableLocations.get(row);
      if (rl != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Cache hit for row <" +
            Bytes.toStringBinary(row) +
            "> in tableName " + Bytes.toString(tableName) +
            ": location server " + rl.getServerAddress() +
            ", location region name " +
            rl.getRegionInfo().getRegionNameAsString());
        }
        return rl;
      }

      // Cut the cache so that we only get the part that could contain
      // regions that match our key
      SoftValueSortedMap<byte[], DBRegionLocation> matchingRegions =
        tableLocations.headMap(row);

      // if that portion of the map is empty, then we're done. otherwise,
      // we need to examine the cached location to verify that it is
      // a match by end key as well.
      if (!matchingRegions.isEmpty()) {
        DBRegionLocation possibleRegion =
          matchingRegions.get(matchingRegions.lastKey());

        // there is a possibility that the reference was garbage collected
        // in the instant since we checked isEmpty().
        if (possibleRegion != null) {
          byte[] endKey = possibleRegion.getRegionInfo().getEndKey();

          // make sure that the end key is greater than the row we're looking
          // for, otherwise the row actually belongs in the next region, not
          // this one. the exception case is when the endkey is
          // DBConstants.EMPTY_START_ROW, signifying that the region we're
          // checking is actually the last region in the table.
          if (Bytes.equals(endKey, DBConstants.EMPTY_END_ROW) ||
              KeyValue.getRowComparator(tableName).compareRows(endKey, 0, endKey.length,
                  row, 0, row.length) > 0) {
            return possibleRegion;
          }
        }
      }

      // Passed all the way through, so we got nothin - complete cache miss
      return null;
    }

    /**
     * Delete a cached location, if it satisfies the table name and row
     * requirements.
     */
    void deleteCachedLocation(final byte [] tableName,
                                      final byte [] row) {
      synchronized (this.cachedRegionLocations) {
        SoftValueSortedMap<byte [], DBRegionLocation> tableLocations =
            getTableLocations(tableName);

        // start to examine the cache. we can only do cache actions
        // if there's something in the cache for this table.
        if (!tableLocations.isEmpty()) {
          DBRegionLocation rl = getCachedLocation(tableName, row);
          if (rl != null) {
            tableLocations.remove(rl.getRegionInfo().getStartKey());
            if (LOG.isDebugEnabled()) {
              LOG.debug("Removed " +
                  rl.getRegionInfo().getRegionNameAsString() +
                  " for tableName=" + Bytes.toString(tableName) +
                  " from cache " + "because of " + Bytes.toStringBinary(row));
            }
          }
        }
      }
    }

    /**
     * @param tableName
     * @return Map of cached locations for passed <code>tableName</code>
     */
    private SoftValueSortedMap<byte [], DBRegionLocation> getTableLocations(
        final byte [] tableName) {
      // find the map of cached locations for this table
      Integer key = Bytes.mapKey(tableName);
      SoftValueSortedMap<byte [], DBRegionLocation> result;
      synchronized (this.cachedRegionLocations) {
        result = this.cachedRegionLocations.get(key);
        // if tableLocations for this table isn't built yet, make one
        if (result == null) {
          result = new SoftValueSortedMap<byte [], DBRegionLocation>(
              Bytes.BYTES_COMPARATOR);
          this.cachedRegionLocations.put(key, result);
        }
      }
      return result;
    }

    /**
     * Allows flushing the region cache.
     */
    public void clearRegionCache() {
     cachedRegionLocations.clear();
    }

    /**
     * Put a newly discovered DBRegionLocation into the cache.
     */
    private void cacheLocation(final byte [] tableName,
        final DBRegionLocation location) {
      byte [] startKey = location.getRegionInfo().getStartKey();
      SoftValueSortedMap<byte [], DBRegionLocation> tableLocations =
        getTableLocations(tableName);
      if (tableLocations.put(startKey, location) == null) {
        LOG.debug("Cached location for " +
            location.getRegionInfo().getRegionNameAsString() +
            " is " + location.getServerAddress());
      }
    }

    public DBRegionInterface getDBRegionConnection(
        DBServerAddress regionServer, boolean getMaster)
    throws IOException {
      if (getMaster) {
        getMaster();
      }
      DBRegionInterface server;
      synchronized (this.servers) {
        // See if we already have a connection
        server = this.servers.get(regionServer.toString());
        if (server == null) { // Get a connection
          try {
            server = (DBRegionInterface)DBRPC.waitForProxy(
                serverInterfaceClass, DBRPCProtocolVersion.versionID,
                regionServer.getInetSocketAddress(), this.conf,
                this.maxRPCAttempts, this.rpcTimeout);
          } catch (RemoteException e) {
            throw RemoteExceptionHandler.decodeRemoteException(e);
          }
          this.servers.put(regionServer.toString(), server);
        }
      }
      return server;
    }

    public DBRegionInterface getDBRegionConnection(
        DBServerAddress regionServer) throws IOException {
      return getDBRegionConnection(regionServer, false);
    }

    public synchronized PaxosWrapper getPaxosWrapper()
        throws IOException {
      return DBConnectionManager.getClientPaxosWatcher(conf)
          .getPaxosWrapper();
    }

    /**
     * Repeatedly try to find the root region in ZK
     * @return DBRegionLocation for root region if found
     * @throws NoServerForRegionException - if the root region can not be
     * located after retrying
     * @throws IOException
     */
    private DBRegionLocation locateRootRegion()
        throws IOException {
      // We lazily instantiate the Paxos object because we don't want to
      // make the constructor have to throw IOException or handle it itself.
      PaxosWrapper zk = getPaxosWrapper();

      DBServerAddress rootRegionAddress = null;
      for (int tries = 0; tries < numRetries; tries++) {
        int localTimeouts = 0;
        // ask the master which server has the root region
        while (rootRegionAddress == null && localTimeouts < numRetries) {
          // Don't read root region until we're out of safe mode so we know
          // that the meta regions have been assigned.
          rootRegionAddress = zk.readRootRegionLocation();
          if (rootRegionAddress == null) {
            try {
              if (LOG.isDebugEnabled()) {
                LOG.debug("Sleeping " + getPauseTime(tries) +
                  "ms, waiting for root region.");
              }
              Thread.sleep(getPauseTime(tries));
            } catch (InterruptedException iex) {
              // continue
            }
            localTimeouts++;
          }
        }

        if (rootRegionAddress == null) {
          throw new NoServerForRegionException(
              "Timed out trying to locate root region");
        }

        try {
          // Get a connection to the region server
          DBRegionInterface server = getDBRegionConnection(rootRegionAddress);
          // if this works, then we're good, and we have an acceptable address,
          // so we can stop doing retries and return the result.
          server.getRegionInfo(DBRegionInfo.ROOT_REGIONINFO.getRegionName());
          if (LOG.isDebugEnabled()) {
            LOG.debug("Found ROOT at " + rootRegionAddress);
          }
          break;
        } catch (Throwable t) {
          t = translateException(t);

          if (tries == numRetries - 1) {
            throw new NoServerForRegionException("Timed out trying to locate "+
                "root region because: " + t.getMessage());
          }

          // Sleep and retry finding root region.
          try {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Root region location changed. Sleeping.");
            }
            Thread.sleep(getPauseTime(tries));
            if (LOG.isDebugEnabled()) {
              LOG.debug("Wake. Retry finding root region.");
            }
          } catch (InterruptedException iex) {
            // continue
          }
        }

        rootRegionAddress = null;
      }

      // if the address is null by this point, then the retries have failed,
      // and we're sort of sunk
      if (rootRegionAddress == null) {
        throw new NoServerForRegionException(
          "unable to locate root region server");
      }

      // return the region location
      return new DBRegionLocation(
        DBRegionInfo.ROOT_REGIONINFO, rootRegionAddress);
    }

    public <T> T getRegionServerWithRetries(ServerCallable<T> callable)
    throws IOException, RuntimeException {
      List<Throwable> exceptions = new ArrayList<Throwable>();
      for(int tries = 0; tries < numRetries; tries++) {
        try {
          callable.instantiateServer(tries != 0);
          return callable.call();
        } catch (Throwable t) {
          t = translateException(t);
          exceptions.add(t);
          if (tries == numRetries - 1) {
            throw new RetriesExhaustedException(callable.getServerName(),
                callable.getRegionName(), callable.getRow(), tries, exceptions);
          }
        }
        try {
          Thread.sleep(getPauseTime(tries));
        } catch (InterruptedException e) {
          // continue
        }
      }
      return null;
    }

    public <T> T getRegionServerWithoutRetries(ServerCallable<T> callable)
        throws IOException, RuntimeException {
      try {
        callable.instantiateServer(false);
        return callable.call();
      } catch (Throwable t) {
        Throwable t2 = translateException(t);
        if (t2 instanceof IOException) {
          throw (IOException)t2;
        } else {
          throw new RuntimeException(t2);
        }
      }
    }

    private DBRegionLocation
      getRegionLocationForRowWithRetries(byte[] tableName, byte[] rowKey,
        boolean reload)
    throws IOException {
      boolean reloadFlag = reload;
      List<Throwable> exceptions = new ArrayList<Throwable>();
      DBRegionLocation location = null;
      int tries = 0;
      for (; tries < numRetries;) {
        try {
          location = getRegionLocation(tableName, rowKey, reloadFlag);
        } catch (Throwable t) {
          exceptions.add(t);
        }
        if (location != null) {
          break;
        }
        reloadFlag = true;
        tries++;
        try {
          Thread.sleep(getPauseTime(tries));
        } catch (InterruptedException e) {
          // continue
        }
      }
      if (location == null) {
        throw new RetriesExhaustedException(" -- nothing found, no 'location' returned," +
          " tableName=" + Bytes.toString(tableName) +
          ", reload=" + reload + " --",
          DBConstants.EMPTY_BYTE_ARRAY, rowKey, tries, exceptions);
      }
      return location;
    }

    /**
     * Helper class for batch updates.
     * Holds code shared doing batch puts and batch deletes.
     */
    private abstract class Batch {
      final DBConnection c;

      private Batch(final DBConnection c) {
        this.c = c;
      }

      /**
       * This is the method subclasses must implement.
       * @param currentList current list of rows
       * @param tableName table we are processing
       * @param row row
       * @return Count of items processed or -1 if all.
       * @throws IOException if a remote or network exception occurs
       * @throws RuntimeException other undefined exception
       */
      abstract int doCall(final List<? extends Row> currentList,
        final byte [] row, final byte [] tableName)
      throws IOException, RuntimeException;

      /**
       * Process the passed <code>list</code>.
       * @param list list of rows to process
       * @param tableName table we are processing
       * @return Count of how many added or -1 if all added.
       * @throws IOException if a remote or network exception occurs
       */
      int process(final List<? extends Row> list, final byte[] tableName)
      throws IOException {
        byte [] region = getRegionName(tableName, list.get(0).getRow(), false);
        byte [] currentRegion = region;
        boolean isLastRow;
        boolean retryOnlyOne = false;
        List<Row> currentList = new ArrayList<Row>();
        int i, tries;
        for (i = 0, tries = 0; i < list.size() && tries < numRetries; i++) {
          Row row = list.get(i);
          currentList.add(row);
          // If the next record goes to a new region, then we are to clear
          // currentList now during this cycle.
          isLastRow = (i + 1) == list.size();
          if (!isLastRow) {
            region = getRegionName(tableName, list.get(i + 1).getRow(), false);
          }
          if (!Bytes.equals(currentRegion, region) || isLastRow || retryOnlyOne) {
            int index = doCall(currentList, row.getRow(), tableName);
            // index is == -1 if all processed successfully, else its index
            // of last record successfully processed.
            if (index != -1) {
              if (tries == numRetries - 1) {
                throw new RetriesExhaustedException("Some server, retryOnlyOne=" +
                  retryOnlyOne + ", index=" + index + ", islastrow=" + isLastRow +
                  ", tries=" + tries + ", numtries=" + numRetries + ", i=" + i +
                  ", listsize=" + list.size() + ", region=" +
                  Bytes.toStringBinary(region), currentRegion, row.getRow(),
                  tries, new ArrayList<Throwable>());
              }
              tries = doBatchPause(currentRegion, tries);
              i = i - currentList.size() + index;
              retryOnlyOne = true;
              // Reload location.
              region = getRegionName(tableName, list.get(i + 1).getRow(), true);
            } else {
              // Reset these flags/counters on successful batch Put
              retryOnlyOne = false;
              tries = 0;
            }
            currentRegion = region;
            currentList.clear();
          }
        }
        return i;
      }

      /**
       * @param t
       * @param r
       * @param re
       * @return Region name that holds passed row <code>r</code>
       * @throws IOException
       */
      private byte [] getRegionName(final byte [] t, final byte [] r,
        final boolean re)
      throws IOException {
        DBRegionLocation location = getRegionLocationForRowWithRetries(t, r, re);
        return location.getRegionInfo().getRegionName();
      }

      /**
       * Do pause processing before retrying...
       * @param currentRegion
       * @param tries
       * @return New value for tries.
       */
      private int doBatchPause(final byte [] currentRegion, final int tries) {
        int localTries = tries;
        long sleepTime = getPauseTime(tries);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Reloading region " + Bytes.toStringBinary(currentRegion) +
            " location because regionserver didn't accept updates; tries=" +
            tries + " of max=" + numRetries + ", waiting=" + sleepTime + "ms");
        }
        try {
          Thread.sleep(sleepTime);
          localTries++;
        } catch (InterruptedException e) {
          // continue
        }
        return localTries;
      }
    }

    public int processBatchOfRows(final ArrayList<Put> list,
      final byte[] tableName)
    throws IOException {
      if (list.isEmpty()) return 0;
      if (list.size() > 1) Collections.sort(list);
      Batch b = new Batch(this) {
        @SuppressWarnings("unchecked")
        @Override
        int doCall(final List<? extends Row> currentList, final byte [] row,
          final byte [] tableName)
        throws IOException, RuntimeException {
          final List<Put> puts = (List<Put>)currentList;
          return getRegionServerWithRetries(new ServerCallable<Integer>(this.c,
              tableName, row) {
            public Integer call() throws IOException {
              return server.put(location.getRegionInfo().getRegionName(), puts);
            }
          });
        }
      };
      return b.process(list, tableName);
    }

    public int processBatchOfDeletes(final List<Delete> list,
      final byte[] tableName)
    throws IOException {
      if (list.isEmpty()) return 0;
      if (list.size() > 1) Collections.sort(list);
      Batch b = new Batch(this) {
        @SuppressWarnings("unchecked")
        @Override
        int doCall(final List<? extends Row> currentList, final byte [] row,
          final byte [] tableName)
        throws IOException, RuntimeException {
          final List<Delete> deletes = (List<Delete>)currentList;
          return getRegionServerWithRetries(new ServerCallable<Integer>(this.c,
                tableName, row) {
              public Integer call() throws IOException {
                return server.delete(location.getRegionInfo().getRegionName(),
                  deletes);
              }
            });
          }
        };
        return b.process(list, tableName);
      }

    void close(boolean stopProxy) {
      if (master != null) {
        if (stopProxy) {
          DBRPC.stopProxy(master);
        }
        master = null;
        masterChecked = false;
      }
      if (stopProxy) {
        for (DBRegionInterface i: servers.values()) {
          DBRPC.stopProxy(i);
        }
      }
    }

    /**
     * Process a batch of Puts on the given executor service.
     *
     * @param list the puts to make - successful puts will be removed.
     * @param pool thread pool to execute requests on
     *
     * In the case of an exception, we take different actions depending on the
     * situation:
     *  - If the exception is a DoNotRetryException, we rethrow it and leave the
     *    'list' parameter in an indeterminate state.
     *  - If the 'list' parameter is a singleton, we directly throw the specific
     *    exception for that put.
     *  - Otherwise, we throw a generic exception indicating that an error occurred.
     *    The 'list' parameter is mutated to contain those puts that did not succeed.
     */
    public void processBatchOfPuts(List<Put> list,
                                   final byte[] tableName, ExecutorService pool) throws IOException {
      boolean singletonList = list.size() == 1;
      Throwable singleRowCause = null;
      for ( int tries = 0 ; tries < numRetries && !list.isEmpty(); ++tries) {
        Collections.sort(list);
        Map<DBServerAddress, MultiPut> regionPuts =
            new HashMap<DBServerAddress, MultiPut>();
        // step 1:
        //  break up into regionserver-sized chunks and build the data structs
        for ( Put put : list ) {
          byte [] row = put.getRow();

          DBRegionLocation loc = locateRegion(tableName, row, true);
          DBServerAddress address = loc.getServerAddress();
          byte [] regionName = loc.getRegionInfo().getRegionName();

          MultiPut mput = regionPuts.get(address);
          if (mput == null) {
            mput = new MultiPut(address);
            regionPuts.put(address, mput);
          }
          mput.add(regionName, put);
        }

        // step 2:
        //  make the requests
        // Discard the map, just use a list now, makes error recovery easier.
        List<MultiPut> multiPuts = new ArrayList<MultiPut>(regionPuts.values());

        List<Future<MultiPutResponse>> futures =
            new ArrayList<Future<MultiPutResponse>>(regionPuts.size());
        for ( MultiPut put : multiPuts ) {
          futures.add(pool.submit(createPutCallable(put.address,
              put,
              tableName)));
        }
        // RUN!
        List<Put> failed = new ArrayList<Put>();

        // step 3:
        //  collect the failures and tries from step 1.
        for (int i = 0; i < futures.size(); i++ ) {
          Future<MultiPutResponse> future = futures.get(i);
          MultiPut request = multiPuts.get(i);
          try {
            MultiPutResponse resp = future.get();

            // For each region
            for (Map.Entry<byte[], List<Put>> e : request.puts.entrySet()) {
              Integer result = resp.getAnswer(e.getKey());
              if (result == null) {
                // failed
                LOG.debug("Failed all for region: " +
                    Bytes.toStringBinary(e.getKey()) + ", removing from cache");
                failed.addAll(e.getValue());
              } else if (result >= 0) {
                // some failures
                List<Put> lst = e.getValue();
                failed.addAll(lst.subList(result, lst.size()));
                LOG.debug("Failed past " + result + " for region: " +
                    Bytes.toStringBinary(e.getKey()) + ", removing from cache");
              }
            }
          } catch (InterruptedException e) {
            // go into the failed list.
            LOG.debug("Failed all from " + request.address, e);
            failed.addAll(request.allPuts());
          } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            // Don't print stack trace if NSRE; NSRE is 'normal' operation.
            if (cause instanceof NotServingRegionException) {
              String msg = cause.getMessage();
              if (msg != null && msg.length() > 0) {
                // msg is the exception as a String... we just want first line.
                msg = msg.split("[\\n\\r]+\\s*at")[0];
              }
              LOG.debug("Failed execution of all on " + request.address +
                " because: " + msg);
            } else {
              // all go into the failed list.
              LOG.debug("Failed execution of all on " + request.address,
                e.getCause());
            }
            failed.addAll(request.allPuts());

            // Just give up, leaving the batch put list in an untouched/semi-committed state
            if (e.getCause() instanceof DoNotRetryIOException) {
              throw (DoNotRetryIOException) e.getCause();
            }

            if (singletonList) {
              // be richer for reporting in a 1 row case.
              singleRowCause = e.getCause();
            }
          }
        }
        list.clear();
        if (!failed.isEmpty()) {
          for (Put failedPut: failed) {
            deleteCachedLocation(tableName, failedPut.getRow());
          }

          list.addAll(failed);

          long sleepTime = getPauseTime(tries);
          LOG.debug("processBatchOfPuts had some failures, sleeping for " + sleepTime +
              " ms!");
          try {
            Thread.sleep(sleepTime);
          } catch (InterruptedException ignored) {
          }
        }
      }
      if (!list.isEmpty()) {
        if (singletonList && singleRowCause != null) {
          throw new IOException(singleRowCause);
        }

        // ran out of retries and didnt succeed everything!
        throw new RetriesExhaustedException("Still had " + list.size() + " puts left after retrying " +
            numRetries + " times.");
      }
    }


    private Callable<MultiPutResponse> createPutCallable(
        final DBServerAddress address, final MultiPut puts,
        final byte [] tableName) {
      final DBConnection connection = this;
      return new Callable<MultiPutResponse>() {
        public MultiPutResponse call() throws IOException {
          return getRegionServerWithoutRetries(
              new ServerCallable<MultiPutResponse>(connection, tableName, null) {
                public MultiPutResponse call() throws IOException {
                  MultiPutResponse resp = server.multiPut(puts);
                  resp.request = puts;
                  return resp;
                }
                @Override
                public void instantiateServer(boolean reload) throws IOException {
                  server = connection.getDBRegionConnection(address);
                }
              }
          );
        }
      };
    }

    private Throwable translateException(Throwable t) throws IOException {
      if (t instanceof UndeclaredThrowableException) {
        t = t.getCause();
      }
      if (t instanceof RemoteException) {
        t = RemoteExceptionHandler.decodeRemoteException((RemoteException)t);
      }
      if (t instanceof DoNotRetryIOException) {
        throw (DoNotRetryIOException)t;
      }
      return t;
    }

    /**
     * Return the number of cached region for a table. It will only be called
     * from a unit test.
     */
    int getNumberOfCachedRegionLocations(final byte[] tableName) {
      Integer key = Bytes.mapKey(tableName);
      synchronized (this.cachedRegionLocations) {
        SoftValueSortedMap<byte[], DBRegionLocation> tableLocs =
          this.cachedRegionLocations.get(key);

        if (tableLocs == null) {
          return 0;
        }
        return tableLocs.values().size();
      }
    }

    /**
     * Check the region cache to see whether a region is cached yet or not.
     * Called by unit tests.
     * @param tableName tableName
     * @param row row
     * @return Region cached or not.
     */
    boolean isRegionCached(final byte[] tableName, final byte[] row) {
      DBRegionLocation location = getCachedLocation(tableName, row);
      return location != null;
    }

    public void setRegionCachePrefetch(final byte[] tableName,
        final boolean enable) {
      if (!enable) {
        regionCachePrefetchDisabledTables.add(Bytes.mapKey(tableName));
      }
      else {
        regionCachePrefetchDisabledTables.remove(Bytes.mapKey(tableName));
      }
    }

    public boolean getRegionCachePrefetch(final byte[] tableName) {
      return !regionCachePrefetchDisabledTables.contains(Bytes.mapKey(tableName));
    }

    public void prewarmRegionCache(final byte[] tableName,
        final Map<DBRegionInfo, DBServerAddress> regions) {
      for (Map.Entry<DBRegionInfo, DBServerAddress> e : regions.entrySet()) {
        cacheLocation(tableName,
            new DBRegionLocation(e.getKey(), e.getValue()));
      }
    }
  }
}

