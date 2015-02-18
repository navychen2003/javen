package org.javenstudio.raptor.bigdb.master;

import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.RemoteExceptionHandler;

import java.io.IOException;

/** Scanner for the <code>ROOT</code> DBRegion. */
class RootScanner extends BaseScanner {
  /**
   * Constructor
   * @param master
   */
  public RootScanner(DBMaster master) {
    super(master, true, master.getShutdownRequested());
  }

  /**
   * Don't retry if we get an error while scanning. Errors are most often
   *
   * caused by the server going away. Wait until next rescan interval when
   * things should be back to normal.
   * @return True if successfully scanned.
   */
  private boolean scanRoot() {
    master.getRegionManager().waitForRootRegionLocation();
    if (master.isClosed()) {
      return false;
    }

    try {
      // Don't interrupt us while we're working
      synchronized(scannerLock) {
        if (master.getRegionManager().getRootRegionLocation() != null) {
          scanRegion(new MetaRegion(master.getRegionManager().getRootRegionLocation(),
            DBRegionInfo.ROOT_REGIONINFO));
        }
      }
    } catch (IOException e) {
      e = RemoteExceptionHandler.checkIOException(e);
      LOG.warn("Scan ROOT region", e);
      // Make sure the file system is still available
      master.checkFileSystem();
    } catch (Exception e) {
      // If for some reason we get some other kind of exception,
      // at least log it rather than go out silently.
      LOG.error("Unexpected exception", e);
    }
    return true;
  }

  @Override
  protected boolean initialScan() {
    this.initialScanComplete = scanRoot();
    return initialScanComplete;
  }

  @Override
  protected void maintenanceScan() {
    scanRoot();
  }
}
