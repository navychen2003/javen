package org.javenstudio.falcon.util;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/** 
 * Single threaded buffered InputStream
 *  Internal use only, subject to change.
 */
public class FastInputStream extends InputStream implements DataInput {
	
  protected final InputStream mInput;
  protected final byte[] mBuffer;
  // number of bytes read from the underlying inputstream
  protected long mReadFromStream; 
  protected int mPos;
  protected int mEnd;
  
  public FastInputStream(InputStream in) {
    // use default BUFSIZE of BufferedOutputStream so if we wrap that
    // it won't cause double buffering.
    this(in, new byte[8192], 0, 0);
  }

  public FastInputStream(InputStream in, byte[] tempBuffer, int start, int end) {
    mInput = in;
    mBuffer = tempBuffer;
    mPos = start;
    mEnd = end;
  }

  @SuppressWarnings("resource")
  public static FastInputStream wrap(InputStream in) {
    return (in instanceof FastInputStream) ? (FastInputStream)in : new FastInputStream(in);
  }

  @Override
  public int read() throws IOException {
    if (mPos >= mEnd) {
      refill();
      if (mPos >= mEnd) return -1;
    }
    return mBuffer[mPos++] & 0xff;     
  }

  public int peek() throws IOException {
    if (mPos >= mEnd) {
      refill();
      if (mPos >= mEnd) return -1;
    }
    return mBuffer[mPos] & 0xff;
  }

  @Override
  public int readUnsignedByte() throws IOException {
    if (mPos >= mEnd) {
      refill();
      if (mPos >= mEnd) {
        throw new EOFException();
      }
    }
    return mBuffer[mPos++] & 0xff;
  }

  public int readWrappedStream(byte[] target, int offset, int len) throws IOException {
    return mInput.read(target, offset, len);
  }

  public long position() {
    return mReadFromStream - (mEnd - mPos);
  }

  public void refill() throws IOException {
    // this will set end to -1 at EOF
    mEnd = readWrappedStream(mBuffer, 0, mBuffer.length);
    if (mEnd > 0) mReadFromStream += mEnd;
    mPos = 0;
  }

  @Override
  public int available() throws IOException {
    return mEnd - mPos;
  }

  @Override
  public int read(byte b[], int off, int len) throws IOException {
    int r=0; // number of bytes we have read

    // first read from our buffer;
    if (mEnd-mPos > 0) {
      r = Math.min(mEnd-mPos, len);
      System.arraycopy(mBuffer, mPos, b, off, r);
      mPos += r;
    }

    if (r == len) return r;

    // amount left to read is >= buffer size
    if (len-r >= mBuffer.length) {
      int ret = readWrappedStream(b, off+r, len-r);
      if (ret >= 0) {
        mReadFromStream += ret;
        r += ret;
        return r;
      } else {
        // negative return code
        return r > 0 ? r : -1;
      }
    }

    refill();

    // read rest from our buffer
    if (mEnd-mPos > 0) {
      int toRead = Math.min(mEnd-mPos, len-r);
      System.arraycopy(mBuffer, mPos, b, off+r, toRead);
      mPos += toRead;
      r += toRead;
      return r;
    }

    return r > 0 ? r : -1;
  }

  @Override
  public void close() throws IOException {
    mInput.close();
  }

  @Override
  public void readFully(byte b[]) throws IOException {
    readFully(b, 0, b.length);
  }

  @Override
  public void readFully(byte b[], int off, int len) throws IOException {
    while (len > 0) {
      int ret = read(b, off, len);
      if (ret==-1) {
        throw new EOFException();
      }
      off += ret;
      len -= ret;
    }
  }

  @Override
  public int skipBytes(int n) throws IOException {
    if (mEnd-mPos >= n) {
      mPos += n;
      return n;
    }

    if (mEnd-mPos < 0) return -1;
    
    int r = mEnd-mPos;
    mPos = mEnd;

    while (r < n) {
      refill();
      if (mEnd-mPos <= 0) return r;
      int toRead = Math.min(mEnd-mPos, n-r);
      r += toRead;
      mPos += toRead;
    }

    return r;
  }

  @Override
  public boolean readBoolean() throws IOException {
    return readByte()==1;
  }

  @Override
  public byte readByte() throws IOException {
    if (mPos >= mEnd) {
      refill();
      if (mPos >= mEnd) throw new EOFException();
    }
    return mBuffer[mPos++];
  }

  @Override
  public short readShort() throws IOException {
    return (short)((readUnsignedByte() << 8) | readUnsignedByte());
  }

  @Override
  public int readUnsignedShort() throws IOException {
    return (readUnsignedByte() << 8) | readUnsignedByte();
  }

  @Override
  public char readChar() throws IOException {
    return (char)((readUnsignedByte() << 8) | readUnsignedByte());
  }

  @Override
  public int readInt() throws IOException {
    return  ((readUnsignedByte() << 24)
            |(readUnsignedByte() << 16)
            |(readUnsignedByte() << 8)
            | readUnsignedByte());
  }

  @Override
  public long readLong() throws IOException {
    return  (((long)readUnsignedByte()) << 56)
            | (((long)readUnsignedByte()) << 48)
            | (((long)readUnsignedByte()) << 40)
            | (((long)readUnsignedByte()) << 32)
            | (((long)readUnsignedByte()) << 24)
            | (readUnsignedByte() << 16)
            | (readUnsignedByte() << 8)
            | (readUnsignedByte());
  }

  @Override
  public float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());    
  }

  @Override
  public double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());    
  }

  @Override
  public String readLine() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String readUTF() throws IOException {
    return new DataInputStream(this).readUTF();
  }
  
}
