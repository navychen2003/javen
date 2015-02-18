package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.conf.Configuration;


/**
 * Defines methods to create new DBTableInterface.
 *
 * @since 0.21.0
 */
public interface DBTableInterfaceFactory {

  /**
   * Creates a new DBTableInterface.
   *
   * @param config HBaseConfiguration instance.
   * @param tableName name of the HBase table.
   * @return DBTableInterface instance.
   */
  DBTableInterface createDBTableInterface(Configuration config, byte[] tableName);


  /**
   * Release the DBTable resource represented by the table.
   * @param table
   */
  void releaseDBTableInterface(final DBTableInterface table);
}

