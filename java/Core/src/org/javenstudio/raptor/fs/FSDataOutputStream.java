package org.javenstudio.raptor.fs;

import java.io.*;

/** Utility that wraps a {@link OutputStream} in a {@link DataOutputStream},
 * buffers output through a {@link BufferedOutputStream} and creates a checksum
 * file. */
public class FSDataOutputStream extends DataOutputStream implements Syncable {
  private OutputStream wrappedStream;

  private static class PositionCache extends FilterOutputStream {
    private FileSystem.Statistics statistics;
    long position;

    public PositionCache(OutputStream out, 
                         FileSystem.Statistics stats,
                         long pos) throws IOException {
      super(out);
      statistics = stats;
      position = pos;
    }

    public void write(int b) throws IOException {
      out.write(b);
      position++;
      if (statistics != null) {
        statistics.incrementBytesWritten(1);
      }
    }
    
    public void write(byte b[], int off, int len) throws IOException {
      out.write(b, off, len);
      position += len;                            // update position
      if (statistics != null) {
        statistics.incrementBytesWritten(len);
      }
    }
      
    public long getPos() throws IOException {
      return position;                            // return cached position
    }
    
    public void close() throws IOException {
      out.close();
    }
  }

  @Deprecated
  public FSDataOutputStream(OutputStream out) throws IOException {
    this(out, null);
  }

  public FSDataOutputStream(OutputStream out, FileSystem.Statistics stats)
    throws IOException {
    this(out, stats, 0);
  }

  public FSDataOutputStream(OutputStream out, FileSystem.Statistics stats,
                            long startPosition) throws IOException {
    super(new PositionCache(out, stats, startPosition));
    wrappedStream = out;
  }
  
  public long getPos() throws IOException {
    return ((PositionCache)out).getPos();
  }

  public void close() throws IOException {
    out.close();         // This invokes PositionCache.close()
  }

  // Returns the underlying output stream. This is used by unit tests.
  public OutputStream getWrappedStream() {
    return wrappedStream;
  }

  /** {@inheritDoc} */
  public void sync() throws IOException {
    if (wrappedStream instanceof Syncable) {
      ((Syncable)wrappedStream).sync();
    }
  }

  public void write(String s) throws IOException {
    if (s != null) write(s.getBytes()); 
  }

}

