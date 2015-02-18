package org.javenstudio.raptor.fs;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;

import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.permission.*;
import org.javenstudio.raptor.util.Progressable;
import org.javenstudio.raptor.util.StringUtils;
import org.javenstudio.raptor.util.Shell;

/****************************************************************
 * Implement the FileSystem API for the raw local filesystem.
 *
 *****************************************************************/
public class RawLocalFileSystem extends FileSystem {
  static final URI NAME = URI.create("file:///");
  private Path workingDir;
  
  public RawLocalFileSystem() {
    workingDir = new Path(System.getProperty("user.dir")).makeQualified(this);
  }
  
  /** Convert a path to a File. */
  public File pathToFile(Path path) {
    checkPath(path);
    if (!path.isAbsolute()) {
      path = new Path(getWorkingDirectory(), path);
    }
    return new File(path.toUri().getPath());
  }

  public URI getUri() { return NAME; }
  
  public void initialize(URI uri, Configuration conf) throws IOException {
    super.initialize(uri, conf);
    setConf(conf);
  }
  
  class TrackingFileInputStream extends FileInputStream {
    public TrackingFileInputStream(File f) throws IOException {
      super(f);
    }
    
    public int read() throws IOException {
      int result = super.read();
      if (result != -1) {
        statistics.incrementBytesRead(1);
      }
      return result;
    }
    
    public int read(byte[] data) throws IOException {
      int result = super.read(data);
      if (result != -1) {
        statistics.incrementBytesRead(result);
      }
      return result;
    }
    
    public int read(byte[] data, int offset, int length) throws IOException {
      int result = super.read(data, offset, length);
      if (result != -1) {
        statistics.incrementBytesRead(result);
      }
      return result;
    }
  }

  /*******************************************************
   * For open()'s FSInputStream
   *******************************************************/
  class LocalFSFileInputStream extends FSInputStream {
    FileInputStream fis;
    private long position;

    public LocalFSFileInputStream(Path f) throws IOException {
      this.fis = new TrackingFileInputStream(pathToFile(f));
    }
    
    public void seek(long pos) throws IOException {
      fis.getChannel().position(pos);
      this.position = pos;
    }
    
    public long getPos() throws IOException {
      return this.position;
    }
    
    public boolean seekToNewSource(long targetPos) throws IOException {
      return false;
    }
    
    /*
     * Just forward to the fis
     */
    public int available() throws IOException { return fis.available(); }
    public void close() throws IOException { fis.close(); }
    public boolean markSupport() { return false; }
    
    public int read() throws IOException {
      try {
        int value = fis.read();
        if (value >= 0) {
          this.position++;
        }
        return value;
      } catch (IOException e) {                 // unexpected exception
        throw new FSError(e);                   // assume native fs error
      }
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
      try {
        int value = fis.read(b, off, len);
        if (value > 0) {
          this.position += value;
        }
        return value;
      } catch (IOException e) {                 // unexpected exception
        throw new FSError(e);                   // assume native fs error
      }
    }
    
    public int read(long position, byte[] b, int off, int len)
      throws IOException {
      ByteBuffer bb = ByteBuffer.wrap(b, off, len);
      try {
        return fis.getChannel().read(bb, position);
      } catch (IOException e) {
        throw new FSError(e);
      }
    }
    
    public long skip(long n) throws IOException {
      long value = fis.skip(n);
      if (value > 0) {
        this.position += value;
      }
      return value;
    }
  }
  
  public FSDataInputStream open(Path f, int bufferSize) throws IOException {
    if (!exists(f)) {
      throw new FileNotFoundException(f.toString());
    }
    return new FSDataInputStream(new BufferedFSInputStream(
        new LocalFSFileInputStream(f), bufferSize));
  }
  
  /*********************************************************
   * For create()'s FSOutputStream.
   *********************************************************/
  class LocalFSFileOutputStream extends OutputStream implements Syncable {
    FileOutputStream fos;
    
    private LocalFSFileOutputStream(Path f, boolean append) throws IOException {
      this.fos = new FileOutputStream(pathToFile(f), append);
    }
    
    /*
     * Just forward to the fos
     */
    public void close() throws IOException { fos.close(); }
    public void flush() throws IOException { fos.flush(); }
    public void write(byte[] b, int off, int len) throws IOException {
      try {
        fos.write(b, off, len);
      } catch (IOException e) {                // unexpected exception
        throw new FSError(e);                  // assume native fs error
      }
    }
    
    public void write(int b) throws IOException {
      try {
        fos.write(b);
      } catch (IOException e) {              // unexpected exception
        throw new FSError(e);                // assume native fs error
      }
    }

