package org.javenstudio.falcon.datum.table.store;

import java.util.LinkedList;

/**
 * Manages the read/write consistency within memstore. This provides
 * an interface for readers to determine what entries to ignore, and
 * a mechanism for writers to obtain new write numbers, then "commit"
 * the new writes for readers to read (thus forming atomic transactions).
 */
public class ReadWriteConsistencyControl {

  private volatile long mMemstoreRead = 0;
  private volatile long mMemstoreWrite = 0;

  private final Object mReadWaiters = new Object();

  // This is the pending queue of writes.
  private final LinkedList<WriteEntry> mWriteQueue =
      new LinkedList<WriteEntry>();

  private static final ThreadLocal<Long> sPerThreadReadPoint =
      new ThreadLocal<Long>();

  /**
   * Get this thread's read point. Used primarily by the memstore scanner to
   * know which values to skip (ie: have not been completed/committed to 
   * memstore).
   */
  public static long getThreadReadPoint() {
    return sPerThreadReadPoint.get();
  }

  /** 
   * Set the thread read point to the given value. The thread RWCC
   * is used by the Memstore scanner so it knows which values to skip. 
   * Give it a value of 0 if you want everything.
   */
  public static void setThreadReadPoint(long readPoint) {
    sPerThreadReadPoint.set(readPoint);
  }

  /**
   * Set the thread RWCC read point to whatever the current read point is in
   * this particular instance of RWCC.  Returns the new thread read point value.
   */
  public static long resetThreadReadPoint(ReadWriteConsistencyControl rwcc) {
    sPerThreadReadPoint.set(rwcc.memstoreReadPoint());
    return getThreadReadPoint();
  }
  
  /**
   * Set the thread RWCC read point to 0 (include everything).
   */
  public static void resetThreadReadPoint() {
    sPerThreadReadPoint.set(0L);
  }

  public WriteEntry beginMemstoreInsert() {
    synchronized (mWriteQueue) {
      long nextWriteNumber = ++mMemstoreWrite;
      WriteEntry e = new WriteEntry(nextWriteNumber);
      mWriteQueue.add(e);
      return e;
    }
  }

  public void completeMemstoreInsert(WriteEntry e) {
    synchronized (mWriteQueue) {
      e.markCompleted();

      long nextReadValue = -1;
      boolean ranOnce = false;
      
      while (!mWriteQueue.isEmpty()) {
        ranOnce = true;
        WriteEntry queueFirst = mWriteQueue.getFirst();

        if (nextReadValue > 0) {
          if ((nextReadValue+1) != queueFirst.getWriteNumber()) {
            throw new RuntimeException("invariant in completeMemstoreInsert violated, prev: "
                + nextReadValue + " next: " + queueFirst.getWriteNumber());
          }
        }

        if (queueFirst.isCompleted()) {
          nextReadValue = queueFirst.getWriteNumber();
          mWriteQueue.removeFirst();
        } else {
          break;
        }
      }

      if (!ranOnce) {
        throw new RuntimeException("never was a first");
      }

      if (nextReadValue > 0) {
        mMemstoreRead = nextReadValue;

        synchronized (mReadWaiters) {
          mReadWaiters.notifyAll();
        }
      }
    }

    boolean interrupted = false;
    
    while (mMemstoreRead < e.getWriteNumber()) {
      synchronized (mReadWaiters) {
        try {
          mReadWaiters.wait(0);
        } catch (InterruptedException ie) {
          // We were interrupted... finish the loop -- i.e. cleanup --and then
          // on our way out, reset the interrupt flag.
          interrupted = true;
        }
      }
    }
    
    if (interrupted) 
      Thread.currentThread().interrupt();
  }

  public long memstoreReadPoint() {
    return mMemstoreRead;
  }

  public static class WriteEntry {
    private long mWriteNumber;
    private boolean mCompleted = false;
    
    WriteEntry(long writeNumber) {
      this.mWriteNumber = writeNumber;
    }
    
    public void markCompleted() {
      this.mCompleted = true;
    }
    
    public boolean isCompleted() {
      return this.mCompleted;
    }
    
    public long getWriteNumber() {
      return this.mWriteNumber;
    }
  }
}
