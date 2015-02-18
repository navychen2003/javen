package org.javenstudio.raptor.bigdb.master;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.RemoteExceptionHandler;
import org.javenstudio.raptor.bigdb.TableNotDisabledException;
import org.javenstudio.raptor.bigdb.TableNotFoundException;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.bigdb.util.Sleeper;
import org.javenstudio.raptor.ipc.RemoteException;

/**
 * Uses Callable pattern so that operations against meta regions do not need
 * to duplicate retry logic.
 */
abstract class RetryableMetaOperation<T> implements Callable<T> {
  protected final Logger LOG = Logger.getLogger(this.getClass());
  protected final Sleeper sleeper;
  protected final MetaRegion m;
  protected final DBMaster master;

  protected DBRegionInterface server;

  protected RetryableMetaOperation(MetaRegion m, DBMaster master) {
    this.m = m;
    this.master = master;
    this.sleeper = new Sleeper(this.master.getThreadWakeFrequency(),
      this.master.getClosed());
  }

  protected T doWithRetries()
  throws IOException, RuntimeException {
    List<IOException> exceptions = new ArrayList<IOException>();
    for (int tries = 0; tries < this.master.getNumRetries(); tries++) {
      if (this.master.isClosed()) {
        return null;
      }
      try {
        this.server =
          this.master.getServerConnection().getDBRegionConnection(m.getServer());
        return this.call();
      } catch (IOException e) {
        if (e instanceof TableNotFoundException ||
            e instanceof TableNotDisabledException ||
            e instanceof InvalidColumnNameException) {
          throw e;
        }
        if (e instanceof RemoteException) {
          e = RemoteExceptionHandler.decodeRemoteException((RemoteException) e);
        }
        if (tries == this.master.getNumRetries() - 1) {
          if (LOG.isDebugEnabled()) {
            StringBuilder message = new StringBuilder(
                "Trying to contact region server for regionName '" +
                Bytes.toString(m.getRegionName()) + "', but failed after " +
                (tries + 1) + " attempts.\n");
            int i = 1;
            for (IOException e2 : exceptions) {
              message.append("Exception " + i + ":\n" + e2);
            }
            LOG.debug("message: " + message);
          }
          this.master.checkFileSystem();
          throw e;
        }
        if (LOG.isDebugEnabled()) {
          exceptions.add(e);
        }
      } catch (Exception e) {
        LOG.debug("Exception in RetryableMetaOperation: ", e);
        throw new RuntimeException(e);
      }
      this.sleeper.sleep();
    }
    return null;
  }
}
