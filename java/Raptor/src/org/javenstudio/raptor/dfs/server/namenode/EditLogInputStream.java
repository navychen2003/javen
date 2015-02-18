package org.javenstudio.raptor.dfs.server.namenode;

import java.io.IOException;
import java.io.InputStream;

/**
 * A generic abstract class to support reading edits log data from 
 * persistent storage.
 * 
 * It should stream bytes from the storage exactly as they were written
 * into the #{@link EditLogOutputStream}.
 */
abstract class EditLogInputStream extends InputStream {
  /**
   * Get this stream name.
   * 
   * @return name of the stream
   */
  abstract String getName();

  /** {@inheritDoc} */
  public abstract int available() throws IOException;

  /** {@inheritDoc} */
  public abstract int read() throws IOException;

  /** {@inheritDoc} */
  public abstract int read(byte[] b, int off, int len) throws IOException;

  /** {@inheritDoc} */
  public abstract void close() throws IOException;

  /**
   * Return the size of the current edits log.
   */
  abstract long length() throws IOException;
}

