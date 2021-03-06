package org.javenstudio.falcon.datum.table.store;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.conf.Configurable;
import org.javenstudio.raptor.io.compress.CodecPool;
import org.javenstudio.raptor.io.compress.CompressionCodec;
import org.javenstudio.raptor.io.compress.CompressionInputStream;
import org.javenstudio.raptor.io.compress.CompressionOutputStream;
import org.javenstudio.raptor.io.compress.Compressor;
import org.javenstudio.raptor.io.compress.Decompressor;
import org.javenstudio.raptor.io.compress.GzipCodec;
import org.javenstudio.raptor.io.compress.DefaultCodec;
import org.javenstudio.raptor.util.ReflectionUtils;

/**
 * Compression related stuff.
 * Copied from hadoop-3315 tfile.
 */
public final class Compression {
  private static final Logger LOG = Logger.getLogger(Compression.class);

  /**
   * Prevent the instantiation of class.
   */
  private Compression() {
    super();
  }

  static class FinishOnFlushCompressionStream extends FilterOutputStream {
    public FinishOnFlushCompressionStream(CompressionOutputStream cout) {
      super(cout);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
      out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
      CompressionOutputStream cout = (CompressionOutputStream) out;
      cout.finish();
      cout.flush();
      cout.resetState();
    }
  }

  /**
   * Compression algorithms.
   */
  public static enum Algorithm {
    LZO("lzo") {
      // Use base type to avoid compile-time dependencies.
      private transient CompressionCodec lzoCodec;

      @Override
      CompressionCodec getCodec() {
        if (lzoCodec == null) {
          Configuration conf = ConfigurationFactory.get();
          conf.setBoolean("raptor.native.lib", true);
          try {
            Class<?> externalCodec =
                ClassLoader.getSystemClassLoader().loadClass("org.javenstudio.compression.lzo.LzoCodec");
            lzoCodec = (CompressionCodec) ReflectionUtils.newInstance(externalCodec, conf);
          } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
        }
        return lzoCodec;
      }
    },
    GZ("gz") {
      private transient GzipCodec codec;

      @Override
      DefaultCodec getCodec() {
        if (codec == null) {
          Configuration conf = ConfigurationFactory.get();
          conf.setBoolean("raptor.native.lib", true);
          codec = new GzipCodec();
          codec.setConf(conf);
        }

        return codec;
      }
    },

    NONE("none") {
      @Override
      DefaultCodec getCodec() {
        return null;
      }

      @Override
      public synchronized InputStream createDecompressionStream(
          InputStream downStream, Decompressor decompressor,
          int downStreamBufferSize) throws IOException {
        if (downStreamBufferSize > 0) {
          return new BufferedInputStream(downStream, downStreamBufferSize);
        }
        // else {
          // Make sure we bypass FSInputChecker buffer.
        // return new BufferedInputStream(downStream, 1024);
        // }
        // }
        return downStream;
      }

      @Override
      public synchronized OutputStream createCompressionStream(
          OutputStream downStream, Compressor compressor,
          int downStreamBufferSize) throws IOException {
        if (downStreamBufferSize > 0) {
          return new BufferedOutputStream(downStream, downStreamBufferSize);
        }

        return downStream;
      }
    };

	// data input buffer size to absorb small reads from application.
    private static final int DATA_IBUF_SIZE = 1 * 1024;
	// data output buffer size to absorb small writes from application.
    private static final int DATA_OBUF_SIZE = 4 * 1024;

    private final String mCompressName;
    
    Algorithm(String name) {
      this.mCompressName = name;
    }

    abstract CompressionCodec getCodec();

    public InputStream createDecompressionStream(
        InputStream downStream, Decompressor decompressor,
        int downStreamBufferSize) throws IOException {
      CompressionCodec codec = getCodec();
      // Set the internal buffer size to read from down stream.
      if (downStreamBufferSize > 0) {
        Configurable c = (Configurable) codec;
        c.getConf().setInt("io.file.buffer.size", downStreamBufferSize);
      }
      CompressionInputStream cis =
          codec.createInputStream(downStream, decompressor);
      BufferedInputStream bis2 = new BufferedInputStream(cis, DATA_IBUF_SIZE);
      return bis2;

    }

    public OutputStream createCompressionStream(
        OutputStream downStream, Compressor compressor, int downStreamBufferSize)
        throws IOException {
      CompressionCodec codec = getCodec();
      OutputStream bos1 = null;
      if (downStreamBufferSize > 0) {
        bos1 = new BufferedOutputStream(downStream, downStreamBufferSize);
      }
      else {
        bos1 = downStream;
      }
      Configurable c = (Configurable) codec;
      c.getConf().setInt("io.file.buffer.size", 32 * 1024);
      CompressionOutputStream cos =
          codec.createOutputStream(bos1, compressor);
      BufferedOutputStream bos2 =
          new BufferedOutputStream(new FinishOnFlushCompressionStream(cos),
              DATA_OBUF_SIZE);
      return bos2;
    }

    public Compressor getCompressor() {
      CompressionCodec codec = getCodec();
      if (codec != null) {
        Compressor compressor = CodecPool.getCompressor(codec);
        if (compressor != null) {
          if (compressor.finished()) {
            // Somebody returns the compressor to CodecPool but is still using
            // it.
        	if (LOG.isWarnEnabled())
              LOG.warn("Compressor obtained from CodecPool is already finished()");
            // throw new AssertionError(
            // "Compressor obtained from CodecPool is already finished()");
          }
          compressor.reset();
        }
        return compressor;
      }
      return null;
    }

    public void returnCompressor(Compressor compressor) {
      if (compressor != null) {
        CodecPool.returnCompressor(compressor);
      }
    }

    public Decompressor getDecompressor() {
      CompressionCodec codec = getCodec();
      if (codec != null) {
        Decompressor decompressor = CodecPool.getDecompressor(codec);
        if (decompressor != null) {
          if (decompressor.finished()) {
            // Somebody returns the decompressor to CodecPool but is still using
            // it.
        	if (LOG.isWarnEnabled())
              LOG.warn("Deompressor obtained from CodecPool is already finished()");
            // throw new AssertionError(
            // "Decompressor obtained from CodecPool is already finished()");
          }
          decompressor.reset();
        }
        return decompressor;
      }

      return null;
    }

    public void returnDecompressor(Decompressor decompressor) {
      if (decompressor != null) {
        CodecPool.returnDecompressor(decompressor);
      }
    }

    public String getName() {
      return mCompressName;
    }
  }

  public static Algorithm getCompressionAlgorithmByName(String compressName) {
    Algorithm[] algos = Algorithm.class.getEnumConstants();

    for (Algorithm a : algos) {
      if (a.getName().equals(compressName)) {
        return a;
      }
    }

    throw new IllegalArgumentException(
        "Unsupported compression algorithm name: " + compressName);
  }

  static String[] getSupportedAlgorithms() {
    Algorithm[] algos = Algorithm.class.getEnumConstants();

    String[] ret = new String[algos.length];
    int i = 0;
    for (Algorithm a : algos) {
      ret[i++] = a.getName();
    }

    return ret;
  }
}
