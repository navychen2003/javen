package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.dfs.DistributedFileSystem;
import org.javenstudio.raptor.dfs.protocol.AlreadyBeingCreatedException;
import org.javenstudio.raptor.dfs.protocol.FSConstants;
import org.javenstudio.raptor.fs.FSDataOutputStream;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.SequenceFile;

public class FSUtils {
  private static final Logger LOG = Logger.getLogger(FSUtils.class);

  public static FileSystem getFs(final Configuration conf) throws IOException { 
	return FileSystem.getLocal(conf);
  }
  
  /**
   * Heuristic to determine whether is safe or not to open a file for append
   * Looks both for dfs.support.append and use reflection to search
   * for SequenceFile.Writer.syncFs() or FSDataOutputStream.hflush()
   * @param conf
   * @return True if append support
   */
  public static boolean isAppendSupported(final Configuration conf) {
    boolean append = conf.getBoolean("dfs.support.append", false);
    if (append) {
      try {
        // TODO: The implementation that comes back when we do a createWriter
        // may not be using SequenceFile so the below is not a definitive test.
        // Will do for now (dfs-200).
        SequenceFile.Writer.class.getMethod("syncFs", new Class<?> []{});
        append = true;
      } catch (SecurityException e) {
      } catch (NoSuchMethodException e) {
        append = false;
      }
    } else {
      try {
        FSDataOutputStream.class.getMethod("hflush", new Class<?> []{});
      } catch (NoSuchMethodException e) {
        append = false;
      }
    }
    return append;
  }

  /**
   * Recover file lease. Used when a file might be suspect to be 
   * had been left open by another process. <code>p</code>
   * @param fs
   * @param p
   * @param append True if append supported
   * @throws IOException
   */
  public static void recoverFileLease(final FileSystem fs, final Path p, Configuration conf)
      throws IOException{
    if (!isAppendSupported(conf)) {
      if (LOG.isWarnEnabled())
        LOG.warn("Running on HDFS without append enabled may result in data loss");
      return;
    }
    // lease recovery not needed for local file system case.
    // currently, local file system doesn't implement append either.
    if (!(fs instanceof DistributedFileSystem)) 
      return;
    
    if (LOG.isInfoEnabled())
      LOG.info("Recovering file" + p);
    
    long startWaiting = System.currentTimeMillis();

    // Trying recovery
    boolean recovered = false;
    while (!recovered) {
      try {
        FSDataOutputStream out = fs.append(p);
        out.close();
        recovered = true;
      } catch (IOException e) {
        //e = RemoteExceptionHandler.checkIOException(e);
        if (e instanceof AlreadyBeingCreatedException) {
          // We expect that we'll get this message while the lease is still
          // within its soft limit, but if we get it past that, it means
          // that the RS is holding onto the file even though it lost its
          // znode. We could potentially abort after some time here.
          long waitedFor = System.currentTimeMillis() - startWaiting;
          if (waitedFor > FSConstants.LEASE_SOFTLIMIT_PERIOD) {
        	if (LOG.isWarnEnabled()) {
              LOG.warn("Waited " + waitedFor + "ms for lease recovery on " + p +
                ":" + e.getMessage());
        	}
          }
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ex) {
            // ignore it and try again
          }
        } else {
          throw new IOException("Failed to open " + p + " for append", e);
        }
      }
    }
    
    if (LOG.isInfoEnabled())
      LOG.info("Finished lease recover attempt for " + p);
  }

}
