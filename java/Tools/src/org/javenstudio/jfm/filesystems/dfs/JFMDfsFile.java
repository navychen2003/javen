package org.javenstudio.jfm.filesystems.dfs;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties; 
import java.net.URI; 
import javax.swing.Icon;

import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.main.Options; 

import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.BlockLocation;
import org.javenstudio.raptor.fs.ContentSummary;
import org.javenstudio.raptor.fs.FileChecksum;
import org.javenstudio.raptor.fs.permission.FsPermission;
import org.javenstudio.raptor.fs.permission.FsAction;


public class JFMDfsFile extends JFMFile {
  
  private JFMDfsFilesystem filesystem = null; 
  private Path thePath = null; 
  private FileStatus status = null; 
  private Properties props = null; 
  private Icon icon = null; 
  @SuppressWarnings("unused")
  private boolean statusGot = false; 

  /**
   * Constructor of this file.
   */
  public JFMDfsFile(JFMDfsFilesystem fs, Object data) {
    super(data);
    this.filesystem = fs; 
    initialize();
  }

  public JFMDfsFile(JFMDfsFilesystem fs, FileStatus status) {
    super(status.getPath().toUri().getPath()); 
    this.filesystem = fs; 
    this.status = status; 
    initialize();
  }

  private void initialize() {
    thePath = new Path((String)data); 
    //setDisplayName(thePath.toUri().getPath()); 
  }

  @SuppressWarnings("static-access")
  private FileSystem getFs() {
    return filesystem.getFs(); 
  }

  private JFMDfsFilesystem getFileSystem() {
    return filesystem; 
  }

  private synchronized FileStatus getStatus() throws IOException {
    if (status == null) {
      statusGot = true; 
      status = getFs().getFileStatus(thePath); 
    }
    return status; 
  }

  public void setPath(String path) {
    if (path != null) thePath = new Path(path); 
  }

  public InputStream getInputStream() throws IOException {
    try {
      return getFs().open(thePath); 
    } catch(IOException exc) {
      //exc.printStackTrace();
      //return null; 
      throw exc; 
    }
  }

  public JFMFile[] listFiles() throws IOException { 
    return getFileSystem().listFiles(this); 
  }

  public OutputStream getOutputStream() throws IOException {
    try {
      return getFs().create(thePath);
    } catch(IOException exc) {
      //exc.printStackTrace();
      //return null; 
      throw exc; 
    }
  }

  public String getName() {    
    return thePath.getName(); 
  }

  public String getParent() {
    Path parent = thePath.getParent(); 
    if (parent != null) 
      return parent.toUri().getPath(); 
    else
      return null; 
  }

  public JFMFile getParentFile() {
    Path parent = thePath.getParent(); 
    if (parent != null) 
      return getFileSystem().getFile(parent.toUri().getPath()); 
    else
      return null; 
  }

  public String getPath() {
    return thePath.toUri().getPath(); 
  }

  public boolean isAbsolute() {
    return thePath.isAbsolute(); 
  }

  public String getAbsolutePath() {
    return thePath.toUri().getPath();
  }

  public JFMFile getAbsoluteFile() {
    return getFileSystem().getFile(getAbsolutePath());
  }

  public String getCanonicalPath() {
    return getAbsolutePath(); 
  }

  public JFMFile getCanonicalFile() {
    return getAbsoluteFile(); 
  }

  public boolean isDirectory() {
    try {
      return getStatus().isDir(); 
    } catch (Exception e) {
      e.printStackTrace();
      return false; 
    }
  }

  public boolean isFile() {
    try {
      return !getStatus().isDir(); 
    } catch (Exception e) {
      e.printStackTrace();
      return false; 
    }
  }

  public boolean isHidden() {
    return false; 
  }

  public long lastModified() {
    try {
      return getStatus().getModificationTime(); 
    } catch (Exception e) {
      e.printStackTrace();
      return 0; 
    }
  }

  public long length() {
    try {
      return getStatus().getLen(); 
    } catch (Exception e) {
      e.printStackTrace();
      return 0; 
    }
  }

  public int compareTo(JFMFile pathname) {
    try {
      FileStatus status1 = getStatus(); 
      FileStatus status2 = getFs().getFileStatus(new Path(pathname.getAbsolutePath())); 
      if (status1.isDir() != status2.isDir()) {
        if (status1.isDir()) return -1; 
        return 1; 
      }
      return status1.compareTo(status2); 
    } catch (Exception e) {
      e.printStackTrace();
      return -1; 
    }
  }

