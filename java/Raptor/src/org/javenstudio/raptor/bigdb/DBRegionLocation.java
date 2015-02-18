package org.javenstudio.raptor.bigdb;

/**
 * Contains the DBRegionInfo for the region and the DBServerAddress for the
 * HRegionServer serving the region
 */
public class DBRegionLocation implements Comparable<DBRegionLocation> {
  private DBRegionInfo regionInfo;
  private DBServerAddress serverAddress;

  /**
   * Constructor
   *
   * @param regionInfo the DBRegionInfo for the region
   * @param serverAddress the DBServerAddress for the region server
   */
  public DBRegionLocation(DBRegionInfo regionInfo, DBServerAddress serverAddress) {
    this.regionInfo = regionInfo;
    this.serverAddress = serverAddress;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "address: " + this.serverAddress.toString() + ", regioninfo: " +
      this.regionInfo;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (!(o instanceof DBRegionLocation)) {
      return false;
    }
    return this.compareTo((DBRegionLocation)o) == 0;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    int result = this.regionInfo.hashCode();
    result ^= this.serverAddress.hashCode();
    return result;
  }

  /** @return DBRegionInfo */
  public DBRegionInfo getRegionInfo(){
    return regionInfo;
  }

  /** @return DBServerAddress */
  public DBServerAddress getServerAddress(){
    return serverAddress;
  }

  //
  // Comparable
  //

  public int compareTo(DBRegionLocation o) {
    int result = this.regionInfo.compareTo(o.regionInfo);
    if(result == 0) {
      result = this.serverAddress.compareTo(o.serverAddress);
    }
    return result;
  }
}