    /** {@inheritDoc} */
    public void sync() throws IOException {
      fos.getFD().sync();      
    }
  }
  
  /** {@inheritDoc} */
  public FSDataOutputStream append(Path f, int bufferSize,
      Progressable progress) throws IOException {
    if (!exists(f)) {
      throw new FileNotFoundException("File " + f + " not found.");
    }
    if (getFileStatus(f).isDir()) {
      throw new IOException("Cannot append to a diretory (=" + f + " ).");
    }
    return new FSDataOutputStream(new BufferedOutputStream(
        new LocalFSFileOutputStream(f, true), bufferSize), statistics);
  }

  /** {@inheritDoc} */
  public FSDataOutputStream create(Path f, boolean overwrite, int bufferSize,
                                   short replication, long blockSize, Progressable progress)
    throws IOException {
    if (exists(f) && !overwrite) {
      throw new IOException("File already exists:"+f);
    }
    Path parent = f.getParent();
    if (parent != null && !mkdirs(parent)) {
      throw new IOException("Mkdirs failed to create " + parent.toString());
    }
    return new FSDataOutputStream(new BufferedOutputStream(
        new LocalFSFileOutputStream(f, false), bufferSize), statistics);
  }

  /** {@inheritDoc} */
  @Override
  public FSDataOutputStream create(Path f, FsPermission permission,
      boolean overwrite, int bufferSize, short replication, long blockSize,
      Progressable progress) throws IOException {
    FSDataOutputStream out = create(f,
        overwrite, bufferSize, replication, blockSize, progress);
    setPermission(f, permission);
    return out;
  }
  
  public boolean rename(Path src, Path dst) throws IOException {
    if (pathToFile(src).renameTo(pathToFile(dst))) {
      return true;
    }
    return FileUtil.copy(this, src, this, dst, true, getConf());
  }
  
  @Deprecated
  public boolean delete(Path p) throws IOException {
    return delete(p, true);
  }
  
  public boolean delete(Path p, boolean recursive) throws IOException {
    File f = pathToFile(p);
    if (f.isFile()) {
      return f.delete();
    } else if ((!recursive) && f.isDirectory() && 
        (f.listFiles().length != 0)) {
      throw new IOException("Directory " + f.toString() + " is not empty");
    }
    return FileUtil.fullyDelete(f);
  }
 
  public FileStatus[] listStatus(Path f) throws IOException {
    File localf = pathToFile(f);
    FileStatus[] results;

    if (!localf.exists()) {
      return null;
    }
    if (localf.isFile()) {
      return new FileStatus[] {
          new RawLocalFileStatus(localf, getDefaultBlockSize(), this) };
    }

    String[] names = localf.list();
    if (names == null) {
      return null;
    }
    results = new FileStatus[names.length];
    for (int i = 0; i < names.length; i++) {
      results[i] = getFileStatus(new Path(f, names[i]));
    }
    return results;
  }

  /**
   * Creates the specified directory hierarchy. Does not
   * treat existence as an error.
   */
  public boolean mkdirs(Path f) throws IOException {
    Path parent = f.getParent();
    File p2f = pathToFile(f);
    return (parent == null || mkdirs(parent)) &&
      (p2f.mkdir() || p2f.isDirectory());
  }

  /** {@inheritDoc} */
  @Override
  public boolean mkdirs(Path f, FsPermission permission) throws IOException {
    boolean b = mkdirs(f);
    setPermission(f, permission);
    return b;
  }
  
  @Override
  public Path getHomeDirectory() {
    return new Path(System.getProperty("user.home")).makeQualified(this);
  }

  /**
   * Set the working directory to the given directory.
   */
  @Override
  public void setWorkingDirectory(Path newDir) {
    workingDir = newDir;
  }
  
  @Override
  public Path getWorkingDirectory() {
    return workingDir;
  }
  
  // In the case of the local filesystem, we can just rename the file.
  public void moveFromLocalFile(Path src, Path dst) throws IOException {
    rename(src, dst);
  }
  
  // We can write output directly to the final location
  public Path startLocalOutput(Path fsOutputFile, Path tmpLocalFile)
    throws IOException {
    return fsOutputFile;
  }
  
  // It's in the right place - nothing to do.
  public void completeLocalOutput(Path fsWorkingFile, Path tmpLocalFile)
    throws IOException {
  }
  
  public void close() throws IOException {
    super.close();
  }
  
  public String toString() {
    return "LocalFS";
  }
  
  public FileStatus getFileStatus(Path f) throws IOException {
    File path = pathToFile(f);
    if (path.exists()) {
      return new RawLocalFileStatus(pathToFile(f), getDefaultBlockSize(), this);
    } else {
      throw new FileNotFoundException( "File " + f + " does not exist.");
    }
  }

