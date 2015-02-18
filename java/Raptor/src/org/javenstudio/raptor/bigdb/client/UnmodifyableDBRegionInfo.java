package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.bigdb.DBRegionInfo;

class UnmodifyableDBRegionInfo extends DBRegionInfo {
  /*
   * Creates an unmodifyable copy of an DBRegionInfo
   *
   * @param info
   */
  UnmodifyableDBRegionInfo(DBRegionInfo info) {
    super(info);
    this.tableDesc = new UnmodifyableDBTableDescriptor(info.getTableDesc());
  }

  /**
   * @param split set split status
   */
  @Override
  public void setSplit(boolean split) {
    throw new UnsupportedOperationException("DBRegionInfo is read-only");
  }

  /**
   * @param offLine set online - offline status
   */
  @Override
  public void setOffline(boolean offLine) {
    throw new UnsupportedOperationException("DBRegionInfo is read-only");
  }
}

