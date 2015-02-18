package org.javenstudio.raptor.bigdb.regionserver.wal;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FSDataOutputStream;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.SequenceFile;
import org.javenstudio.raptor.io.SequenceFile.Metadata;
import org.javenstudio.raptor.io.compress.DefaultCodec;

/**
 * Implementation of {@link DBLog.Writer} that delegates to
 * {@link SequenceFile.Writer}.
 */
public class SequenceFileLogWriter implements DBLog.Writer {
  private final Logger LOG = Logger.getLogger(getClass());
  // The sequence file we delegate to.
  private SequenceFile.Writer writer;
  // The dfsclient out stream gotten made accessible or null if not available.
  private OutputStream dfsClient_out;
  // The syncFs method from hdfs-200 or null if not available.
  private Method syncFs;

  public SequenceFileLogWriter() {
    super();
  }

  @Override
  public void init(FileSystem fs, Path path, Configuration conf)
      throws IOException {
    // Create a SF.Writer instance.
    this.writer = SequenceFile.createWriter(fs, conf, path,
      DBLog.getKeyClass(conf), WALEdit.class,
      fs.getConf().getInt("io.file.buffer.size", 4096),
      (short) conf.getInt("bigdb.regionserver.hlog.replication",
        fs.getDefaultReplication()),
      conf.getLong("bigdb.regionserver.hlog.blocksize",
        fs.getDefaultBlockSize()),
      SequenceFile.CompressionType.NONE,
      new DefaultCodec(),
      null,
      new Metadata());

    // Get at the private FSDataOutputStream inside in SequenceFile so we can
    // call sync on it.  Make it accessible.  Stash it aside for call up in
    // the sync method.
    final Field fields [] = this.writer.getClass().getDeclaredFields();
    final String fieldName = "out";
    for (int i = 0; i < fields.length; ++i) {
      if (fieldName.equals(fields[i].getName())) {
        try {
          // Make the 'out' field up in SF.Writer accessible.
          fields[i].setAccessible(true);
          FSDataOutputStream out =
            (FSDataOutputStream)fields[i].get(this.writer);
          this.dfsClient_out = out.getWrappedStream();
          break;
        } catch (IllegalAccessException ex) {
          throw new IOException("Accessing " + fieldName, ex);
        }
      }
    }

    // Now do dirty work to see if syncFs is available.
    // Test if syncfs is available.
    Method m = null;
    if (conf.getBoolean("dfs.support.append", false)) {
      try {
        // function pointer to writer.syncFs()
        m = this.writer.getClass().getMethod("syncFs", new Class<?> []{});
      } catch (SecurityException e) {
        throw new IOException("Failed test for syncfs", e);
      } catch (NoSuchMethodException e) {
        // Not available
      }
    }
    this.syncFs = m;
    LOG.info((this.syncFs != null)?
      "Using syncFs -- HDFS-200": "syncFs -- HDFS-200 -- not available");
  }

  @Override
  public void append(DBLog.Entry entry) throws IOException {
    this.writer.append(entry.getKey(), entry.getEdit());
  }

  @Override
  public void close() throws IOException {
    this.writer.close();
  }

  @Override
  public void sync() throws IOException {
    if (this.syncFs != null) {
      try {
       this.syncFs.invoke(this.writer, DBLog.NO_ARGS);
      } catch (Exception e) {
        throw new IOException("Reflection", e);
      }
    }
  }

  @Override
  public long getLength() throws IOException {
    return this.writer.getLength();
  }

  /**
   * @return The dfsclient out stream up inside SF.Writer made accessible, or
   * null if not available.
   */
  public OutputStream getDFSCOutputStream() {
    return this.dfsClient_out;
  }
}
