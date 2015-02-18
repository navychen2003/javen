package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.bigdb.DBRegionLocation;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Abstract class that implements Callable, used by retryable actions.
 * @param <T> the class that the ServerCallable handles
 */
public abstract class ServerCallable<T> implements Callable<T> {
  protected final DBConnection connection;
  protected final byte [] tableName;
  protected final byte [] row;
  protected DBRegionLocation location;
  protected DBRegionInterface server;

  /**
   * @param connection connection callable is on
   * @param tableName table name callable is on
   * @param row row we are querying
   */
  public ServerCallable(DBConnection connection, byte [] tableName, byte [] row) {
    this.connection = connection;
    this.tableName = tableName;
    this.row = row;
  }

  /**
   *
   * @param reload set this to true if connection should re-find the region
   * @throws IOException e
   */
  public void instantiateServer(boolean reload) throws IOException {
    this.location = connection.getRegionLocation(tableName, row, reload);
    this.server = connection.getDBRegionConnection(location.getServerAddress());
  }

  /** @return the server name */
  public String getServerName() {
    if (location == null) {
      return null;
    }
    return location.getServerAddress().toString();
  }

  /** @return the region name */
  public byte[] getRegionName() {
    if (location == null) {
      return null;
    }
    return location.getRegionInfo().getRegionName();
  }

  /** @return the row */
  public byte [] getRow() {
    return row;
  }
}
