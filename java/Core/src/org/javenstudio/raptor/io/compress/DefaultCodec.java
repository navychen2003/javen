package org.javenstudio.raptor.io.compress;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import org.javenstudio.raptor.conf.Configurable;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.io.compress.zlib.*;

public class DefaultCodec implements Configurable, CompressionCodec {
  
  Configuration conf;

  public void setConf(Configuration conf) {
    this.conf = conf;
  }
  
  public Configuration getConf() {
    return conf;
  }
  
  public CompressionOutputStream createOutputStream(OutputStream out) 
  throws IOException {
    return new CompressorStream(out, createCompressor(), 
                                conf.getInt("io.file.buffer.size", 4*1024));
  }

  public CompressionOutputStream createOutputStream(OutputStream out, 
                                                    Compressor compressor) 
  throws IOException {
    return new CompressorStream(out, compressor, 
                                conf.getInt("io.file.buffer.size", 4*1024));
  }

  public Class<? extends Compressor> getCompressorType() {
    return ZlibFactory.getZlibCompressorType(conf);
  }

  public Compressor createCompressor() {
    return ZlibFactory.getZlibCompressor(conf);
  }

  public CompressionInputStream createInputStream(InputStream in) 
  throws IOException {
    return new DecompressorStream(in, createDecompressor(),
                                  conf.getInt("io.file.buffer.size", 4*1024));
  }

  public CompressionInputStream createInputStream(InputStream in, 
                                                  Decompressor decompressor) 
  throws IOException {
    return new DecompressorStream(in, decompressor, 
                                  conf.getInt("io.file.buffer.size", 4*1024));
  }

  public Class<? extends Decompressor> getDecompressorType() {
    return ZlibFactory.getZlibDecompressorType(conf);
  }

  public Decompressor createDecompressor() {
    return ZlibFactory.getZlibDecompressor(conf);
  }
  
  public String getDefaultExtension() {
    return ".deflate";
  }

}

