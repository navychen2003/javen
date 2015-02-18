package org.javenstudio.falcon.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ChannelFastInputStream extends FastInputStream {
	
  private FileChannel mChannel;

  public ChannelFastInputStream(FileChannel ch, long chPosition) {
    //super(null, new byte[10],0,0); // a small buffer size for testing purposes
    super(null);
    mChannel = ch;
    mReadFromStream = chPosition;
  }

  @Override
  public int readWrappedStream(byte[] target, int offset, int len) throws IOException {
    ByteBuffer bb = ByteBuffer.wrap(target, offset, len);
    int ret = mChannel.read(bb, mReadFromStream);
    return ret;
  }

  public void seek(long position) throws IOException {
    if (position <= mReadFromStream && position >= getBufferPos()) {
      // seek within buffer
      mPos = (int)(position - getBufferPos());
    } else {
      // not needed - underlying read should handle (unless read never done)
      // long currSize = ch.size(); 
      // if (position > currSize) {
      //   throw new EOFException("Read past EOF: seeking to " + position 
      // 		+ " on file of size " + currSize + " file=" + ch);
      // }
      mReadFromStream = position;
      mEnd = mPos = 0;
    }
    assert position() == position;
  }

  /** where is the start of the buffer relative to the whole file */
  public long getBufferPos() {
    return mReadFromStream - mEnd;
  }

  public int getBufferSize() {
    return mBuffer.length;
  }

  @Override
  public void close() throws IOException {
    mChannel.close();
  }
  
  @Override
  public String toString() {
    return "ChannelFastInputStream{readFromStream=" + mReadFromStream 
    		+ " pos=" + mPos + " end=" + mEnd + " bufferPos=" + getBufferPos() 
    		+ " position=" + position() + "}";
  }
  
}
