package org.javenstudio.raptor.bigdb.master;

import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.DBServerAddress;
import org.javenstudio.raptor.bigdb.util.Bytes;


/** Describes a meta region and its server */
public class MetaRegion implements Comparable<MetaRegion> {
  private final DBServerAddress server;
  private DBRegionInfo regionInfo;

  MetaRegion(final DBServerAddress server, DBRegionInfo regionInfo) {
    if (server == null) {
      throw new IllegalArgumentException("server cannot be null");
    }
    this.server = server;
    if (regionInfo == null) {
      throw new IllegalArgumentException("regionInfo cannot be null");
    }
    this.regionInfo = regionInfo;
  }

  @Override
  public String toString() {
    return "{server: " + this.server.toString() + ", regionname: " +
        regionInfo.getRegionNameAsString() + ", startKey: <" +
        Bytes.toString(regionInfo.getStartKey()) + ">}";
  }

  /** @return the regionName */
  public byte [] getRegionName() {
    return regionInfo.getRegionName();
  }

  /** @return the server */
  public DBServerAddress getServer() {
    return server;
  }

  /** @return the startKey */
  public byte [] getStartKey() {
    return regionInfo.getStartKey();
  }


  /** @return the endKey */
  public byte [] getEndKey() {
    return regionInfo.getEndKey();
  }


  public DBRegionInfo getRegionInfo() {
    return regionInfo;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof MetaRegion && this.compareTo((MetaRegion)o) == 0;
  }

  @Override
  public int hashCode() {
    return regionInfo.hashCode();
  }

  // Comparable

  public int compareTo(MetaRegion other) {
    int cmp = regionInfo.compareTo(other.regionInfo);
    if(cmp == 0) {
      // Might be on different host?
      cmp = this.server.compareTo(other.server);
    }
    return cmp;
  }
}
