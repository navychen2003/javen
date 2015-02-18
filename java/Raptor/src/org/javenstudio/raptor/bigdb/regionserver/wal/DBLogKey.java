package org.javenstudio.raptor.bigdb.regionserver.wal;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;

import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.util.Bytes;
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
  private byte [] regionName;
  private byte [] tablename;
  private long logSeqNum;
  // Time at which this edit was written.
  private long writeTime;

  private byte clusterId;

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
  public DBLogKey(final byte [] regionName, final byte [] tablename,
      long logSeqNum, final long now) {
    this.regionName = regionName;
    this.tablename = tablename;
    this.logSeqNum = logSeqNum;
    this.writeTime = now;
    this.clusterId = DBConstants.DEFAULT_CLUSTER_ID;
  }

  //////////////////////////////////////////////////////////////////////////////
  // A bunch of accessors
  //////////////////////////////////////////////////////////////////////////////

  /** @return region name */
  public byte [] getRegionName() {
    return regionName;
  }

  /** @return table name */
  public byte [] getTablename() {
    return tablename;
  }

  /** @return log sequence number */
  public long getLogSeqNum() {
    return logSeqNum;
  }

  void setLogSeqNum(long logSeqNum) {
    this.logSeqNum = logSeqNum;
  }

  /**
   * @return the write time
   */
  public long getWriteTime() {
    return this.writeTime;
  }

  /**
   * Get the id of the original cluster
   * @return
   */
  public byte getClusterId() {
    return clusterId;
  }

  /**
   * Set the cluster id of this key
   * @param clusterId
   */
  public void setClusterId(byte clusterId) {
    this.clusterId = clusterId;
  }

  @Override
  public String toString() {
    return Bytes.toString(tablename) + "/" + Bytes.toString(regionName) + "/" +
      logSeqNum;
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
    int result = Bytes.hashCode(this.regionName);
    result ^= this.logSeqNum;
    result ^= this.writeTime;
    result ^= this.clusterId;
    return result;
  }

  public int compareTo(DBLogKey o) {
    int result = Bytes.compareTo(this.regionName, o.regionName);
    if (result == 0) {
      if (this.logSeqNum < o.logSeqNum) {
        result = -1;
      } else if (this.logSeqNum > o.logSeqNum) {
        result = 1;
      }
      if (result == 0) {
        if (this.writeTime < o.writeTime) {
          result = -1;
        } else if (this.writeTime > o.writeTime) {
          return 1;
        }
      }
    }
    return result;
  }

  public void write(DataOutput out) throws IOException {
    Bytes.writeByteArray(out, this.regionName);
    Bytes.writeByteArray(out, this.tablename);
    out.writeLong(this.logSeqNum);
    out.writeLong(this.writeTime);
    out.writeByte(this.clusterId);
  }

  public void readFields(DataInput in) throws IOException {
    this.regionName = Bytes.readByteArray(in);
    this.tablename = Bytes.readByteArray(in);
    this.logSeqNum = in.readLong();
    this.writeTime = in.readLong();
    try {
      this.clusterId = in.readByte();
    } catch(EOFException e) {
      // Means it's an old key, just continue
    }
  }

}