  public boolean equals(Object obj) {
    try {
      FileStatus status2 = getFs().getFileStatus(new Path(((JFMFile)obj).getAbsolutePath())); 
      FileStatus status1 = getStatus();
      return status1.equals(status2);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public int hashCode() {
    return thePath.hashCode(); 
  }

  public boolean canRead(){
    try {
      FsPermission permission = getStatus().getPermission();
      FsAction action = permission.getUserAction();
      return action.implies(FsAction.READ);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean canWrite(){
    try {
      FsPermission permission = getStatus().getPermission();
      FsAction action = permission.getUserAction();
      return action.implies(FsAction.WRITE);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean exists(){
    try {
      return getStatus() != null;
    } catch (FileNotFoundException ex) {
      return false; 
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public JFMFile mkdir(String name, String path) throws IOException {
    JFMFile newDir = getFileSystem().getFile(new Path(thePath, name)); 
    if (getFileSystem().mkdir(newDir)) 
      return newDir; 
    else
      return null; 
  }

  public JFMFile mkdir(JFMFile newDir) throws IOException {
    if (getFileSystem().mkdir(newDir)) 
      return newDir; 
    else
      return null; 
  }

  public boolean createNewFile(String name, String path) throws IOException {
    return getFileSystem().createNewFile(this, name); 
  }

  public boolean rename(String name) throws IOException {
    Path parent = thePath.getParent(); 
    if (parent == null || name == null || name.length() == 0) 
      return false; 

    Path newPath = new Path(parent, name);
    try {
      return getFs().rename(thePath, newPath);
    } catch (Exception ex) {
      ex.printStackTrace();
      if (ex instanceof IOException)
        throw (IOException)ex;
      else
        throw new IOException(ex.toString());
    }
  }

  public boolean renameTo(JFMFile newfile) throws IOException {
    try {
      Path newPath = ((JFMDfsFile)newfile).thePath; 
      return getFs().rename(thePath, newPath);
    } catch (Exception ex) {
      ex.printStackTrace();
      if (ex instanceof IOException)
        throw (IOException)ex;
      else
        throw new IOException(ex.toString());
    }
  }

  public JFMFile createFile(String name, String path) {
    Path newfile = new Path(thePath, name); 
    return getFileSystem().getFile(newfile.toUri().getPath()); 
  }

  @SuppressWarnings("deprecation")
  public boolean delete() throws IOException {
    try {
      getFs().delete(thePath); 
      return true; 
    } catch (Exception ex) {
      ex.printStackTrace();
      if (ex instanceof IOException)
        throw (IOException)ex;
      else
        throw new IOException(ex.toString());
    }
  }
      
  public Icon getIcon(){
    if (icon != null) return icon; 

    //if we're an UP directory, then return null, otherwise ... ask the filesystem.
    if(!"..".equals(this.getDisplayName())){           
      icon = getRegisteredIcon(this);
    }
    return icon;
  }

  public String getSystemDisplayName() {
    return thePath.toUri().getPath(); 
  }

  @SuppressWarnings("deprecation")
  public String getFsName() {
    return getFs().getName();
  }

  public String getFsSchemeName() {
    return getFileSystem().getSchemeAuthority(); 
  }

  public URI toURI() {
    try {
      return new URI(getFileSystem().getScheme(), 
                     getFileSystem().getAuthority(), getAbsolutePath(), null, null); 
    } catch (Exception e) {
      e.printStackTrace(); 
      return null; 
    }
  }

  public synchronized Properties getProperties() {
    if (props != null) return props;
    props = new Properties();

    try { 
      props.setProperty("file.BlockSize",   ""+getStatus().getBlockSize()); 
      props.setProperty("file.Replication", ""+getStatus().getReplication()); 
      props.setProperty("file.Owner",       ""+getStatus().getOwner()); 
      props.setProperty("file.Group",       ""+getStatus().getGroup()); 
      props.setProperty("file.Permission",  ""+getStatus().getPermission()); 
    } catch (Exception ex) { 
      ex.printStackTrace(); 
    }

    try {
      BlockLocation[] locations = getFs().getFileBlockLocations(getStatus(), 0, length()); 
      for (int i=0; locations != null && i < locations.length; i++) {
        BlockLocation location = locations[i]; 
        props.setProperty("file.Blocks."+i, location.toString()); 
      }
    } catch (Exception ex) {
      ex.printStackTrace(); 
    }

    try {
      ContentSummary summary = getFs().getContentSummary(thePath); 
      StringBuffer sbuf = new StringBuffer(); 
      sbuf.append("length:"+summary.getLength()+"("+Options.byteDesc(summary.getLength())+")"); 
      sbuf.append(", dircount:"+summary.getDirectoryCount()); 
      sbuf.append(", filecount:"+summary.getFileCount()); 
      sbuf.append(", quota:"+summary.getQuota()); 
      sbuf.append(", spaceconsumed:"+summary.getSpaceConsumed()); 
      sbuf.append(", spacequota:"+summary.getSpaceQuota()); 
      props.setProperty("file.ContentSummary", sbuf.toString()); 
    } catch (Exception ex) {
      ex.printStackTrace(); 
    }

    if (isFile()) {
      try {
        FileChecksum checksum = getFs().getFileChecksum(thePath); 
        StringBuffer sbuf = new StringBuffer(); 
        sbuf.append("algorithm:"+checksum.getAlgorithmName()); 
        sbuf.append(", length:"+checksum.getLength()); 
        props.setProperty("file.FileChecksum", sbuf.toString()); 
      } catch (Exception ex) {
        ex.printStackTrace(); 
      }
    }

    return props;
  }

}
