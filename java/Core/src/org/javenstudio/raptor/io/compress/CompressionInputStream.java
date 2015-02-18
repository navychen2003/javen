package org.javenstudio.raptor.io.compress;

import java.io.IOException;
import java.io.InputStream;

/**
 * A compression input stream.
 *
 * <p>Implementations are assumed to be buffered.  This permits clients to
 * reposition the underlying input stream then call {@link #resetState()},
 * without having to also synchronize client buffers.
 */
public abstract class CompressionInputStream extends InputStream {
  /**
   * The input stream to be compressed. 
   */
  protected final InputStream in;

  /**
   * Create a compression input stream that reads
   * the decompressed bytes from the given stream.
   * 
   * @param in The input stream to be compressed.
   */
  protected CompressionInputStream(InputStream in) {
    this.in = in;
  }

  public void close() throws IOException {
    in.close();
  }
  
  /**
   * Read bytes from the stream.
   * Made abstract to prevent leakage to underlying stream.
   */
  public abstract int read(byte[] b, int off, int len) throws IOException;

  /**
   * Reset the decompressor to its initial state and discard any buffered data,
   * as the underlying stream may have been repositioned.
   */
  public abstract void resetState() throws IOException;
  
}

