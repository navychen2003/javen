package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.conf.Configuration;


/**
 * Used by server processes to expose DBServerConnection method
 * so can call DBConnectionManager#setRootRegionLocation
 */
public class ServerConnectionManager extends DBConnectionManager {
  /*
   * Not instantiable
   */
  private ServerConnectionManager() {}

  /**
   * Get the connection object for the instance specified by the configuration
   * If no current connection exists, create a new connection for that instance
   * @param conf configuration
   * @return DBConnection object for the instance specified by the configuration
   */
  public static ServerConnection getConnection(Configuration conf) {
    return (ServerConnection) DBConnectionManager.getConnection(conf);
  }
}

