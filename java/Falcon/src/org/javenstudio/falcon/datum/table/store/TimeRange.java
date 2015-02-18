package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.Writable;

/**
 * Represents an interval of version timestamps.
 * <p>
 * Evaluated according to minStamp <= timestamp < maxStamp
 * or [minStamp,maxStamp) in interval notation.
 * <p>
 * Only used internally; should not be accessed directly by clients.
 */
public class TimeRange implements Writable {

  private long mMinStamp = 0L;
  private long mMaxStamp = Long.MAX_VALUE;
  private boolean mAllTime = false;

  /**
   * Default constructor.
   * Represents interval [0, Long.MAX_VALUE) (allTime)
   */
  public TimeRange() {
    this.mAllTime = true;
  }

  /**
   * Represents interval [minStamp, Long.MAX_VALUE)
   * @param minStamp the minimum timestamp value, inclusive
   */
  public TimeRange(long minStamp) {
    this.mMinStamp = minStamp;
  }

  /**
   * Represents interval [minStamp, Long.MAX_VALUE)
   * @param minStamp the minimum timestamp value, inclusive
   */
  public TimeRange(byte[] minStamp) {
  	this.mMinStamp = Bytes.toLong(minStamp);
  }

  /**
   * Represents interval [minStamp, maxStamp)
   * @param minStamp the minimum timestamp, inclusive
   * @param maxStamp the maximum timestamp, exclusive
   * @throws IOException
   */
  public TimeRange(long minStamp, long maxStamp)
      throws IOException {
    if (maxStamp < minStamp) 
      throw new IOException("maxStamp is smaller than minStamp");
    
    this.mMinStamp = minStamp;
    this.mMaxStamp = maxStamp;
  }

  /**
   * Represents interval [minStamp, maxStamp)
   * @param minStamp the minimum timestamp, inclusive
   * @param maxStamp the maximum timestamp, exclusive
   * @throws IOException
   */
  public TimeRange(byte[] minStamp, byte[] maxStamp)
      throws IOException {
    this(Bytes.toLong(minStamp), Bytes.toLong(maxStamp));
  }

  /**
   * @return the smallest timestamp that should be considered
   */
  public long getMin() {
    return mMinStamp;
  }

  /**
   * @return the biggest timestamp that should be considered
   */
  public long getMax() {
    return mMaxStamp;
  }

  /**
   * Check if the specified timestamp is within this TimeRange.
   * <p>
   * Returns true if within interval [minStamp, maxStamp), false
   * if not.
   * @param bytes timestamp to check
   * @param offset offset into the bytes
   * @return true if within TimeRange, false if not
   */
  public boolean withinTimeRange(byte[] bytes, int offset) {
  	if (mAllTime) return true;
  	return withinTimeRange(Bytes.toLong(bytes, offset));
  }

  /**
   * Check if the specified timestamp is within this TimeRange.
   * <p>
   * Returns true if within interval [minStamp, maxStamp), false
   * if not.
   * @param timestamp timestamp to check
   * @return true if within TimeRange, false if not
   */
  public boolean withinTimeRange(long timestamp) {
  	if (mAllTime) return true;
  	// check if >= minStamp
  	return (mMinStamp <= timestamp && timestamp < mMaxStamp);
  }

  /**
   * Check if the specified timestamp is within this TimeRange.
   * <p>
   * Returns true if within interval [minStamp, maxStamp), false
   * if not.
   * @param timestamp timestamp to check
   * @return true if within TimeRange, false if not
   */
  public boolean withinOrAfterTimeRange(long timestamp) {
    if (mAllTime) return true;
    // check if >= minStamp
    return (timestamp >= mMinStamp);
  }

  /**
   * Compare the timestamp to timerange
   * @param timestamp
   * @return -1 if timestamp is less than timerange,
   * 0 if timestamp is within timerange,
   * 1 if timestamp is greater than timerange
   */
  public int compare(long timestamp) {
    if (timestamp < mMinStamp) {
      return -1;
    } else if (timestamp >= mMaxStamp) {
      return 1;
    } else {
      return 0;
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("maxStamp=");
    sb.append(this.mMaxStamp);
    sb.append(", minStamp=");
    sb.append(this.mMinStamp);
    return sb.toString();
  }

  //Writable
  @Override
  public void readFields(final DataInput in) throws IOException {
    this.mMinStamp = in.readLong();
    this.mMaxStamp = in.readLong();
    this.mAllTime = in.readBoolean();
  }

  @Override
  public void write(final DataOutput out) throws IOException {
    out.writeLong(this.mMinStamp);
    out.writeLong(this.mMaxStamp);
    out.writeBoolean(this.mAllTime);
  }
}
