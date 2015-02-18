package org.javenstudio.raptor.bigdb.client;

import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.bigdb.DoNotRetryIOException;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.DBRegionLocation;
import org.javenstudio.raptor.bigdb.DBServerAddress;
import org.javenstudio.raptor.bigdb.DBTableDescriptor;
import org.javenstudio.raptor.bigdb.KeyValue;
import org.javenstudio.raptor.bigdb.NotServingRegionException;
import org.javenstudio.raptor.bigdb.UnknownScannerException;
import org.javenstudio.raptor.bigdb.client.MetaScanner.MetaScannerVisitor;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.bigdb.util.Pair;
import org.javenstudio.raptor.bigdb.util.Writables;

/**
 * Used to communicate with a single HBase table.
 * 
 * This class is not thread safe for writes.
 * Gets, puts, and deletes take out a row lock for the duration
 * of their operation.  Scans (currently) do not respect
 * row locking.
 * 
 * See {@link HBaseAdmin} to create, drop, list, enable and disable tables.
 */
public class DBTable implements DBTableInterface {
  private final DBConnection connection;
  private final byte [] tableName;
  protected final int scannerTimeout;
  private volatile Configuration configuration;
  private final ArrayList<Put> writeBuffer = new ArrayList<Put>();
  private long writeBufferSize;
  private boolean autoFlush;
  private long currentWriteBufferSize;
  protected int scannerCaching;
  private int maxKeyValueSize;

  private long maxScannerResultSize;

  /**
   * Creates an object to access a HBase table.
   *
   * @param tableName Name of the table.
   * @throws IOException if a remote or network exception occurs
   */
  public DBTable(final String tableName)
      throws IOException {
    this(ConfigurationFactory.get(), Bytes.toBytes(tableName));
  }

  /**
   * Creates an object to access a HBase table.
   *
   * @param tableName Name of the table.
   * @throws IOException if a remote or network exception occurs
   */
  public DBTable(final byte [] tableName)
      throws IOException {
    this(ConfigurationFactory.get(), tableName);
  }

  /**
   * Creates an object to access a HBase table.
   *
   * @param conf Configuration object to use.
   * @param tableName Name of the table.
   * @throws IOException if a remote or network exception occurs
   */
  public DBTable(Configuration conf, final String tableName)
      throws IOException {
    this(conf, Bytes.toBytes(tableName));
  }

