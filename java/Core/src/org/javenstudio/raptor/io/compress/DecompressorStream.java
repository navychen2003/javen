package org.javenstudio.raptor.io.compress;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.raptor.io.compress.Decompressor;

public class DecompressorStream extends CompressionInputStream {
  protected Decompressor decompressor = null;
  protected byte[] buffer;
  protected boolean eof = false;
  protected boolean closed = false;
  
  public DecompressorStream(InputStream in, Decompressor decompressor, int bufferSize) {
    super(in);

    if (in == null || decompressor == null) {
      throw new NullPointerException();
    } else if (bufferSize <= 0) {
      throw new IllegalArgumentException("Illegal bufferSize");
    }

    this.decompressor = decompressor;
    buffer = new byte[bufferSize];
  }

  public DecompressorStream(InputStream in, Decompressor decompressor) {
    this(in, decompressor, 512);
  }

  /**
   * Allow derived classes to directly set the underlying stream.
   * 
   * @param in Underlying input stream.
   */
  protected DecompressorStream(InputStream in) {
    super(in);
  }
  
  private byte[] oneByte = new byte[1];
  public int read() throws IOException {
    checkStream();
    return (read(oneByte, 0, oneByte.length) == -1) ? -1 : (oneByte[0] & 0xff);
  }

  public int read(byte[] b, int off, int len) throws IOException {
    checkStream();
    
    if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return 0;
    }

    return decompress(b, off, len);
  }

  protected int decompress(byte[] b, int off, int len) throws IOException {
    int n = 0;
    
    while ((n = decompressor.decompress(b, off, len)) == 0) {
      if (decompressor.finished() || decompressor.needsDictionary()) {
        eof = true;
        return -1;
      }
      if (decompressor.needsInput()) {
        getCompressedData();
      }
    }
    
    return n;
  }
  
  protected void getCompressedData() throws IOException {
    checkStream();
  
    int n = in.read(buffer, 0, buffer.length);
    if (n == -1) {
      throw new EOFException("Unexpected end of input stream");
    }

    decompressor.setInput(buffer, 0, n);
  }
  
  protected void checkStream() throws IOException {
    if (closed) {
      throw new IOException("Stream closed");
    }
  }
  
  public void resetState() throws IOException {
    decompressor.reset();
  }

  private byte[] skipBytes = new byte[512];
  public long skip(long n) throws IOException {
    // Sanity checks
    if (n < 0) {
      throw new IllegalArgumentException("negative skip length");
    }
    checkStream();
    
    // Read 'n' bytes
    int skipped = 0;
    while (skipped < n) {
      int len = Math.min(((int)n - skipped), skipBytes.length);
      len = read(skipBytes, 0, len);
      if (len == -1) {
        eof = true;
        break;
      }
      skipped += len;
    }
    return skipped;
  }

  public int available() throws IOException {
    checkStream();
    return (eof) ? 0 : 1;
  }

  public void close() throws IOException {
    if (!closed) {
      in.close();
      closed = true;
    }
  }

  public boolean markSupported() {
    return false;
  }

  public synchronized void mark(int readlimit) {
  }

  public synchronized void reset() throws IOException {
    throw new IOException("mark/reset not supported");
  }

}

