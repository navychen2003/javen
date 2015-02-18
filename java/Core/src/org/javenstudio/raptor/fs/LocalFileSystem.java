package org.javenstudio.raptor.fs;

import java.io.*;
import java.net.URI;
import java.util.*;

/****************************************************************
 * Implement the FileSystem API for the checksumed local filesystem.
 *
 *****************************************************************/
public class LocalFileSystem extends ChecksumFileSystem {
  static final URI NAME = URI.create("file:///");
  static private Random rand = new Random();
  FileSystem rfs;
  
  public LocalFileSystem() {
    this(new RawLocalFileSystem());
  }
  
  public FileSystem getRaw() {
    return rfs;
  }
    
  public LocalFileSystem(FileSystem rawLocalFileSystem) {
    super(rawLocalFileSystem);
    rfs = rawLocalFileSystem;
  }
    
  /** Convert a path to a File. */
  public File pathToFile(Path path) {
    return ((RawLocalFileSystem)fs).pathToFile(path);
  }

  @Override
  public void copyFromLocalFile(boolean delSrc, Path src, Path dst)
    throws IOException {
    FileUtil.copy(this, src, this, dst, delSrc, getConf());
  }

  @Override
  public void copyToLocalFile(boolean delSrc, Path src, Path dst)
    throws IOException {
    FileUtil.copy(this, src, this, dst, delSrc, getConf());
  }

  /**
   * Moves files to a bad file directory on the same device, so that their
   * storage will not be reused.
   */
  public boolean reportChecksumFailure(Path p, FSDataInputStream in,
                                       long inPos,
                                       FSDataInputStream sums, long sumsPos) {
    try {
      // canonicalize f
      File f = ((RawLocalFileSystem)fs).pathToFile(p).getCanonicalFile();
      
      // find highest writable parent dir of f on the same device
      String device = new DF(f, getConf()).getMount();
      File parent = f.getParentFile();
      File dir = null;
      while (parent!=null && parent.canWrite() && parent.toString().startsWith(device)) {
        dir = parent;
        parent = parent.getParentFile();
      }

      if (dir==null) {
        throw new IOException(
                              "not able to find the highest writable parent dir");
      }
        
      // move the file there
      File badDir = new File(dir, "bad_files");
      if (!badDir.mkdirs()) {
        if (!badDir.isDirectory()) {
          throw new IOException("Mkdirs failed to create " + badDir.toString());
        }
      }
      String suffix = "." + rand.nextInt();
      File badFile = new File(badDir, f.getName()+suffix);
      LOG.warn("Moving bad file " + f + " to " + badFile);
      in.close();                               // close it first
      f.renameTo(badFile);                      // rename it

      // move checksum file too
      File checkFile = ((RawLocalFileSystem)fs).pathToFile(getChecksumFile(p));
      checkFile.renameTo(new File(badDir, checkFile.getName()+suffix));

    } catch (IOException e) {
      LOG.warn("Error moving bad file " + p + ": " + e);
    }
    return false;
  }
}

