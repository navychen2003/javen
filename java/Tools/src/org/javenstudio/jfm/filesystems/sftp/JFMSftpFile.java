package org.javenstudio.jfm.filesystems.sftp;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties; 
import java.net.URI; 
import javax.swing.Icon;

import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.main.Options; 

import com.jcraft.jsch.*;


public class JFMSftpFile extends JFMFile {
 
  private JFMSftpFilesystem filesystem = null; 
  private String thePath = null; 
  private SftpATTRS attrs = null; 
  private Properties props = null; 
  private Icon icon = null; 
  @SuppressWarnings("unused")
  private boolean attrsGot = false; 

  /**
   * Constructor of this file.
   */
  public JFMSftpFile(JFMSftpFilesystem fs, Object data) {
    super(data);
    this.filesystem = fs; 
    initialize();
  }

  private void initialize() {
    thePath = Options.normalizePath((String)data); 
  }

  @SuppressWarnings("static-access")
  private ChannelSftp getFs() {
    return filesystem.getFs(); 
  }

  private JFMSftpFilesystem getFileSystem() {
    return filesystem; 
  }

  public synchronized SftpATTRS getAttrs() throws SftpException, FileNotFoundException {
    if (attrs == null) {
      synchronized (JFMSftpFilesystem.lockFs) {
        try {
          attrsGot = true; 
          attrs = getFs().stat(thePath); 
        } catch (SftpException e) {
          if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE)
            throw new FileNotFoundException(thePath+" not found: "+e.toString()); 
          else
            throw e.appendMessage(thePath); 
        }
      }
    }
    return attrs; 
  }

  public void setPath(String path) {
    if (path != null) thePath = path; 
  }

  public InputStream getInputStream() throws IOException {
    try {
      synchronized (JFMSftpFilesystem.lockFs) {
        return getFs().get(thePath); 
      }
    } catch (Exception exc) {
      if (exc instanceof IOException) 
        throw (IOException) exc; 
      else
        throw new IOException(exc.toString()); 
    }
  }

  public JFMFile[] listFiles() throws IOException { 
    return getFileSystem().listFiles(this); 
  }

  public OutputStream getOutputStream() throws IOException {
    try {
      synchronized (JFMSftpFilesystem.lockFs) {
        return getFs().put(thePath);
      }
    } catch (Exception exc) {
      if (exc instanceof IOException) 
        throw (IOException) exc; 
      else
        throw new IOException(exc.toString()); 
    }
  }

  public String getName() {    
    int slash = thePath.lastIndexOf('/');
    return thePath.substring(slash+1);
  }

  public String getParent() {
    if ("/".equals(thePath)) 
      return null; 
    int slash = thePath.lastIndexOf('/');
    if (slash == 0 && thePath.length() > 1)
      return "/"; 
    else if (slash > 0) 
      return thePath.substring(0, slash); 
    else
      return null; 
  }

  public JFMFile getParentFile() {
    String parent = getParent(); 
    if (parent != null) 
      return getFileSystem().getFile(parent); 
    else
      return null; 
  }

  public String getPath() {
    return thePath; 
  }

  public boolean isAbsolute() {
    //int start = Options.hasWindowsDrive(thePath, true) ? 3 : 0;
    //return thePath.startsWith(getFileSystem().getSeparator(), start);
    return Options.startsWithRoot(thePath); 
  }

  public String getAbsolutePath() {
    return thePath;
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
      return getAttrs().isDir(); 
    } catch (Exception e) {
      printStackTrace(e);
      return false; 
    }
  }

  public boolean isFile() {
    try {
      return !getAttrs().isDir(); 
    } catch (Exception e) {
      printStackTrace(e);
      return false; 
    }
  }

  public boolean isHidden() {
    return false; 
  }

  public long lastModified() {
    try {
      return ((long)getAttrs().getMTime())*1000; 
    } catch (Exception e) {
      printStackTrace(e);
      return 0; 
    }
  }

  public long lastAccessed() {
    try {
      return ((long)getAttrs().getATime())*1000; 
    } catch (Exception e) {
      printStackTrace(e);
      return 0; 
    }
  }

  public long length() {
    try {
      return (long)getAttrs().getSize(); 
    } catch (Exception e) {
      printStackTrace(e);
      return 0; 
    }
  }

  public int compareTo(JFMFile other) {
    try {
      if (other == null || !(other instanceof JFMSftpFile))
        return 1; 
      SftpATTRS attrs1 = getAttrs(); 
      SftpATTRS attrs2 = ((JFMSftpFile)other).getAttrs(); 
      if (attrs1.isDir() != attrs2.isDir()) {
        if (attrs1.isDir()) return -1; 
        return 1; 
      }
      return thePath.compareTo(other.getAbsolutePath()); 
    } catch (Exception e) {
      printStackTrace(e);
      return -1; 
    }
  }

  public boolean equals(Object obj) {
    try {
      return thePath.equals(((JFMSftpFile)obj).thePath);
    } catch (Exception e) {
      printStackTrace(e);
      return false;
    }
  }

  public int hashCode() {
    return thePath.hashCode(); 
  }

  public boolean canRead() {
    try {
      int p = getAttrs().getPermissions(); 
      if ((p & SftpATTRS.S_IRUSR) != 0)
        return true; 
    } catch (Exception e) {
      printStackTrace(e);
    }
    return false;
  }

  public boolean canWrite() {
    try {
      int p = getAttrs().getPermissions(); 
      if ((p & SftpATTRS.S_IWUSR) != 0)
        return true; 
    } catch (Exception e) {
      printStackTrace(e);
    }
    return false;
  }

  public boolean exists(){
    try {
      return getAttrs() != null;
    } catch (FileNotFoundException ex) {
      return false;
    } catch (SftpException e) {
      printStackTrace(e);
      return false;
    }
  }

  public JFMFile mkdir(String name, String path) throws IOException {
    JFMFile newdir = getFileSystem().getFile(thePath + "/" + name); 
    if (getFileSystem().mkdir(newdir)) 
      return newdir; 
    else 
      return null; 
  }

  public JFMFile mkdir(JFMFile newdir) throws IOException {
    if (getFileSystem().mkdir(newdir)) 
      return newdir; 
    else 
      return null; 
  }

  public boolean createNewFile(String name, String path) throws IOException {
    return getFileSystem().createNewFile(this, name);
  }

  public boolean rename(String name) throws IOException {
    String parent = getParent(); 
    if (parent == null || name == null || name.length() == 0) 
      return false; 

    String newPath = parent + "/" + name;
    try {
      synchronized (JFMSftpFilesystem.lockFs) {
        getFs().rename(thePath, newPath);
        return true; 
      }
    } catch (Exception e) {
      printStackTrace(e);
      if (e instanceof IOException) 
        throw (IOException)e; 
      else
        throw new IOException(e.toString()); 
    }
  }

  public boolean renameTo(JFMFile newfile) throws IOException {
    try {
      synchronized (JFMSftpFilesystem.lockFs) {
        String newPath = ((JFMSftpFile)newfile).thePath; 
        getFs().rename(thePath, newPath);
        return true; 
      }
    } catch (Exception e) {
      printStackTrace(e);
      if (e instanceof IOException) 
        throw (IOException)e; 
      else
        throw new IOException(e.toString()); 
    }
  }

  public JFMFile createFile(String name, String path) throws IOException {
    return getFileSystem().getFile(thePath + "/" + name); 
  }

  public boolean delete() throws IOException {
    return getFileSystem().delete(this); 
  }
      
  public Icon getIcon() {
    if (icon != null) return icon; 

    //if we're an UP directory, then return null, otherwise ... ask the filesystem.
    if(!"..".equals(this.getDisplayName())){           
      icon = getRegisteredIcon(this);
    }
    return icon;
  }

  public String getSystemDisplayName() {
    return thePath; 
  }

  public String getFsName() {
    return getFileSystem().getName();
  }

  @SuppressWarnings("static-access")
  public String getFsSchemeName() {
    return getFileSystem().getSchemeAuthority(); 
  }

  @SuppressWarnings("static-access")
  public URI toURI() {
    try {
      String path = thePath; 
      if (!path.startsWith("/")) path = "/" + path; 
      return new URI(getFileSystem().getScheme(), getFileSystem().getAuthority(), path, null, null); 
    } catch (Exception e) {
      e.printStackTrace(); 
      return null; 
    }
  }

  public synchronized Properties getProperties() {
    if (props != null) return props;
    props = new Properties();

    try { 
      props.setProperty("file.UId",       ""+getAttrs().getUId()); 
      props.setProperty("file.GId",       ""+getAttrs().getGId()); 
      props.setProperty("file.Permission",""+getAttrs().getPermissionsString()); 
    } catch (Exception ex) { 
      ex.printStackTrace(); 
    }

    return props;
  }

  private void printStackTrace(Exception e) {
    if (e != null && !(e instanceof FileNotFoundException)) 
      e.printStackTrace(); 
  }

}
