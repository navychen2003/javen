package org.javenstudio.raptor.bigdb.master;

import org.javenstudio.raptor.bigdb.DBRegionInfo;

/**
 * Abstract class that performs common operations for
 * @see ProcessRegionClose and @see ProcessRegionOpen
 */
abstract class ProcessRegionStatusChange extends RegionServerOperation {
  protected final boolean isMetaTable;
  protected final DBRegionInfo regionInfo;
  private volatile MetaRegion metaRegion = null;
  protected volatile byte[] metaRegionName = null;

  /**
   * @param master the master
   * @param regionInfo region info
   */
  public ProcessRegionStatusChange(DBMaster master, DBRegionInfo regionInfo) {
    super(master);
    this.regionInfo = regionInfo;
    this.isMetaTable = regionInfo.isMetaTable();
  }

  protected boolean metaRegionAvailable() {
    boolean available = true;
    if (isMetaTable) {
      // This operation is for the meta table
      if (!rootAvailable()) {
        requeue();
        // But we can't proceed unless the root region is available
        available = false;
      }
    } else {
      if (!master.getRegionManager().isInitialRootScanComplete() ||
          !metaTableAvailable()) {
        // The root region has not been scanned or the meta table is not
        // available so we can't proceed.
        // Put the operation on the delayedToDoQueue
        requeue();
        available = false;
      }
    }
    return available;
  }

  protected MetaRegion getMetaRegion() {
    if (isMetaTable) {
      this.metaRegionName = DBRegionInfo.ROOT_REGIONINFO.getRegionName();
      this.metaRegion = new MetaRegion(master.getRegionManager().getRootRegionLocation(),
          DBRegionInfo.ROOT_REGIONINFO);
    } else {
      this.metaRegion =
        master.getRegionManager().getFirstMetaRegionForRegion(regionInfo);
      if (this.metaRegion != null) {
        this.metaRegionName = this.metaRegion.getRegionName();
      }
    }
    return this.metaRegion;
  }
  
  public DBRegionInfo getRegionInfo() {
    return regionInfo;
  }
}
