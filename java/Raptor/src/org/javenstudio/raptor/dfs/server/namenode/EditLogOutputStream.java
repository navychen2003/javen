package org.javenstudio.raptor.dfs.server.namenode;

import java.io.IOException;
import java.io.OutputStream;

import org.javenstudio.raptor.io.Writable;

/**
 * A generic abstract class to support journaling of edits logs into 
 * a persistent storage.
 */
abstract class EditLogOutputStream extends OutputStream {
  // these are statistics counters
  private long numSync;        // number of sync(s) to disk
  private long totalTimeSync;  // total time to sync

  EditLogOutputStream() throws IOException {
    numSync = totalTimeSync = 0;
  }

  /**
   * Get this stream name.
   * 
   * @return name of the stream
   */
  abstract String getName();

  /** {@inheritDoc} */
  abstract public void write(int b) throws IOException;

  /**
   * Write edits log record into the stream.
   * The record is represented by operation name and
   * an array of Writable arguments.
   * 
   * @param op operation
   * @param writables array of Writable arguments
   * @throws IOException
   */
  abstract void write(byte op, Writable ... writables) throws IOException;

  /**
   * Create and initialize new edits log storage.
   * 
   * @throws IOException
   */
  abstract void create() throws IOException;

  /** {@inheritDoc} */
  abstract public void close() throws IOException;

  /**
   * All data that has been written to the stream so far will be flushed.
   * New data can be still written to the stream while flushing is performed.
   */
  abstract void setReadyToFlush() throws IOException;

  /**
   * Flush and sync all data that is ready to be flush 
   * {@link #setReadyToFlush()} into underlying persistent store.
   * @throws IOException
   */
  abstract protected void flushAndSync() throws IOException;

  /**
   * Flush data to persistent store.
   * Collect sync metrics.
   */
  public void flush() throws IOException {
    numSync++;
    long start = FSNamesystem.now();
    flushAndSync();
    long end = FSNamesystem.now();
    totalTimeSync += (end - start);
  }

  /**
   * Return the size of the current edits log.
   * Length is used to check when it is large enough to start a checkpoint.
   */
  abstract long length() throws IOException;

  /**
   * Return total time spent in {@link #flushAndSync()}
   */
  long getTotalSyncTime() {
    return totalTimeSync;
  }

  /**
   * Return number of calls to {@link #flushAndSync()}
   */
  long getNumSync() {
    return numSync;
  }
}

