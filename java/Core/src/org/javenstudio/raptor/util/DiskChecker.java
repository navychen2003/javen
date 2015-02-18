package org.javenstudio.raptor.util;

import java.io.File;
import java.io.IOException;

/**
 * Class that provides utility functions for checking disk problem
 */

public class DiskChecker {

  public static class DiskErrorException extends IOException {
	private static final long serialVersionUID = 1L;

	public DiskErrorException(String msg) {
      super(msg);
    }
  }
    
  public static class DiskOutOfSpaceException extends IOException {
	private static final long serialVersionUID = 1L;

	public DiskOutOfSpaceException(String msg) {
      super(msg);
    }
  }
      
  /** 
   * The semantics of mkdirsWithExistsCheck method is different from the mkdirs
   * method provided in the Sun's java.io.File class in the following way:
   * While creating the non-existent parent directories, this method checks for
   * the existence of those directories if the mkdir fails at any point (since
   * that directory might have just been created by some other process).
   * If both mkdir() and the exists() check fails for any seemingly 
   * non-existent directory, then we signal an error; Sun's mkdir would signal
   * an error (return false) if a directory it is attempting to create already
   * exists or the mkdir fails.
   * @param dir
   * @return true on success, false on failure
   */
  public static boolean mkdirsWithExistsCheck(File dir) {
    if (dir.mkdir() || dir.exists()) {
      return true;
    }
    File canonDir = null;
    try {
      canonDir = dir.getCanonicalFile();
    } catch (IOException e) {
      return false;
    }
    String parent = canonDir.getParent();
    return (parent != null) && 
           (mkdirsWithExistsCheck(new File(parent)) &&
                                      (canonDir.mkdir() || canonDir.exists()));
  }
  
  public static void checkDir(File dir) throws DiskErrorException {
    if (!mkdirsWithExistsCheck(dir))
      throw new DiskErrorException("can not create directory: " 
                                   + dir.toString());
        
    if (!dir.isDirectory())
      throw new DiskErrorException("not a directory: " 
                                   + dir.toString());
            
    if (!dir.canRead())
      throw new DiskErrorException("directory is not readable: " 
                                   + dir.toString());
            
    if (!dir.canWrite())
      throw new DiskErrorException("directory is not writable: " 
                                   + dir.toString());
  }

}
