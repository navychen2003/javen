package org.javenstudio.falcon.util;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** 
 * Single threaded buffered OutputStream
 *  Internal use only, subject to change.
 */
public class FastOutputStream extends OutputStream implements DataOutput {
	
  protected final OutputStream mOutput;
  protected byte[] mBuffer; 
  // how many bytes written to the underlying stream
  protected long mWritten; 
  protected int mPos;

  public FastOutputStream(OutputStream w) {
    // use default BUFSIZE of BufferedOutputStream so if we wrap that
    // it won't cause double buffering.
    this(w, new byte[8192], 0);
  }

  public FastOutputStream(OutputStream sink, byte[] tempBuffer, int start) {
    mOutput = sink;
    mBuffer = tempBuffer;
    mPos = start;
  }

  @SuppressWarnings("resource")
  public static FastOutputStream wrap(OutputStream sink) {
   return (sink instanceof FastOutputStream) ? (FastOutputStream)sink : new FastOutputStream(sink);
  }

  @Override
  public void write(int b) throws IOException {
    write((byte)b);
  }

  @Override
  public void write(byte b[]) throws IOException {
    write(b, 0, b.length);
  }

  public void write(byte b) throws IOException {
    if (mPos >= mBuffer.length) {
      mWritten += mPos;
      flush(mBuffer, 0, mBuffer.length);
      mPos = 0;
    }
    mBuffer[mPos++] = b;
  }

  @Override
  public void write(byte arr[], int off, int len) throws IOException {
    for (;;) {
      int space = mBuffer.length - mPos;

      if (len <= space) {
        System.arraycopy(arr, off, mBuffer, mPos, len);
        mPos += len;
        return;
        
      } else if (len > mBuffer.length) {
        if (mPos > 0) {
          flush(mBuffer, 0, mPos); // flush
          mWritten += mPos;
          mPos = 0;
        }
        
        // don't buffer, just write to sink
        flush(arr, off, len);
        mWritten += len;
        return;
      }

      // buffer is too big to fit in the free space, but
      // not big enough to warrant writing on its own.
      // write whatever we can fit, then flush and iterate.
      System.arraycopy(arr, off, mBuffer, mPos, space);
      
      // important to do this first, since buf.length can change after a flush!
      mWritten += mBuffer.length; 
      flush(mBuffer, 0, mBuffer.length);
      mPos = 0;
      off += space;
      len -= space;
    }
  }

  /** 
   * reserve at least len bytes at the end of the buffer.
   * Invalid if len > buffer.length
   */
  public void reserve(int len) throws IOException {
    if (len > (mBuffer.length - mPos))
      flushBuffer();
  }

  ////////////////// DataOutput methods ///////////////////
  @Override
  public void writeBoolean(boolean v) throws IOException {
    write(v ? 1:0);
  }

  @Override
  public void writeByte(int v) throws IOException {
    write((byte)v);
  }

  @Override
  public void writeShort(int v) throws IOException {
    write((byte)(v >>> 8));
    write((byte)v);
  }

  @Override
  public void writeChar(int v) throws IOException {
    writeShort(v);
  }

  @Override
  public void writeInt(int v) throws IOException {
    reserve(4);
    mBuffer[mPos] = (byte)(v>>>24);
    mBuffer[mPos+1] = (byte)(v>>>16);
    mBuffer[mPos+2] = (byte)(v>>>8);
    mBuffer[mPos+3] = (byte)(v);
    mPos += 4;
  }

  @Override
  public void writeLong(long v) throws IOException {
    reserve(8);
    mBuffer[mPos] = (byte)(v>>>56);
    mBuffer[mPos+1] = (byte)(v>>>48);
    mBuffer[mPos+2] = (byte)(v>>>40);
    mBuffer[mPos+3] = (byte)(v>>>32);
    mBuffer[mPos+4] = (byte)(v>>>24);
    mBuffer[mPos+5] = (byte)(v>>>16);
    mBuffer[mPos+6] = (byte)(v>>>8);
    mBuffer[mPos+7] = (byte)(v);
    mPos += 8;
  }

  @Override
  public void writeFloat(float v) throws IOException {
    writeInt(Float.floatToRawIntBits(v));
  }

  @Override
  public void writeDouble(double v) throws IOException {
    writeLong(Double.doubleToRawLongBits(v));
  }

  @Override
  public void writeBytes(String s) throws IOException {
    // non-optimized version, but this shouldn't be used anyway
    for (int i=0; i<s.length(); i++)
      write((byte)s.charAt(i));
  }

  @Override
  public void writeChars(String s) throws IOException {
    // non-optimized version
    for (int i=0; i<s.length(); i++)
      writeChar(s.charAt(i)); 
  }

  @Override
  public void writeUTF(String s) throws IOException {
    // non-optimized version, but this shouldn't be used anyway
    DataOutputStream daos = new DataOutputStream(this);
    daos.writeUTF(s);
  }

  @Override
  public void flush() throws IOException {
    flushBuffer();
    if (mOutput != null) mOutput.flush();
  }

  @Override
  public void close() throws IOException {
    flushBuffer();
    if (mOutput != null) mOutput.close();
  }

  /** 
   * Only flushes the buffer of the FastOutputStream, not that of the
   * underlying stream.
   */
  public void flushBuffer() throws IOException {
    if (mPos > 0) {
      mWritten += mPos;
      flush(mBuffer, 0, mPos);
      mPos = 0;
    }
  }

  /** All writes to the sink will go through this method */
  public void flush(byte[] buf, int offset, int len) throws IOException {
    mOutput.write(buf, offset, len);
  }

  public long size() {
    return mWritten + mPos;
  }

  /** 
   * Returns the number of bytes actually written to the underlying OutputStream, not including
   * anything currently buffered by this class itself.
   */
  public long written() {
    return mWritten;
  }

  /** Resets the count returned by written() */
  public void setWritten(long written) {
    mWritten = written;
  }

}
