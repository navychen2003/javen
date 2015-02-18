package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.conf.Configuration;

import java.io.IOException;

/**
 * Factory for creating DBTable instances.
 *
 * @since 0.21.0
 */
public class DBTableFactory implements DBTableInterfaceFactory {

  @Override
  public DBTableInterface createDBTableInterface(Configuration config,
      byte[] tableName) {
    try {
      return new DBTable(config, tableName);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  @Override
  public void releaseDBTableInterface(DBTableInterface table) {
    try {
      table.close();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

  }

}