  static class RawLocalFileStatus extends LocalFileStatus {
    /* We can add extra fields here. It breaks at least CopyFiles.FilePair().
     * We recognize if the information is already loaded by check if
     * onwer.equals("").
     */
    private boolean isPermissionLoaded() {
      return super.getPermission() != null || !super.getOwner().equals(""); 
    }
    
    RawLocalFileStatus(File f, long defaultBlockSize, FileSystem fs) {
      super(f, f.length(), f.isDirectory(), f.isHidden(), 1, defaultBlockSize,
            f.lastModified(), new Path(f.getPath()).makeQualified(fs));
    }
    
    @Override
    public FsPermission getPermission() {
      if (!isPermissionLoaded()) {
        loadPermissionInfo();
      }
      return super.getPermission();
    }

    @Override
    public String getOwner() {
      if (!isPermissionLoaded()) {
        loadPermissionInfo();
      }
      return super.getOwner();
    }

    @Override
    public String getGroup() {
      if (!isPermissionLoaded()) {
        loadPermissionInfo();
      }
      return super.getGroup();
    }

    /// loads permissions, owner, and group from `ls -ld`
    private synchronized void loadPermissionInfo() {
      setPermission(null);
      setOwner(null);
      setGroup(null);
        
      IOException e = null;
      try {
    	File file = new File(getPath().toUri());
    	if (Shell.WINDOWS) { 
    		boolean canRead = file.canRead();
    		boolean canWrite = file.canWrite();
    		boolean canExecute = file.canExecute();
    		String permission = "" + (canRead?'r':'-') + (canWrite?'r':'-') + (canExecute?'r':'-');
    		permission = "" + (isDir()?'d':'-') + permission + permission + permission;
    		setPermission(FsPermission.valueOf(permission));
    	    setOwner(null);
    	    setGroup(null);
    	} else { 
	      StringTokenizer t = new StringTokenizer(
	          execCommand(file, Shell.getGET_PERMISSION_COMMAND()));
	      //expected format
	      //-rw-------    1 username groupname ...
	      String permission = t.nextToken();
	      if (permission.length() > 10) { //files with ACLs might have a '+'
	        permission = permission.substring(0, 10);
	      }
	      setPermission(FsPermission.valueOf(permission));
	      t.nextToken();
	      setOwner(t.nextToken());
	      setGroup(t.nextToken());
    	}
      } catch (Shell.ExitCodeException ioe) {
        if (ioe.getExitCode() != 1) {
          e = ioe;
        } else {
          setPermission(null);
          setOwner(null);
          setGroup(null);
        }
      } catch (IOException ioe) {
        e = ioe;
      } finally {
        if (e != null) {
          throw new RuntimeException("Error while running command to get " +
                                     "file permissions : " + 
                                     StringUtils.stringifyException(e));
        }
      }
    }

    @Override
    public void write(DataOutput out) throws IOException {
      if (!isPermissionLoaded()) {
        loadPermissionInfo();
      }
      super.write(out);
    }
  }

  /**
   * Use the command chown to set owner.
   */
  @Override
  public void setOwner(Path p, String username, String groupname) 
	  throws IOException {
    if (username == null && groupname == null) {
      throw new IOException("username == null && groupname == null");
    }

	if (Shell.WINDOWS) { 
		if (LOG.isDebugEnabled()) {
			LOG.debug("setOwner: " + Shell.SET_OWNER_COMMAND 
					+ " not supported for win32");
		}
		return;
	}
    
    if (username == null) {
      execCommand(pathToFile(p), Shell.SET_GROUP_COMMAND, groupname); 
    } else {
      //OWNER[:[GROUP]]
      String s = username + (groupname == null? "": ":" + groupname);
      execCommand(pathToFile(p), Shell.SET_OWNER_COMMAND, s);
    }
  }

  /**
   * Use the command chmod to set permission.
   */
  @Override
  public void setPermission(Path p, FsPermission permission) 
	  throws IOException {
	if (Shell.WINDOWS) { 
		if (LOG.isDebugEnabled()) {
			LOG.debug("setPermission: " + Shell.SET_PERMISSION_COMMAND 
					+ " not supported for win32");
		}
		return;
	}
	
    execCommand(pathToFile(p), Shell.SET_PERMISSION_COMMAND,
        String.format("%04o", permission.toShort()));
  }

  private static String execCommand(File f, String... cmd) throws IOException {
    String[] args = new String[cmd.length + 1];
    System.arraycopy(cmd, 0, args, 0, cmd.length);
    args[cmd.length] = f.getCanonicalPath();
    String output = Shell.execCommand(args);
    return output;
  }
}

