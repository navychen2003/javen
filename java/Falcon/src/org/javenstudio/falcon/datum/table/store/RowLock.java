package org.javenstudio.falcon.datum.table.store;

/**
 * Holds row name and lock id.
 */
public class RowLock {
  private byte[] mRow = null;
  private long mLockId = -1L;

  /**
   * Creates a RowLock from a row and lock id
   * @param row row to lock on
   * @param lockId the lock id
   */
  public RowLock(final byte[] row, final long lockId) {
    this.mRow = row;
    this.mLockId = lockId;
  }

  /**
   * Creates a RowLock with only a lock id
   * @param lockId lock id
   */
  public RowLock(final long lockId) {
    this.mLockId = lockId;
  }

  /**
   * Get the row for this RowLock
   * @return the row
   */
  public byte[] getRow() {
    return mRow;
  }

  /**
   * Get the lock id from this RowLock
   * @return the lock id
   */
  public long getLockId() {
    return mLockId;
  }
}
