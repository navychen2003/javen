package org.javenstudio.raptor.bigdb.master;

import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.regionserver.DBRegion;

import java.io.IOException;

/**
 * ProcessRegionClose is the way we do post-processing on a closed region. We
 * only spawn one of these asynchronous tasks when the region needs to be
 * either offlined or deleted. We used to create one of these tasks whenever
 * a region was closed, but since closing a region that isn't being offlined
 * or deleted doesn't actually require post processing, it's no longer
 * necessary.
 */
public class ProcessRegionClose extends ProcessRegionStatusChange {
  protected final boolean offlineRegion;
  protected final boolean reassignRegion;

  /**
  * @param master
  * @param regionInfo Region to operate on
  * @param offlineRegion if true, set the region to offline in meta
  * @param reassignRegion if true, region is to be reassigned
  */
  public ProcessRegionClose(DBMaster master, DBRegionInfo regionInfo,
      boolean offlineRegion, boolean reassignRegion) {

   super(master, regionInfo);
   this.offlineRegion = offlineRegion;
   this.reassignRegion = reassignRegion;
  }

  @Override
  public String toString() {
    return "ProcessRegionClose of " + this.regionInfo.getRegionNameAsString() +
      ", " + this.offlineRegion + ", reassign: " + this.reassignRegion;
  }

  @Override
  protected boolean process() throws IOException {
    if (!metaRegionAvailable()) {
      // We can't proceed unless the meta region we are going to update
      // is online. metaRegionAvailable() has put this operation on the
      // delayedToDoQueue, so return true so the operation is not put
      // back on the toDoQueue
      return true;
    }
    Boolean result = null;
    if (offlineRegion || reassignRegion) {
      result =
        new RetryableMetaOperation<Boolean>(getMetaRegion(), this.master) {
          public Boolean call() throws IOException {


            // We can't proceed unless the meta region we are going to update
            // is online. metaRegionAvailable() will put this operation on the
            // delayedToDoQueue, so return true so the operation is not put
            // back on the toDoQueue

            if (metaRegionAvailable()) {
              if(offlineRegion) {
                // offline the region in meta and then remove it from the
                // set of regions in transition
                DBRegion.offlineRegionInMETA(server, metaRegionName,
                    regionInfo);
                master.getRegionManager().removeRegion(regionInfo);
                LOG.info("region closed: " + regionInfo.getRegionNameAsString());
              } else {
                // we are reassigning the region eventually, so set it unassigned
                // and remove the server info
                DBRegion.cleanRegionInMETA(server, metaRegionName,
                    regionInfo);
                master.getRegionManager().setUnassigned(regionInfo, false);
                LOG.info("region set as unassigned: " + regionInfo.getRegionNameAsString());
              }
            }
            return true;
          }
        }.doWithRetries();
        result = result == null ? true : result;

    } else {
      LOG.info("Region was neither offlined, or asked to be reassigned, what gives: " +
      regionInfo.getRegionNameAsString());
    }

    return result == null ? true : result;
  }
}

