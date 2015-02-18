package org.javenstudio.raptor.io.compress.zlib;

import java.io.IOException;
import java.util.zip.Deflater;

import org.javenstudio.raptor.io.compress.Compressor;

/**
 * A wrapper around java.util.zip.Deflater to make it conform 
 * to org.javenstudio.raptor.io.compress.Compressor interface.
 * 
 */
public class BuiltInZlibDeflater extends Deflater implements Compressor {

  public BuiltInZlibDeflater(int level, boolean nowrap) {
    super(level, nowrap);
  }

  public BuiltInZlibDeflater(int level) {
    super(level);
  }

  public BuiltInZlibDeflater() {
    super();
  }

  public synchronized int compress(byte[] b, int off, int len) 
    throws IOException {
    return super.deflate(b, off, len);
  }
}

