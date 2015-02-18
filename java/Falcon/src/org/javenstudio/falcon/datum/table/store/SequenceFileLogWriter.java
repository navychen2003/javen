package org.javenstudio.falcon.datum.table.store;

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
  private static final Logger LOG = Logger.getLogger(SequenceFileLogWriter.class);
  
  private FileSystem mFs = null;
  private Configuration mConf = null;
  private Path mPath = null;
  
  // The sequence file we delegate to.
  private SequenceFile.Writer mWriter = null;
  // The dfsclient out stream gotten made accessible or null if not available.
  private OutputStream mDfsClientOut = null;
  // The syncFs method from hdfs-200 or null if not available.
  private Method mSyncFs = null;

  public SequenceFileLogWriter() {
    super();
  }

  @Override
  public synchronized void init(FileSystem fs, Path path, Configuration conf)
      throws IOException {
	if (fs == null || conf == null || path == null) 
	  throw new IOException("Cannot init with null fs/conf/path");
	
	this.mFs = fs;
	this.mConf = conf;
	this.mPath = path;
  }
  
  private synchronized SequenceFile.Writer getWriter(boolean init) 
		  throws IOException {
	if (this.mWriter != null) return this.mWriter;
	if (init == false) return this.mWriter;
	
	FileSystem fs = this.mFs;
	Configuration conf = this.mConf;
	Path path = this.mPath;
	
	if (fs == null || conf == null || path == null) 
		throw new IOException("Cannot init LogWriter with null fs/conf/path");
	
    // Create a SF.Writer instance.
    this.mWriter = SequenceFile.createWriter(fs, conf, path,
      DBLog.getKeyClass(conf), WALEdit.class,
      fs.getConf().getInt("io.file.buffer.size", 4096),
      (short) conf.getInt("bigdb.regionserver.dblog.replication",
        fs.getDefaultReplication()),
      conf.getLong("bigdb.regionserver.dblog.blocksize",
        fs.getDefaultBlockSize()),
      SequenceFile.CompressionType.NONE,
      new DefaultCodec(),
      null,
      new Metadata());

    // Get at the private FSDataOutputStream inside in SequenceFile so we can
    // call sync on it.  Make it accessible.  Stash it aside for call up in
    // the sync method.
    final Field[] fields = this.mWriter.getClass().getDeclaredFields();
    final String fieldName = "out";
    
    for (int i = 0; i < fields.length; ++i) {
      if (fieldName.equals(fields[i].getName())) {
        try {
          // Make the 'out' field up in SF.Writer accessible.
          fields[i].setAccessible(true);
          FSDataOutputStream out =
            (FSDataOutputStream)fields[i].get(this.mWriter);
          this.mDfsClientOut = out.getWrappedStream();
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
        m = this.mWriter.getClass().getMethod("syncFs", new Class<?> []{});
      } catch (SecurityException e) {
        throw new IOException("Failed test for syncfs", e);
      } catch (NoSuchMethodException e) {
        // Not available
      }
    }
    this.mSyncFs = m;
    if (LOG.isInfoEnabled()) {
      LOG.info((this.mSyncFs != null) ?
        "Using syncFs -- HDFS-200": "syncFs -- HDFS-200 -- not available");
    }
    
    return this.mWriter;
  }

  @Override
  public synchronized void append(DBLog.Entry entry) throws IOException {
	if (entry == null) return;
	SequenceFile.Writer writer = getWriter(true);
    if (writer != null) writer.append(entry.getKey(), entry.getEdit());
  }

  @Override
  public synchronized void close() throws IOException {
	SequenceFile.Writer writer = getWriter(false);
    if (writer != null) writer.close();
    this.mWriter = null;
  }

  @Override
  public synchronized void sync() throws IOException {
    if (this.mSyncFs != null) {
      try {
    	SequenceFile.Writer writer = getWriter(false);
    	if (writer != null)
          this.mSyncFs.invoke(writer, DBLog.NO_ARGS);
      } catch (Exception e) {
        throw new IOException("Reflection", e);
      }
    }
  }

  @Override
  public synchronized long getLength() throws IOException {
	SequenceFile.Writer writer = getWriter(false);
    return writer != null ? writer.getLength() : 0;
  }

  /**
   * @return The dfsclient out stream up inside SF.Writer made accessible, or
   * null if not available.
   */
  public synchronized OutputStream getDFSCOutputStream() {
    return this.mDfsClientOut;
  }
}
