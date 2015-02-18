package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.Writable;

/**
 * Stores the minimum and maximum timestamp values.
 * Can be used to find if any given time range overlaps with its time range
 * MemStores use this class to track its minimum and maximum timestamps.
 * When writing StoreFiles, this information is stored in meta blocks and used
 * at read time to match against the required TimeRange
 */
public class TimeRangeTracker implements Writable {

  private long mMinimumTimestamp = -1;
  private long mMaximumTimestamp = -1;

  /**
   * Default constructor.
   * Initializes TimeRange to be null
   */
  public TimeRangeTracker() {
  }

  /**
   * Copy Constructor
   * @param trt source TimeRangeTracker
   */
  public TimeRangeTracker(final TimeRangeTracker trt) {
    this.mMinimumTimestamp = trt.getMinimumTimestamp();
    this.mMaximumTimestamp = trt.getMaximumTimestamp();
  }

  public TimeRangeTracker(long minimumTimestamp, long maximumTimestamp) {
    this.mMinimumTimestamp = minimumTimestamp;
    this.mMaximumTimestamp = maximumTimestamp;
  }

  /**
   * Update the current TimestampRange to include the timestamp from KeyValue
   * If the Key is of type DeleteColumn or DeleteFamily, it includes the
   * entire time range from 0 to timestamp of the key.
   * @param kv the KeyValue to include
   */
  public void includeTimestamp(final KeyValue kv) {
    includeTimestamp(kv.getTimestamp());
    if (kv.isDeleteColumnOrFamily()) {
      includeTimestamp(0);
    }
  }

  /**
   * Update the current TimestampRange to include the timestamp from Key.
   * If the Key is of type DeleteColumn or DeleteFamily, it includes the
   * entire time range from 0 to timestamp of the key.
   * @param key
   */
  public void includeTimestamp(final byte[] key) {
    includeTimestamp(Bytes.toLong(key,key.length-KeyValue.TIMESTAMP_TYPE_SIZE));
    int type = key[key.length - 1];
    if (type == KeyValue.Type.DeleteColumn.getCode() ||
        type == KeyValue.Type.DeleteFamily.getCode()) {
      includeTimestamp(0);
    }
  }

  /**
   * If required, update the current TimestampRange to include timestamp
   * @param timestamp the timestamp value to include
   */
  private void includeTimestamp(final long timestamp) {
    if (mMaximumTimestamp == -1) {
      mMinimumTimestamp = timestamp;
      mMaximumTimestamp = timestamp;
    } else if (mMinimumTimestamp > timestamp) {
      mMinimumTimestamp = timestamp;
    } else if (mMaximumTimestamp < timestamp) {
      mMaximumTimestamp = timestamp;
    }
    return;
  }

  /**
   * Check if the range has any overlap with TimeRange
   * @param tr TimeRange
   * @return True if there is overlap, false otherwise
   */
  public boolean includesTimeRange(final TimeRange tr) {
    return (this.mMinimumTimestamp < tr.getMax() &&
        this.mMaximumTimestamp >= tr.getMin());
  }

  /**
   * @return the minimumTimestamp
   */
  public long getMinimumTimestamp() {
    return mMinimumTimestamp;
  }

  /**
   * @return the maximumTimestamp
   */
  public long getMaximumTimestamp() {
    return mMaximumTimestamp;
  }

  @Override
  public void write(final DataOutput out) throws IOException {
    out.writeLong(mMinimumTimestamp);
    out.writeLong(mMaximumTimestamp);
  }

  @Override
  public void readFields(final DataInput in) throws IOException {
    this.mMinimumTimestamp = in.readLong();
    this.mMaximumTimestamp = in.readLong();
  }

}
