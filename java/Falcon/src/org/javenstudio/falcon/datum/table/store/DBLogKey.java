package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;

import org.javenstudio.raptor.io.WritableComparable;

/**
 * A Key for an entry in the change log.
 *
 * The log intermingles edits to many tables and rows, so each log entry
 * identifies the appropriate table and row.  Within a table and row, they're
 * also sorted.
 *
 * <p>Some Transactional edits (START, COMMIT, ABORT) will not have an
 * associated row.
 */
public class DBLogKey implements WritableComparable<DBLogKey> {

  private byte[] mRegionName;
  private byte[] mTablename;
  private long mLogSeqNum;
  // Time at which this edit was written.
  private long mWriteTime;
  private byte mClusterId;

  /** Writable Consructor -- Do not use. */
  public DBLogKey() {
    this(null, null, 0L, DBConstants.LATEST_TIMESTAMP);
  }

  /**
   * Create the log key!
   * We maintain the tablename mainly for debugging purposes.
   * A regionName is always a sub-table object.
   *
   * @param regionName  - name of region
   * @param tablename   - name of table
   * @param logSeqNum   - log sequence number
   * @param now Time at which this edit was written.
   */
  public DBLogKey(final byte[] regionName, final byte[] tablename,
      long logSeqNum, final long now) {
    this.mRegionName = regionName;
    this.mTablename = tablename;
    this.mLogSeqNum = logSeqNum;
    this.mWriteTime = now;
    this.mClusterId = DBConstants.DEFAULT_CLUSTER_ID;
  }

  //////////////////////////////////////////////////////////////////////////////
  // A bunch of accessors
  //////////////////////////////////////////////////////////////////////////////

  /** @return region name */
  public byte[] getRegionName() {
    return mRegionName;
  }

  /** @return table name */
  public byte[] getTablename() {
    return mTablename;
  }

  /** @return log sequence number */
  public long getLogSeqNum() {
    return mLogSeqNum;
  }

  void setLogSeqNum(long logSeqNum) {
    this.mLogSeqNum = logSeqNum;
  }

  /**
   * @return the write time
   */
  public long getWriteTime() {
    return this.mWriteTime;
  }

  /**
   * Get the id of the original cluster
   * @return
   */
  public byte getClusterId() {
    return mClusterId;
  }

  /**
   * Set the cluster id of this key
   * @param clusterId
   */
  public void setClusterId(byte clusterId) {
    this.mClusterId = clusterId;
  }

  @Override
  public String toString() {
    return Bytes.toString(mTablename) + "/" + Bytes.toString(mRegionName) + "/" +
      mLogSeqNum;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return compareTo((DBLogKey)obj) == 0;
  }

  @Override
  public int hashCode() {
    int result = Bytes.hashCode(this.mRegionName);
    result ^= this.mLogSeqNum;
    result ^= this.mWriteTime;
    result ^= this.mClusterId;
    return result;
  }

  @Override
  public int compareTo(DBLogKey o) {
    int result = Bytes.compareTo(this.mRegionName, o.mRegionName);
    if (result == 0) {
      if (this.mLogSeqNum < o.mLogSeqNum) {
        result = -1;
      } else if (this.mLogSeqNum > o.mLogSeqNum) {
        result = 1;
      }
      if (result == 0) {
        if (this.mWriteTime < o.mWriteTime) {
          result = -1;
        } else if (this.mWriteTime > o.mWriteTime) {
          return 1;
        }
      }
    }
    return result;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    Bytes.writeByteArray(out, this.mRegionName);
    Bytes.writeByteArray(out, this.mTablename);
    out.writeLong(this.mLogSeqNum);
    out.writeLong(this.mWriteTime);
    out.writeByte(this.mClusterId);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    this.mRegionName = Bytes.readByteArray(in);
    this.mTablename = Bytes.readByteArray(in);
    this.mLogSeqNum = in.readLong();
    this.mWriteTime = in.readLong();
    try {
      this.mClusterId = in.readByte();
    } catch(EOFException e) {
      // Means it's an old key, just continue
    }
  }

}
