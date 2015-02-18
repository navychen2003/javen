package org.javenstudio.falcon.util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MemOutputStream extends FastOutputStream {
	
  private List<byte[]> mBuffers = new LinkedList<byte[]>();
  
  public MemOutputStream(byte[] tempBuffer) {
    super(null, tempBuffer, 0);
  }

  @Override
  public void flush(byte[] arr, int offset, int len) throws IOException {
    if (arr == mBuffer && offset == 0 && len == mBuffer.length) {
    	mBuffers.add(mBuffer);  // steal the buffer
      mBuffer = new byte[8192];
      
    } else if (len > 0) {
      byte[] newBuf = new byte[len];
      System.arraycopy(arr, offset, newBuf, 0, len);
      mBuffers.add(newBuf);
    }
  }

  public void writeAll(FastOutputStream fos) throws IOException {
    for (byte[] buffer : mBuffers) {
      fos.write(buffer);
    }
    if (mPos > 0) {
      fos.write(mBuffer, 0, mPos);
    }
  }
  
}
