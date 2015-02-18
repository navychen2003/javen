package org.javenstudio.raptor.bigdb.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.bigdb.util.Bytes;

/**
 * A simple pool of DBTable instances.<p>
 *
 * Each DBTablePool acts as a pool for all tables.  To use, instantiate an
 * DBTablePool and use {@link #getTable(String)} to get an DBTable from the pool.
 * Once you are done with it, return it to the pool with {@link #putTable(DBTableInterface)}.<p>
 *
 * A pool can be created with a <i>maxSize</i> which defines the most DBTable
 * references that will ever be retained for each table.  Otherwise the default
 * is {@link Integer#MAX_VALUE}.<p>
 */
public class DBTablePool {
  private final Map<String, LinkedList<DBTableInterface>> tables =
      Collections.synchronizedMap(new HashMap<String, LinkedList<DBTableInterface>>());
  private final Configuration config;
  private final int maxSize;
  private DBTableInterfaceFactory tableFactory = new DBTableFactory();

  /**
   * Default Constructor.  Default Configuration and no limit on pool size.
   */
  public DBTablePool() {
    this(ConfigurationFactory.get(), Integer.MAX_VALUE);
  }

  /**
   * Constructor to set maximum versions and use the specified configuration.
   * @param config configuration
   * @param maxSize maximum number of references to keep for each table
   */
  public DBTablePool(Configuration config, int maxSize) {
    this.config = config;
    this.maxSize = maxSize;
  }

  public DBTablePool(Configuration config, int maxSize, DBTableInterfaceFactory tableFactory) {
    this.config = config;
    this.maxSize = maxSize;
    this.tableFactory = tableFactory;
  }

  /**
   * Get a reference to the specified table from the pool.<p>
   *
   * Create a new one if one is not available.
   * @param tableName table name
   * @return a reference to the specified table
   * @throws RuntimeException if there is a problem instantiating the DBTable
   */
  public DBTableInterface getTable(String tableName) {
    LinkedList<DBTableInterface> queue = tables.get(tableName);
    if(queue == null) {
      queue = new LinkedList<DBTableInterface>();
      tables.put(tableName, queue);
      return createDBTable(tableName);
    }
    DBTableInterface table;
    synchronized(queue) {
      table = queue.poll();
    }
    if(table == null) {
      return createDBTable(tableName);
    }
    return table;
  }

  /**
   * Get a reference to the specified table from the pool.<p>
   *
   * Create a new one if one is not available.
   * @param tableName table name
   * @return a reference to the specified table
   * @throws RuntimeException if there is a problem instantiating the DBTable
   */
  public DBTableInterface getTable(byte [] tableName) {
    return getTable(Bytes.toString(tableName));
  }

  /**
   * Puts the specified DBTable back into the pool.<p>
   *
   * If the pool already contains <i>maxSize</i> references to the table,
   * then nothing happens.
   * @param table table
   */
  public void putTable(DBTableInterface table) {
    LinkedList<DBTableInterface> queue = tables.get(Bytes.toString(table.getTableName()));
    synchronized(queue) {
      if(queue.size() >= maxSize) return;
      queue.add(table);
    }
  }

  protected DBTableInterface createDBTable(String tableName) {
    return this.tableFactory.createDBTableInterface(config, Bytes.toBytes(tableName));
  }

  /**
   * Closes all the DBTable instances , belonging to the given table, in the table pool.
   * <p>
   * Note: this is a 'shutdown' of the given table pool and different from
   * {@link #putTable(DBTableInterface)}, that is used to return the table
   * instance to the pool for future re-use.
   *
   * @param tableName
   */
  public void closeTablePool(final String tableName)  {
    Queue<DBTableInterface> queue = tables.get(tableName);
    synchronized (queue) {
      DBTableInterface table = queue.poll();
      while (table != null) {
        this.tableFactory.releaseDBTableInterface(table);
        table = queue.poll();
      }
    }

  }

  /**
   * See {@link #closeTablePool(String)}.
   *
   * @param tableName
   */
  public void closeTablePool(final byte[] tableName)  {
    closeTablePool(Bytes.toString(tableName));
  }

  int getCurrentPoolSize(String tableName) {
    Queue<DBTableInterface> queue = tables.get(tableName);
    synchronized(queue) {
      return queue.size();
    }
  }
}

