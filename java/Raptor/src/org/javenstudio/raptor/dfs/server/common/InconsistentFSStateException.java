package org.javenstudio.raptor.dfs.server.common;

import java.io.File;
import java.io.IOException;

import org.javenstudio.raptor.util.StringUtils;

/**
 * The exception is thrown when file system state is inconsistent 
 * and is not recoverable. 
 * 
 */
public class InconsistentFSStateException extends IOException {
  private static final long serialVersionUID = 1L;

  public InconsistentFSStateException(File dir, String descr) {
    super("Directory " + getFilePath(dir)
          + " is in an inconsistent state: " + descr);
  }

  public InconsistentFSStateException(File dir, String descr, Throwable ex) {
    this(dir, descr + "\n" + StringUtils.stringifyException(ex));
  }
  
  private static String getFilePath(File dir) {
    try {
      return dir.getCanonicalPath();
    } catch(IOException e) {}
    return dir.getPath();
  }
}

