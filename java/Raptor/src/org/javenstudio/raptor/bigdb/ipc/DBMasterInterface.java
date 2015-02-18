package org.javenstudio.raptor.bigdb.ipc;

import org.javenstudio.raptor.bigdb.ClusterStatus;
import org.javenstudio.raptor.bigdb.DBColumnDescriptor;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBTableDescriptor;
import org.javenstudio.raptor.io.Writable;

import java.io.IOException;

/**
 * Clients interact with the DBMasterInterface to gain access to meta-level
 * BigDB functionality, like finding an HRegionServer and creating/destroying
 * tables.
 *
 * <p>NOTE: if you change the interface, you must change the RPC version
 * number in DBRPCProtocolVersion
 *
 */
public interface DBMasterInterface extends DBRPCProtocolVersion {

  /** @return true if master is available */
  public boolean isMasterRunning();

  // Admin tools would use these cmds

  /**
   * Creates a new table.  If splitKeys are specified, then the table will be
   * created with an initial set of multiple regions.  If splitKeys is null,
   * the table will be created with a single region.
   * @param desc table descriptor
   * @param splitKeys
   * @throws IOException
   */
  public void createTable(DBTableDescriptor desc, byte [][] splitKeys)
    throws IOException;

  /**
   * Deletes a table
   * @param tableName table to delete
   * @throws IOException e
   */
  public void deleteTable(final byte [] tableName) throws IOException;

  /**
   * Adds a column to the specified table
   * @param tableName table to modify
   * @param column column descriptor
   * @throws IOException e
   */
  public void addColumn(final byte [] tableName, DBColumnDescriptor column)
    throws IOException;

  /**
   * Modifies an existing column on the specified table
   * @param tableName table name
   * @param columnName name of the column to edit
   * @param descriptor new column descriptor
   * @throws IOException e
   */
  public void modifyColumn(final byte [] tableName, final byte [] columnName,
    DBColumnDescriptor descriptor) throws IOException;

  /**
   * Deletes a column from the specified table. Table must be disabled.
   * @param tableName table to alter
   * @param columnName column family to remove
   * @throws IOException e
   */
  public void deleteColumn(final byte [] tableName, final byte [] columnName)
    throws IOException;

  /**
   * Puts the table on-line (only needed if table has been previously taken offline)
   * @param tableName table to enable
   * @throws IOException e
   */
  public void enableTable(final byte [] tableName) throws IOException;

  /**
   * Take table offline
   *
   * @param tableName table to take offline
   * @throws IOException e
   */
  public void disableTable(final byte [] tableName) throws IOException;

  /**
   * Modify a table's metadata
   *
   * @param tableName table to modify
   * @param op the operation to do
   * @param args arguments for operation
   * @throws IOException e
   */
  public void modifyTable(byte[] tableName, DBConstants.Modify op, Writable[] args)
    throws IOException;

  /**
   * Shutdown an BigDB cluster.
   * @throws IOException e
   */
  public void shutdown() throws IOException;

  /**
   * Return cluster status.
   * @return status object
   */
  public ClusterStatus getClusterStatus();
}

