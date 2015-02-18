package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.DBRegionLocation;
import org.javenstudio.raptor.bigdb.DBServerAddress;
import org.javenstudio.raptor.bigdb.DBTableDescriptor;
import org.javenstudio.raptor.bigdb.MasterNotRunningException;
import org.javenstudio.raptor.bigdb.ipc.DBMasterInterface;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.paxos.PaxosWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Cluster connection.
 * {@link DBConnectionManager} manages instances of this class.
 */
public interface DBConnection {
  /**
   * Retrieve PaxosWrapper used by the connection.
   * @return PaxosWrapper handle being used by the connection.
   * @throws IOException if a remote or network exception occurs
   */
  public PaxosWrapper getPaxosWrapper() throws IOException;

  /**
   * @return proxy connection to master server for this instance
   * @throws MasterNotRunningException if the master is not running
   */
  public DBMasterInterface getMaster() throws MasterNotRunningException;

  /** @return - true if the master server is running */
  public boolean isMasterRunning();

  /**
   * Checks if <code>tableName</code> exists.
   * @param tableName Table to check.
   * @return True if table exists already.
   * @throws MasterNotRunningException if the master is not running
   */
  public boolean tableExists(final byte [] tableName)
      throws MasterNotRunningException;

  /**
   * A table that isTableEnabled == false and isTableDisabled == false
   * is possible. This happens when a table has a lot of regions
   * that must be processed.
   * @param tableName table name
   * @return true if the table is enabled, false otherwise
   * @throws IOException if a remote or network exception occurs
   */
  public boolean isTableEnabled(byte[] tableName) throws IOException;

  /**
   * @param tableName table name
   * @return true if the table is disabled, false otherwise
   * @throws IOException if a remote or network exception occurs
   */
  public boolean isTableDisabled(byte[] tableName) throws IOException;

  /**
   * @param tableName table name
   * @return true if all regions of the table are available, false otherwise
   * @throws IOException if a remote or network exception occurs
   */
  public boolean isTableAvailable(byte[] tableName) throws IOException;

  /**
   * List all the userspace tables.  In other words, scan the META table.
   *
   * If we wanted this to be really fast, we could implement a special
   * catalog table that just contains table names and their descriptors.
   * Right now, it only exists as part of the META table's region info.
   *
   * @return - returns an array of DBTableDescriptors
   * @throws IOException if a remote or network exception occurs
   */
  public DBTableDescriptor[] listTables() throws IOException;

  /**
   * @param tableName table name
   * @return table metadata
   * @throws IOException if a remote or network exception occurs
   */
  public DBTableDescriptor getDBTableDescriptor(byte[] tableName)
    throws IOException;

  /**
   * Find the location of the region of <i>tableName</i> that <i>row</i>
   * lives in.
   * @param tableName name of the table <i>row</i> is in
   * @param row row key you're trying to find the region of
   * @return DBRegionLocation that describes where to find the reigon in
   * question
   * @throws IOException if a remote or network exception occurs
   */
  public DBRegionLocation locateRegion(final byte [] tableName,
      final byte [] row) throws IOException;

  /**
   * Allows flushing the region cache.
   */
  public void clearRegionCache();

  /**
   * Find the location of the region of <i>tableName</i> that <i>row</i>
   * lives in, ignoring any value that might be in the cache.
   * @param tableName name of the table <i>row</i> is in
   * @param row row key you're trying to find the region of
   * @return DBRegionLocation that describes where to find the reigon in
   * question
   * @throws IOException if a remote or network exception occurs
   */
  public DBRegionLocation relocateRegion(final byte [] tableName,
      final byte [] row) throws IOException;

  /**
   * Establishes a connection to the region server at the specified address.
   * @param regionServer - the server to connect to
   * @return proxy for DBRegionServer
   * @throws IOException if a remote or network exception occurs
   */
  public DBRegionInterface getDBRegionConnection(DBServerAddress regionServer)
    throws IOException;

  /**
   * Establishes a connection to the region server at the specified address.
   * @param regionServer - the server to connect to
   * @param getMaster - do we check if master is alive
   * @return proxy for DBRegionServer
   * @throws IOException if a remote or network exception occurs
   */
  public DBRegionInterface getDBRegionConnection(
      DBServerAddress regionServer, boolean getMaster) throws IOException;

  /**
   * Find region location hosting passed row
   * @param tableName table name
   * @param row Row to find.
   * @param reload If true do not use cache, otherwise bypass.
   * @return Location of row.
   * @throws IOException if a remote or network exception occurs
   */
  DBRegionLocation getRegionLocation(byte [] tableName, byte [] row,
    boolean reload) throws IOException;

  /**
   * Pass in a ServerCallable with your particular bit of logic defined and
   * this method will manage the process of doing retries with timed waits
   * and refinds of missing regions.
   *
   * @param <T> the type of the return value
   * @param callable callable to run
   * @return an object of type T
   * @throws IOException if a remote or network exception occurs
   * @throws RuntimeException other unspecified error
   */
  public <T> T getRegionServerWithRetries(ServerCallable<T> callable)
    throws IOException, RuntimeException;

  /**
   * Pass in a ServerCallable with your particular bit of logic defined and
   * this method will pass it to the defined region server.
   * @param <T> the type of the return value
   * @param callable callable to run
   * @return an object of type T
   * @throws IOException if a remote or network exception occurs
   * @throws RuntimeException other unspecified error
   */
  public <T> T getRegionServerWithoutRetries(ServerCallable<T> callable) 
    throws IOException, RuntimeException;


  /**
   * Process a batch of Puts. Does the retries.
   * @param list A batch of Puts to process.
   * @param tableName The name of the table
   * @return Count of committed Puts.  On fault, < list.size().
   * @throws IOException if a remote or network exception occurs
   */
  public int processBatchOfRows(ArrayList<Put> list, byte[] tableName)
    throws IOException;

  /**
   * Process a batch of Deletes. Does the retries.
   * @param list A batch of Deletes to process.
   * @return Count of committed Deletes. On fault, < list.size().
   * @param tableName The name of the table
   * @throws IOException if a remote or network exception occurs
   */
  public int processBatchOfDeletes(List<Delete> list, byte[] tableName)
    throws IOException;

  public void processBatchOfPuts(List<Put> list,
	final byte[] tableName, ExecutorService pool) throws IOException;

  /**
   * Enable or disable region cache prefetch for the table. It will be
   * applied for the given table's all HTable instances within this
   * connection. By default, the cache prefetch is enabled.
   * @param tableName name of table to configure.
   * @param enable Set to true to enable region cache prefetch.
   */
  public void setRegionCachePrefetch(final byte[] tableName,
      final boolean enable);

  /**
   * Check whether region cache prefetch is enabled or not.
   * @param tableName name of table to check
   * @return true if table's region cache prefecth is enabled. Otherwise
   * it is disabled.
   */
  public boolean getRegionCachePrefetch(final byte[] tableName);

  /**
   * Load the region map and warm up the global region cache for the table.
   * @param tableName name of the table to perform region cache prewarm.
   * @param regions a region map.
   */
  public void prewarmRegionCache(final byte[] tableName,
      final Map<DBRegionInfo, DBServerAddress> regions);
}

