package org.javenstudio.raptor.bigdb.client;

import java.io.IOException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.DoNotRetryIOException;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.NotServingRegionException;
import org.javenstudio.raptor.bigdb.RemoteExceptionHandler;
import org.javenstudio.raptor.ipc.RemoteException;


/**
 * Retries scanner operations such as create, next, etc.
 * Used by {@link ResultScanner}s made by {@link HTable}.
 */
public class ScannerCallable extends ServerCallable<Result[]> {
  private static final Logger LOG = Logger.getLogger(ScannerCallable.class);
  
  private long scannerId = -1L;
  private boolean instantiated = false;
  private boolean closed = false;
  private Scan scan;
  private int caching = 1;

  /**
   * @param connection which connection
   * @param tableName table callable is on
   * @param scan the scan to execute
   */
  public ScannerCallable (DBConnection connection, byte [] tableName, Scan scan) {
    super(connection, tableName, scan.getStartRow());
    this.scan = scan;
  }

  /**
   * @param reload force reload of server location
   * @throws IOException
   */
  @Override
  public void instantiateServer(boolean reload) throws IOException {
    if (!instantiated || reload) {
      super.instantiateServer(reload);
      instantiated = true;
    }
  }

  /**
   * @see java.util.concurrent.Callable#call()
   */
  public Result [] call() throws IOException {
    if (scannerId != -1L && closed) {
      close();
    } else if (scannerId == -1L && !closed) {
      this.scannerId = openScanner();
    } else {
      Result [] rrs = null;
      try {
        rrs = server.next(scannerId, caching);
      } catch (IOException e) {
        IOException ioe = null;
        if (e instanceof RemoteException) {
          ioe = RemoteExceptionHandler.decodeRemoteException((RemoteException)e);
        }
        if (ioe == null) throw new IOException(e);
        if (ioe instanceof NotServingRegionException) {
          // Throw a DNRE so that we break out of cycle of calling NSRE
          // when what we need is to open scanner against new location.
          // Attach NSRE to signal client that it needs to resetup scanner.
          throw new DoNotRetryIOException("Reset scanner", ioe);
        } else {
          // The outer layers will retry
          throw ioe;
        }
      }
      return rrs;
    }
    return null;
  }

  private void close() {
    if (this.scannerId == -1L) {
      return;
    }
    try {
      this.server.close(this.scannerId);
    } catch (IOException e) {
      LOG.warn("Ignore, probably already closed", e);
    }
    this.scannerId = -1L;
  }

  protected long openScanner() throws IOException {
    return this.server.openScanner(this.location.getRegionInfo().getRegionName(),
      this.scan);
  }

  protected Scan getScan() {
    return scan;
  }

  /**
   * Call this when the next invocation of call should close the scanner
   */
  public void setClose() {
    this.closed = true;
  }

  /**
   * @return the DBRegionInfo for the current region
   */
  public DBRegionInfo getDBRegionInfo() {
    if (!instantiated) {
      return null;
    }
    return location.getRegionInfo();
  }

  /**
   * Get the number of rows that will be fetched on next
   * @return the number of rows for caching
   */
  public int getCaching() {
    return caching;
  }

  /**
   * Set the number of rows that will be fetched on next
   * @param caching the number of rows for caching
   */
  public void setCaching(int caching) {
    this.caching = caching;
  }
}

