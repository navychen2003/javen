package org.javenstudio.raptor.bigdb.client;

/**
 * Holds row name and lock id.
 */
public class RowLock {
  private byte [] row = null;
  private long lockId = -1L;

  /**
   * Creates a RowLock from a row and lock id
   * @param row row to lock on
   * @param lockId the lock id
   */
  public RowLock(final byte [] row, final long lockId) {
    this.row = row;
    this.lockId = lockId;
  }

  /**
   * Creates a RowLock with only a lock id
   * @param lockId lock id
   */
  public RowLock(final long lockId) {
    this.lockId = lockId;
  }

  /**
   * Get the row for this RowLock
   * @return the row
   */
  public byte [] getRow() {
    return row;
  }

  /**
   * Get the lock id from this RowLock
   * @return the lock id
   */
  public long getLockId() {
    return lockId;
  }
}

