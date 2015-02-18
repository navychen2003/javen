package org.javenstudio.raptor.bigdb.regionserver;

import org.javenstudio.raptor.bigdb.DBRegionInfo;

/**
 * Add and remove online regions.
 */
interface OnlineRegions {
  /**
   * Add to online regions.
   * @param r
   */
  void addToOnlineRegions(final DBRegion r);

  /**
   * This method removes DBRegion corresponding to hri from the Map of onlineRegions.
   *
   * @param hri the DBRegionInfo corresponding to the DBRegion to-be-removed.
   * @return the removed DBRegion, or null if the DBRegion was not in onlineRegions.
   */
  DBRegion removeFromOnlineRegions(DBRegionInfo hri);
}

