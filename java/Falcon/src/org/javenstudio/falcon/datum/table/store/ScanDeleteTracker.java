package org.javenstudio.falcon.datum.table.store;

/**
 * This class is responsible for the tracking and enforcement of Deletes
 * during the course of a Scan operation.
 *
 * It only has to enforce Delete and DeleteColumn, since the
 * DeleteFamily is handled at a higher level.
 *
 * <p>
 * This class is utilized through three methods:
 * <ul><li>{@link #add} when encountering a Delete or DeleteColumn
 * <li>{@link #isDeleted} when checking if a Put KeyValue has been deleted
 * <li>{@link #update} when reaching the end of a StoreFile or row for scans
 * <p>
 * This class is NOT thread-safe as queries are never multi-threaded
 */
public class ScanDeleteTracker implements DeleteTracker {

  private long mFamilyStamp = -1L;
  private byte[] mDeleteBuffer = null;
  private int mDeleteOffset = 0;
  private int mDeleteLength = 0;
  private byte mDeleteType = 0;
  private long mDeleteTimestamp = 0L;

  /**
   * Constructor for ScanDeleteTracker
   */
  public ScanDeleteTracker() {
    super();
  }

  /**
   * Add the specified KeyValue to the list of deletes to check against for
   * this row operation.
   * <p>
   * This is called when a Delete is encountered in a StoreFile.
   * @param buffer KeyValue buffer
   * @param qualifierOffset column qualifier offset
   * @param qualifierLength column qualifier length
   * @param timestamp timestamp
   * @param type delete type as byte
   */
  @Override
  public void add(byte[] buffer, int qualifierOffset, int qualifierLength,
      long timestamp, byte type) {
    if (timestamp > mFamilyStamp) {
      if (type == KeyValue.Type.DeleteFamily.getCode()) {
        mFamilyStamp = timestamp;
        return;
      }

      if (mDeleteBuffer != null && type < mDeleteType) {
        // same column, so ignore less specific delete
        if (Bytes.compareTo(mDeleteBuffer, mDeleteOffset, mDeleteLength,
            buffer, qualifierOffset, qualifierLength) == 0){
          return;
        }
      }
      
      // new column, or more general delete type
      mDeleteBuffer = buffer;
      mDeleteOffset = qualifierOffset;
      mDeleteLength = qualifierLength;
      mDeleteType = type;
      mDeleteTimestamp = timestamp;
    }
    // missing else is never called.
  }

  /**
   * Check if the specified KeyValue buffer has been deleted by a previously
   * seen delete.
   *
   * @param buffer KeyValue buffer
   * @param qualifierOffset column qualifier offset
   * @param qualifierLength column qualifier length
   * @param timestamp timestamp
   * @return true is the specified KeyValue is deleted, false if not
   */
  @Override
  public boolean isDeleted(byte[] buffer, int qualifierOffset,
      int qualifierLength, long timestamp) {
    if (timestamp <= mFamilyStamp) 
      return true;

    if (mDeleteBuffer != null) {
      int ret = Bytes.compareTo(mDeleteBuffer, mDeleteOffset, mDeleteLength,
          buffer, qualifierOffset, qualifierLength);

      if (ret == 0) {
        if (mDeleteType == KeyValue.Type.DeleteColumn.getCode()) 
          return true;
        
        // Delete (aka DeleteVersion)
        // If the timestamp is the same, keep this one
        if (timestamp == mDeleteTimestamp) 
          return true;
        
        // use assert or not?
        assert timestamp < mDeleteTimestamp;

        // different timestamp, let's clear the buffer.
        mDeleteBuffer = null;
      } else if(ret < 0){
        // Next column case.
        mDeleteBuffer = null;
      } else {
        //Should never happen, throw Exception
      }
    }

    return false;
  }

  @Override
  public boolean isEmpty() {
    return mDeleteBuffer == null && mFamilyStamp == 0;
  }

  //called between every row.
  @Override
  public void reset() {
    mFamilyStamp = 0L;
    mDeleteBuffer = null;
  }

  //should not be called at all even (!)
  @Override
  public void update() {
    this.reset();
  }
}
