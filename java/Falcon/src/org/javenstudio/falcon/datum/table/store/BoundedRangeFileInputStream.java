package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.raptor.fs.FSDataInputStream;

/**
 * BoundedRangeFIleInputStream abstracts a contiguous region of a Hadoop
 * FSDataInputStream as a regular input stream. One can create multiple
 * BoundedRangeFileInputStream on top of the same FSDataInputStream and they
 * would not interfere with each other.
 * Copied from hadoop-335 tfile.
 */
public class BoundedRangeFileInputStream  extends InputStream {

  private final byte[] mOneByte = new byte[1];
  private final boolean mPread;
  private FSDataInputStream mIn;
  private long mPos;
  private long mEnd;
  private long mMark;
  
  /**
   * Constructor
   *
   * @param in
   *          The FSDataInputStream we connect to.
   * @param offset
   *          Beginning offset of the region.
   * @param length
   *          Length of the region.
   * @param pread If true, use Filesystem positional read rather than seek+read.
   *
   *          The actual length of the region may be smaller if (off_begin +
   *          length) goes beyond the end of FS input stream.
   */
  public BoundedRangeFileInputStream(FSDataInputStream in, long offset,
      long length, final boolean pread) {
    if (offset < 0 || length < 0) {
      throw new IndexOutOfBoundsException("Invalid offset/length: " + offset
          + "/" + length);
    }

    this.mIn = in;
    this.mPos = offset;
    this.mEnd = offset + length;
    this.mMark = -1;
    this.mPread = pread;
  }

  @Override
  public int available() throws IOException {
    int avail = mIn.available();
    if (mPos + avail > mEnd) {
      avail = (int) (mEnd - mPos);
    }

    return avail;
  }

  @Override
  public int read() throws IOException {
    int ret = read(mOneByte);
    if (ret == 1) return mOneByte[0] & 0xff;
    return -1;
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if ((off | len | (off + len) | (b.length - (off + len))) < 0) 
      throw new IndexOutOfBoundsException();

    int n = (int) Math.min(Integer.MAX_VALUE, Math.min(len, (mEnd - mPos)));
    if (n == 0) return -1;
    
    int ret = 0;
    if (this.mPread) {
      ret = mIn.read(mPos, b, off, n);
    } else {
      synchronized (mIn) {
        mIn.seek(mPos);
        ret = mIn.read(b, off, n);
      }
    }
    
    if (ret < 0) {
      mEnd = mPos;
      return -1;
    }
    
    mPos += ret;
    return ret;
  }
  
  /**
   * We may skip beyond the end of the file.
   */
  @Override
  public long skip(long n) throws IOException {
    long len = Math.min(n, mEnd - mPos);
    mPos += len;
    return len;
  }

  @Override
  public void mark(int readlimit) {
    mMark = mPos;
  }

  @Override
  public void reset() throws IOException {
    if (mMark < 0) throw new IOException("Resetting to invalid mark");
    mPos = mMark;
  }

  @Override
  public boolean markSupported() {
    return true;
  }

  @Override
  public void close() {
    // Invalidate the state of the stream.
    mIn = null;
    mPos = mEnd;
    mMark = -1;
  }
}