  /**
   * Creates an object to access a HBase table.
   *
   * @param conf Configuration object to use.
   * @param tableName Name of the table.
   * @throws IOException if a remote or network exception occurs
   */
  public DBTable(Configuration conf, final byte [] tableName)
      throws IOException {
    this.tableName = tableName;
    if (conf == null) {
      this.scannerTimeout = 0;
      this.connection = null;
      return;
    }
    this.connection = DBConnectionManager.getConnection(conf);
    this.scannerTimeout =
      (int) conf.getLong(DBConstants.BIGDB_REGIONSERVER_LEASE_PERIOD_KEY, 
    		  DBConstants.DEFAULT_BIGDB_REGIONSERVER_LEASE_PERIOD);
    this.configuration = conf;
    this.connection.locateRegion(tableName, DBConstants.EMPTY_START_ROW);
    this.writeBufferSize = conf.getLong("bigdb.client.write.buffer", 2097152);
    this.autoFlush = true;
    this.currentWriteBufferSize = 0;
    this.scannerCaching = conf.getInt("bigdb.client.scanner.caching", 1);

    this.maxScannerResultSize = conf.getLong(
      DBConstants.BIGDB_CLIENT_SCANNER_MAX_RESULT_SIZE_KEY,
      DBConstants.DEFAULT_BIGDB_CLIENT_SCANNER_MAX_RESULT_SIZE);
    this.maxKeyValueSize = conf.getInt("bigdb.client.keyvalue.maxsize", -1);

    int nrHRS = getCurrentNrHRS();
    if (nrHRS == 0) {
      // No servers running -- set default of 10 threads.
      nrHRS = 10;
    }
    int nrThreads = conf.getInt("bigdb.htable.threads.max", nrHRS);

    // Unfortunately Executors.newCachedThreadPool does not allow us to
    // set the maximum size of the pool, so we have to do it ourselves.
    this.pool = new ThreadPoolExecutor(0, nrThreads,
        60, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(),
        new DaemonThreadFactory());
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * TODO Might want to change this to public, would be nice if the number
   * of threads would automatically change when servers were added and removed
   * @return the number of region servers that are currently running
   * @throws IOException if a remote or network exception occurs
   */
  int getCurrentNrHRS() throws IOException {
    return DBConnectionManager
      .getClientPaxosWatcher(this.configuration)
      .getPaxosWrapper()
      .getRSDirectoryCount();
  }

  // For multiput
  private ExecutorService pool;

  /**
   * Tells whether or not a table is enabled or not.
   * @param tableName Name of table to check.
   * @return {@code true} if table is online.
   * @throws IOException if a remote or network exception occurs
   */
  public static boolean isTableEnabled(String tableName) throws IOException {
    return isTableEnabled(Bytes.toBytes(tableName));
  }

  /**
   * Tells whether or not a table is enabled or not.
   * @param tableName Name of table to check.
   * @return {@code true} if table is online.
   * @throws IOException if a remote or network exception occurs
   */
  public static boolean isTableEnabled(byte[] tableName) throws IOException {
    return isTableEnabled(ConfigurationFactory.get(), tableName);
  }

  /**
   * Tells whether or not a table is enabled or not.
   * @param conf The Configuration object to use.
   * @param tableName Name of table to check.
   * @return {@code true} if table is online.
   * @throws IOException if a remote or network exception occurs
   */
  public static boolean isTableEnabled(Configuration conf, String tableName)
      throws IOException {
    return isTableEnabled(conf, Bytes.toBytes(tableName));
  }

  /**
   * Tells whether or not a table is enabled or not.
   * @param conf The Configuration object to use.
   * @param tableName Name of table to check.
   * @return {@code true} if table is online.
   * @throws IOException if a remote or network exception occurs
   */
  public static boolean isTableEnabled(Configuration conf, byte[] tableName)
      throws IOException {
    return DBConnectionManager.getConnection(conf).isTableEnabled(tableName);
  }

  /**
   * Find region location hosting passed row using cached info
   * @param row Row to find.
   * @return The location of the given row.
   * @throws IOException if a remote or network exception occurs
   */
  public DBRegionLocation getRegionLocation(final String row)
      throws IOException {
    return connection.getRegionLocation(tableName, Bytes.toBytes(row), false);
  }

  /**
   * Finds the region on which the given row is being served.
   * @param row Row to find.
   * @return Location of the row.
   * @throws IOException if a remote or network exception occurs
   */
  public DBRegionLocation getRegionLocation(final byte [] row)
      throws IOException {
    return connection.getRegionLocation(tableName, row, false);
  }

  public byte [] getTableName() {
    return this.tableName;
  }

  /**
   * <em>INTERNAL</em> Used by unit tests and tools to do low-level
   * manipulations.
   * @return An DBConnection instance.
   */
  // TODO(tsuna): Remove this.  Unit tests shouldn't require public helpers.
  public DBConnection getConnection() {
    return this.connection;
  }

  /**
   * Gets the number of rows that a scanner will fetch at once.
   * <p>
   * The default value comes from {@code bigdb.client.scanner.caching}.
   */
  public int getScannerCaching() {
    return scannerCaching;
  }

  /**
   * Sets the number of rows that a scanner will fetch at once.
   * <p>
   * This will override the value specified by
   * {@code bigdb.client.scanner.caching}.
   * Increasing this value will reduce the amount of work needed each time
   * {@code next()} is called on a scanner, at the expense of memory use
   * (since more rows will need to be maintained in memory by the scanners).
   * @param scannerCaching the number of rows a scanner will fetch at once.
   */
  public void setScannerCaching(int scannerCaching) {
    this.scannerCaching = scannerCaching;
  }

  public DBTableDescriptor getTableDescriptor() throws IOException {
    return new UnmodifyableDBTableDescriptor(
      this.connection.getDBTableDescriptor(this.tableName));
  }

  /**
   * Gets the starting row key for every region in the currently open table.
   * <p>
   * This is mainly useful for the MapReduce integration.
   * @return Array of region starting row keys
   * @throws IOException if a remote or network exception occurs
   */
  public byte [][] getStartKeys() throws IOException {
    return getStartEndKeys().getFirst();
  }

  /**
   * Gets the ending row key for every region in the currently open table.
   * <p>
   * This is mainly useful for the MapReduce integration.
   * @return Array of region ending row keys
   * @throws IOException if a remote or network exception occurs
   */
  public byte[][] getEndKeys() throws IOException {
    return getStartEndKeys().getSecond();
  }

  /**
   * Gets the starting and ending row keys for every region in the currently
   * open table.
   * <p>
   * This is mainly useful for the MapReduce integration.
   * @return Pair of arrays of region starting and ending row keys
   * @throws IOException if a remote or network exception occurs
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Pair<byte[][],byte[][]> getStartEndKeys() throws IOException {
    final List<byte[]> startKeyList = new ArrayList<byte[]>();
    final List<byte[]> endKeyList = new ArrayList<byte[]>();
    MetaScannerVisitor visitor = new MetaScannerVisitor() {
      public boolean processRow(Result rowResult) throws IOException {
        DBRegionInfo info = Writables.getDBRegionInfo(
            rowResult.getValue(DBConstants.CATALOG_FAMILY,
                DBConstants.REGIONINFO_QUALIFIER));
        if (Bytes.equals(info.getTableDesc().getName(), getTableName())) {
          if (!(info.isOffline() || info.isSplit())) {
            startKeyList.add(info.getStartKey());
            endKeyList.add(info.getEndKey());
          }
        }
        return true;
      }
    };
    MetaScanner.metaScan(configuration, visitor, this.tableName);
    return new Pair(startKeyList.toArray(new byte[startKeyList.size()][]),
                endKeyList.toArray(new byte[endKeyList.size()][]));
  }

  /**
   * Gets all the regions and their address for this table.
   * <p>
   * This is mainly useful for the MapReduce integration.
   * @return A map of DBRegionInfo with it's server address
   * @throws IOException if a remote or network exception occurs
   */
  public Map<DBRegionInfo, DBServerAddress> getRegionsInfo() throws IOException {
    final Map<DBRegionInfo, DBServerAddress> regionMap =
      new TreeMap<DBRegionInfo, DBServerAddress>();

    MetaScannerVisitor visitor = new MetaScannerVisitor() {
      public boolean processRow(Result rowResult) throws IOException {
        DBRegionInfo info = Writables.getDBRegionInfo(
            rowResult.getValue(DBConstants.CATALOG_FAMILY,
                DBConstants.REGIONINFO_QUALIFIER));

        if (!(Bytes.equals(info.getTableDesc().getName(), getTableName()))) {
          return false;
        }

        DBServerAddress server = new DBServerAddress();
        byte [] value = rowResult.getValue(DBConstants.CATALOG_FAMILY,
            DBConstants.SERVER_QUALIFIER);
        if (value != null && value.length > 0) {
          String address = Bytes.toString(value);
          server = new DBServerAddress(address);
        }

        if (!(info.isOffline() || info.isSplit())) {
          regionMap.put(new UnmodifyableDBRegionInfo(info), server);
        }
        return true;
      }

    };
    MetaScanner.metaScan(configuration, visitor, tableName);
    return regionMap;
  }

  /**
   * Save the passed region information and the table's regions
   * cache.
   * <p>
   * This is mainly useful for the MapReduce integration. You can call
   * {@link #deserializeRegionInfo deserializeRegionInfo}
   * to deserialize regions information from a
   * {@link DataInput}, then call this method to load them to cache.
   *
   * <pre>
   * {@code
   * DBTable t1 = new DBTable("foo");
   * FileInputStream fis = new FileInputStream("regions.dat");
   * DataInputStream dis = new DataInputStream(fis);
   *
   * Map<DBRegionInfo, DBServerAddress> hm = t1.deserializeRegionInfo(dis);
   * t1.prewarmRegionCache(hm);
   * }
   * </pre>
   * @param regionMap This piece of regions information will be loaded
   * to region cache.
   */
  public void prewarmRegionCache(Map<DBRegionInfo, DBServerAddress> regionMap) {
    this.connection.prewarmRegionCache(this.getTableName(), regionMap);
  }

  /**
   * Serialize the regions information of this table and output
   * to <code>out</code>.
   * <p>
   * This is mainly useful for the MapReduce integration. A client could
   * perform a large scan for all the regions for the table, serialize the
   * region info to a file. MR job can ship a copy of the meta for the table in
   * the DistributedCache.
   * <pre>
   * {@code
   * FileOutputStream fos = new FileOutputStream("regions.dat");
   * DataOutputStream dos = new DataOutputStream(fos);
   * table.serializeRegionInfo(dos);
   * dos.flush();
   * dos.close();
   * }
   * </pre>
   * @param out {@link DataOutput} to serialize this object into.
   * @throws IOException if a remote or network exception occurs
   */
  public void serializeRegionInfo(DataOutput out) throws IOException {
    Map<DBRegionInfo, DBServerAddress> allRegions = this.getRegionsInfo();
    // first, write number of regions
    out.writeInt(allRegions.size());
    for (Map.Entry<DBRegionInfo, DBServerAddress> es : allRegions.entrySet()) {
      es.getKey().write(out);
      es.getValue().write(out);
    }
  }

  /**
   * Read from <code>in</code> and deserialize the regions information.
   *
   * <p>It behaves similarly as {@link #getRegionsInfo getRegionsInfo}, except
   * that it loads the region map from a {@link DataInput} object.
   *
   * <p>It is supposed to be followed immediately by  {@link
   * #prewarmRegionCache prewarmRegionCache}.
   *
   * <p>
   * Please refer to {@link #prewarmRegionCache prewarmRegionCache} for usage.
   *
   * @param in {@link DataInput} object.
   * @return A map of DBRegionInfo with its server address.
   * @throws IOException if an I/O exception occurs.
   */
  public Map<DBRegionInfo, DBServerAddress> deserializeRegionInfo(DataInput in)
  throws IOException {
    final Map<DBRegionInfo, DBServerAddress> allRegions =
      new TreeMap<DBRegionInfo, DBServerAddress>();

    // the first integer is expected to be the size of records
    int regionsCount = in.readInt();
    for (int i = 0; i < regionsCount; ++i) {
      DBRegionInfo hri = new DBRegionInfo();
      hri.readFields(in);
      DBServerAddress hsa = new DBServerAddress();
      hsa.readFields(in);
      allRegions.put(hri, hsa);
    }
    return allRegions;
  }

   public Result getRowOrBefore(final byte[] row, final byte[] family)
   throws IOException {
     return connection.getRegionServerWithRetries(
         new ServerCallable<Result>(connection, tableName, row) {
       public Result call() throws IOException {
         return server.getClosestRowBefore(location.getRegionInfo().getRegionName(),
           row, family);
       }
     });
   }

  public ResultScanner getScanner(final Scan scan) throws IOException {
    ClientScanner s = new ClientScanner(scan);
    s.initialize();
    return s;
  }

  public ResultScanner getScanner(byte [] family) throws IOException {
    Scan scan = new Scan();
    scan.addFamily(family);
    return getScanner(scan);
  }

  public ResultScanner getScanner(byte [] family, byte [] qualifier)
      throws IOException {
    Scan scan = new Scan();
    scan.addColumn(family, qualifier);
    return getScanner(scan);
  }

  public Result get(final Get get) throws IOException {
    return connection.getRegionServerWithRetries(
        new ServerCallable<Result>(connection, tableName, get.getRow()) {
          public Result call() throws IOException {
            return server.get(location.getRegionInfo().getRegionName(), get);
          }
        }
    );
  }

  public void delete(final Delete delete)
      throws IOException {
    connection.getRegionServerWithRetries(
        new ServerCallable<Boolean>(connection, tableName, delete.getRow()) {
          public Boolean call() throws IOException {
            server.delete(location.getRegionInfo().getRegionName(), delete);
            return null; // FindBugs NP_BOOLEAN_RETURN_NULL
          }
        }
    );
  }

  public void delete(final List<Delete> deletes)
      throws IOException {
    int last = 0;
    try {
      last = connection.processBatchOfDeletes(deletes, this.tableName);
    } finally {
      deletes.subList(0, last).clear();
    }
  }

  public void put(final Put put) throws IOException {
    doPut(Arrays.asList(put));
  }

  public void put(final List<Put> puts) throws IOException {
    doPut(puts);
  }

  private void doPut(final List<Put> puts) throws IOException {
    for (Put put : puts) {
      validatePut(put);
      writeBuffer.add(put);
      currentWriteBufferSize += put.heapSize();
    }
    if (autoFlush || currentWriteBufferSize > writeBufferSize) {
      flushCommits();
    }
  }

  public long incrementColumnValue(final byte [] row, final byte [] family,
      final byte [] qualifier, final long amount)
      throws IOException {
    return incrementColumnValue(row, family, qualifier, amount, true);
  }

  public long incrementColumnValue(final byte [] row, final byte [] family,
      final byte [] qualifier, final long amount, final boolean writeToWAL)
      throws IOException {
    NullPointerException npe = null;
    if (row == null) {
      npe = new NullPointerException("row is null");
    } else if (family == null) {
      npe = new NullPointerException("column is null");
    }
    if (npe != null) {
      throw new IOException(
          "Invalid arguments to incrementColumnValue", npe);
    }
    return connection.getRegionServerWithRetries(
        new ServerCallable<Long>(connection, tableName, row) {
          public Long call() throws IOException {
            return server.incrementColumnValue(
                location.getRegionInfo().getRegionName(), row, family,
                qualifier, amount, writeToWAL);
          }
        }
    );
  }

  /**
   * Atomically checks if a row/family/qualifier value match the expectedValue.
   * If it does, it adds the put.  If value == null, checks for non-existence
   * of the value.
   *
   * @param row to check
   * @param family column family
   * @param qualifier column qualifier
   * @param value the expected value
   * @param put put to execute if value matches.
   * @throws IOException
   * @return true if the new put was execute, false otherwise
   */
  public boolean checkAndPut(final byte [] row,
      final byte [] family, final byte [] qualifier, final byte [] value,
      final Put put) throws IOException {
    return connection.getRegionServerWithRetries(
        new ServerCallable<Boolean>(connection, tableName, row) {
          public Boolean call() throws IOException {
            return server.checkAndPut(location.getRegionInfo().getRegionName(),
                row, family, qualifier, value, put) ? Boolean.TRUE : Boolean.FALSE;
          }
        }
    );
  }

  /**
   * Atomically checks if a row/family/qualifier value match the expectedValue.
   * If it does, it adds the delete.  If value == null, checks for non-existence
   * of the value.
   *
   * @param row to check
   * @param family column family
   * @param qualifier column qualifier
   * @param value the expected value
   * @param delete delete to execute if value matches.
   * @throws IOException
   * @return true if the new delete was executed, false otherwise
   */
  public boolean checkAndDelete(final byte [] row,
      final byte [] family, final byte [] qualifier, final byte [] value,
      final Delete delete) throws IOException {
    return connection.getRegionServerWithRetries(
        new ServerCallable<Boolean>(connection, tableName, row) {
          public Boolean call() throws IOException {
            return server.checkAndDelete(
                location.getRegionInfo().getRegionName(),
                row, family, qualifier, value, delete) 
            ? Boolean.TRUE : Boolean.FALSE;
          }
        }
    );
  }

  /**
   * Test for the existence of columns in the table, as specified in the Get.<p>
   *
   * This will return true if the Get matches one or more keys, false if not.<p>
   *
   * This is a server-side call so it prevents any data from being transfered
   * to the client.
   * @param get param to check for
   * @return true if the specified Get matches one or more keys, false if not
   * @throws IOException
   */
  public boolean exists(final Get get) throws IOException {
    return connection.getRegionServerWithRetries(
        new ServerCallable<Boolean>(connection, tableName, get.getRow()) {
          public Boolean call() throws IOException {
            return server.
                exists(location.getRegionInfo().getRegionName(), get);
          }
        }
    );
  }

  public void flushCommits() throws IOException {
    try {
      connection.processBatchOfPuts(writeBuffer,
          tableName, pool);
    } finally {
      // the write buffer was adjusted by processBatchOfPuts
      currentWriteBufferSize = 0;
      for (Put aPut : writeBuffer) {
        currentWriteBufferSize += aPut.heapSize();
      }
    }
  }

  public void close() throws IOException{
    flushCommits();
  }

  // validate for well-formedness
  private void validatePut(final Put put) throws IllegalArgumentException{
    if (put.isEmpty()) {
      throw new IllegalArgumentException("No columns to insert");
    }
    if (maxKeyValueSize > 0) {
      for (List<KeyValue> list : put.getFamilyMap().values()) {
        for (KeyValue kv : list) {
          if (kv.getLength() > maxKeyValueSize) {
            throw new IllegalArgumentException("KeyValue size too large");
          }
        }
      }
    }
  }

  public RowLock lockRow(final byte [] row)
      throws IOException {
    return connection.getRegionServerWithRetries(
      new ServerCallable<RowLock>(connection, tableName, row) {
        public RowLock call() throws IOException {
          long lockId =
              server.lockRow(location.getRegionInfo().getRegionName(), row);
          return new RowLock(row,lockId);
        }
      }
    );
  }

  public void unlockRow(final RowLock rl)
      throws IOException {
    connection.getRegionServerWithRetries(
      new ServerCallable<Boolean>(connection, tableName, rl.getRow()) {
        public Boolean call() throws IOException {
          server.unlockRow(location.getRegionInfo().getRegionName(),
              rl.getLockId());
          return null; // FindBugs NP_BOOLEAN_RETURN_NULL
        }
      }
    );
  }

  public boolean isAutoFlush() {
    return autoFlush;
  }

  /**
   * Turns 'auto-flush' on or off.
   * <p>
   * When enabled (default), {@link Put} operations don't get buffered/delayed
   * and are immediately executed.  This is slower but safer.
   * <p>
   * Turning this off means that multiple {@link Put}s will be accepted before
   * any RPC is actually sent to do the write operations.  If the application
   * dies before pending writes get flushed to HBase, data will be lost.
   * Other side effects may include the fact that the application thinks a
   * {@link Put} was executed successfully whereas it was in fact only
   * buffered and the operation may fail when attempting to flush all pending
   * writes.  In that case though, the code will retry the failed {@link Put}
   * upon its next attempt to flush the buffer.
   *
   * @param autoFlush Whether or not to enable 'auto-flush'.
   * @see #flushCommits
   */
  public void setAutoFlush(boolean autoFlush) {
    this.autoFlush = autoFlush;
  }

  /**
   * Returns the maximum size in bytes of the write buffer for this DBTable.
   * <p>
   * The default value comes from the configuration parameter
   * {@code bigdb.client.write.buffer}.
   * @return The size of the write buffer in bytes.
   */
  public long getWriteBufferSize() {
    return writeBufferSize;
  }

  /**
   * Sets the size of the buffer in bytes.
   * <p>
   * If the new size is less than the current amount of data in the
   * write buffer, the buffer gets flushed.
   * @param writeBufferSize The new write buffer size, in bytes.
   * @throws IOException if a remote or network exception occurs.
   */
  public void setWriteBufferSize(long writeBufferSize) throws IOException {
    this.writeBufferSize = writeBufferSize;
    if(currentWriteBufferSize > writeBufferSize) {
      flushCommits();
    }
  }

  /**
   * Returns the write buffer.
   * @return The current write buffer.
   */
  public ArrayList<Put> getWriteBuffer() {
    return writeBuffer;
  }

  /**
   * Implements the scanner interface for the HBase client.
   * If there are multiple regions in a table, this scanner will iterate
   * through them all.
   */
  protected class ClientScanner implements ResultScanner {
    private final Logger CLIENT_LOG = Logger.getLogger(this.getClass());
    // HEADSUP: The scan internal start row can change as we move through table.
    private Scan scan;
    private boolean closed = false;
    // Current region scanner is against.  Gets cleared if current region goes
    // wonky: e.g. if it splits on us.
    private DBRegionInfo currentRegion = null;
    private ScannerCallable callable = null;
    private final LinkedList<Result> cache = new LinkedList<Result>();
    private final int caching;
    private long lastNext;
    // Keep lastResult returned successfully in case we have to reset scanner.
    private Result lastResult = null;

    protected ClientScanner(final Scan scan) {
      if (CLIENT_LOG.isDebugEnabled()) {
        CLIENT_LOG.debug("Creating scanner over "
            + Bytes.toString(getTableName())
            + " starting at key '" + Bytes.toStringBinary(scan.getStartRow()) + "'");
      }
      this.scan = scan;
      this.lastNext = System.currentTimeMillis();

      // Use the caching from the Scan.  If not set, use the default cache setting for this table.
      if (this.scan.getCaching() > 0) {
        this.caching = this.scan.getCaching();
      } else {
        this.caching = DBTable.this.scannerCaching;
      }

      // Removed filter validation.  We have a new format now, only one of all
      // the current filters has a validate() method.  We can add it back,
      // need to decide on what we're going to do re: filter redesign.
      // Need, at the least, to break up family from qualifier as separate
      // checks, I think it's important server-side filters are optimal in that
      // respect.
    }

    public void initialize() throws IOException {
      nextScanner(this.caching, false);
    }

    protected Scan getScan() {
      return scan;
    }

    protected long getTimestamp() {
      return lastNext;
    }

    // returns true if the passed region endKey
    private boolean checkScanStopRow(final byte [] endKey) {
      if (this.scan.getStopRow().length > 0) {
        // there is a stop row, check to see if we are past it.
        byte [] stopRow = scan.getStopRow();
        int cmp = Bytes.compareTo(stopRow, 0, stopRow.length,
          endKey, 0, endKey.length);
        if (cmp <= 0) {
          // stopRow <= endKey (endKey is equals to or larger than stopRow)
          // This is a stop.
          return true;
        }
      }
      return false; //unlikely.
    }

    /*
     * Gets a scanner for the next region.  If this.currentRegion != null, then
     * we will move to the endrow of this.currentRegion.  Else we will get
     * scanner at the scan.getStartRow().  We will go no further, just tidy
     * up outstanding scanners, if <code>currentRegion != null</code> and
     * <code>done</code> is true.
     * @param nbRows
     * @param done Server-side says we're done scanning.
     */
    private boolean nextScanner(int nbRows, final boolean done)
    throws IOException {
      // Close the previous scanner if it's open
      if (this.callable != null) {
        this.callable.setClose();
        getConnection().getRegionServerWithRetries(callable);
        this.callable = null;
      }

      // Where to start the next scanner
      byte [] localStartKey;

      // if we're at end of table, close and return false to stop iterating
      if (this.currentRegion != null) {
        byte [] endKey = this.currentRegion.getEndKey();
        if (endKey == null ||
            Bytes.equals(endKey, DBConstants.EMPTY_BYTE_ARRAY) ||
            checkScanStopRow(endKey) ||
            done) {
          close();
          if (CLIENT_LOG.isDebugEnabled()) {
            CLIENT_LOG.debug("Finished with scanning at " + this.currentRegion);
          }
          return false;
        }
        localStartKey = endKey;
        if (CLIENT_LOG.isDebugEnabled()) {
          CLIENT_LOG.debug("Finished with region " + this.currentRegion);
        }
      } else {
        localStartKey = this.scan.getStartRow();
      }

      if (CLIENT_LOG.isDebugEnabled()) {
        CLIENT_LOG.debug("Advancing internal scanner to startKey at '" +
          Bytes.toStringBinary(localStartKey) + "'");
      }
      try {
        callable = getScannerCallable(localStartKey, nbRows);
        // Open a scanner on the region server starting at the
        // beginning of the region
        getConnection().getRegionServerWithRetries(callable);
        this.currentRegion = callable.getDBRegionInfo();
      } catch (IOException e) {
        close();
        throw e;
      }
      return true;
    }

    protected ScannerCallable getScannerCallable(byte [] localStartKey,
        int nbRows) {
      scan.setStartRow(localStartKey);
      ScannerCallable s = new ScannerCallable(getConnection(),
        getTableName(), scan);
      s.setCaching(nbRows);
      return s;
    }

    public Result next() throws IOException {
      // If the scanner is closed but there is some rows left in the cache,
      // it will first empty it before returning null
      if (cache.size() == 0 && this.closed) {
        return null;
      }
      if (cache.size() == 0) {
        Result [] values = null;
        long remainingResultSize = maxScannerResultSize;
        int countdown = this.caching;
        // We need to reset it if it's a new callable that was created
        // with a countdown in nextScanner
        callable.setCaching(this.caching);
        // This flag is set when we want to skip the result returned.  We do
        // this when we reset scanner because it split under us.
        boolean skipFirst = false;
        do {
          try {
            // Server returns a null values if scanning is to stop.  Else,
            // returns an empty array if scanning is to go on and we've just
            // exhausted current region.
            values = getConnection().getRegionServerWithRetries(callable);
            if (skipFirst) {
              skipFirst = false;
              // Reget.
              values = getConnection().getRegionServerWithRetries(callable);
            }
          } catch (DoNotRetryIOException e) {
            if (e instanceof UnknownScannerException) {
              long timeout = lastNext + scannerTimeout;
              // If we are over the timeout, throw this exception to the client
              // Else, it's because the region moved and we used the old id
              // against the new region server; reset the scanner.
              if (timeout < System.currentTimeMillis()) {
                long elapsed = System.currentTimeMillis() - lastNext;
                ScannerTimeoutException ex = new ScannerTimeoutException(
                    elapsed + "ms passed since the last invocation, " +
                        "timeout is currently set to " + scannerTimeout);
                ex.initCause(e);
                throw ex;
              }
            } else {
              Throwable cause = e.getCause();
              if (cause == null || !(cause instanceof NotServingRegionException)) {
                throw e;
              }
            }
            // Else, its signal from depths of ScannerCallable that we got an
            // NSRE on a next and that we need to reset the scanner.
            if (this.lastResult != null) {
              this.scan.setStartRow(this.lastResult.getRow());
              // Skip first row returned.  We already let it out on previous
              // invocation.
              skipFirst = true;
            }
            // Clear region
            this.currentRegion = null;
            continue;
          }
          lastNext = System.currentTimeMillis();
          if (values != null && values.length > 0) {
            for (Result rs : values) {
              cache.add(rs);
              for (KeyValue kv : rs.raw()) {
                  remainingResultSize -= kv.heapSize();
              }
              countdown--;
              this.lastResult = rs;
            }
          }
          // Values == null means server-side filter has determined we must STOP
        } while (remainingResultSize > 0 && countdown > 0 && nextScanner(countdown, values == null));
      }

      if (cache.size() > 0) {
        return cache.poll();
      }
      return null;
    }

    /**
     * Get <param>nbRows</param> rows.
     * How many RPCs are made is determined by the {@link Scan#setCaching(int)}
     * setting (or bigdb.client.scanner.caching in bigdb-site.xml).
     * @param nbRows number of rows to return
     * @return Between zero and <param>nbRows</param> RowResults.  Scan is done
     * if returned array is of zero-length (We never return null).
     * @throws IOException
     */
    public Result [] next(int nbRows) throws IOException {
      // Collect values to be returned here
      ArrayList<Result> resultSets = new ArrayList<Result>(nbRows);
      for(int i = 0; i < nbRows; i++) {
        Result next = next();
        if (next != null) {
          resultSets.add(next);
        } else {
          break;
        }
      }
      return resultSets.toArray(new Result[resultSets.size()]);
    }

    public void close() {
      if (callable != null) {
        callable.setClose();
        try {
          getConnection().getRegionServerWithRetries(callable);
        } catch (IOException e) {
          // We used to catch this error, interpret, and rethrow. However, we
          // have since decided that it's not nice for a scanner's close to
          // throw exceptions. Chances are it was just an UnknownScanner
          // exception due to lease time out.
        }
        callable = null;
      }
      closed = true;
    }

    public Iterator<Result> iterator() {
      return new Iterator<Result>() {
        // The next RowResult, possibly pre-read
        Result next = null;

        // return true if there is another item pending, false if there isn't.
        // this method is where the actual advancing takes place, but you need
        // to call next() to consume it. hasNext() will only advance if there
        // isn't a pending next().
        public boolean hasNext() {
          if (next == null) {
            try {
              next = ClientScanner.this.next();
              return next != null;
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
          return true;
        }

        // get the pending next item and advance the iterator. returns null if
        // there is no next item.
        public Result next() {
          // since hasNext() does the real advancing, we call this to determine
          // if there is a next before proceeding.
          if (!hasNext()) {
            return null;
          }

          // if we get to here, then hasNext() has given us an item to return.
          // we want to return the item and then null out the next pointer, so
          // we use a temporary variable.
          Result temp = next;
          next = null;
          return temp;
        }

        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  static class DaemonThreadFactory implements ThreadFactory {
    static final AtomicInteger poolNumber = new AtomicInteger(1);
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        DaemonThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null)? s.getThreadGroup() :
                                 Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" +
                          poolNumber.getAndIncrement() +
                         "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            if (!t.isDaemon())
                t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
  }

  /**
   * Enable or disable region cache prefetch for the table. It will be
   * applied for the given table's all DBTable instances who share the same
   * connection. By default, the cache prefetch is enabled.
   * @param tableName name of table to configure.
   * @param enable Set to true to enable region cache prefetch. Or set to
   * false to disable it.
   */
  public static void setRegionCachePrefetch(final byte[] tableName,
      boolean enable) {
    DBConnectionManager.getConnection(ConfigurationFactory.get()).
    setRegionCachePrefetch(tableName, enable);
  }

  /**
   * Enable or disable region cache prefetch for the table. It will be
   * applied for the given table's all DBTable instances who share the same
   * connection. By default, the cache prefetch is enabled.
   * @param conf The Configuration object to use.
   * @param tableName name of table to configure.
   * @param enable Set to true to enable region cache prefetch. Or set to
   * false to disable it.
   */
  public static void setRegionCachePrefetch(final Configuration conf,
      final byte[] tableName, boolean enable) {
    DBConnectionManager.getConnection(conf).setRegionCachePrefetch(
        tableName, enable);
  }

  /**
   * Check whether region cache prefetch is enabled or not for the table.
   * @param conf The Configuration object to use.
   * @param tableName name of table to check
   * @return true if table's region cache prefecth is enabled. Otherwise
   * it is disabled.
   */
  public static boolean getRegionCachePrefetch(final Configuration conf,
      final byte[] tableName) {
    return DBConnectionManager.getConnection(conf).getRegionCachePrefetch(
        tableName);
  }

  /**
   * Check whether region cache prefetch is enabled or not for the table.
   * @param tableName name of table to check
   * @return true if table's region cache prefecth is enabled. Otherwise
   * it is disabled.
   */
  public static boolean getRegionCachePrefetch(final byte[] tableName) {
    return DBConnectionManager.getConnection(ConfigurationFactory.get()).
    getRegionCachePrefetch(tableName);
  }
}

