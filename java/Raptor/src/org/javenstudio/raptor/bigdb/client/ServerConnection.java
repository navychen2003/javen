package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.bigdb.DBRegionLocation;

/**
 * Used by master and region server, so that they do not need to wait for the
 * cluster to be up to get a connection.
 */
public interface ServerConnection extends DBConnection {
  /**
   * Set root region location in connection
   * @param rootRegion region location for root region
   */
  public void setRootRegionLocation(DBRegionLocation rootRegion);

  /**
   * Unset the root region location in the connection. Called by
   * ServerManager.processRegionClose.
   */
  public void unsetRootRegionLocation();
}

