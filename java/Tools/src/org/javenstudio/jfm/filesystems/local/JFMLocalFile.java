package org.javenstudio.jfm.filesystems.local;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream; 
import java.io.FileOutputStream; 
import java.util.Properties; 
import java.net.URI; 
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

import org.javenstudio.jfm.filesystems.JFMFile;


public class JFMLocalFile extends JFMFile {
  
  private FileSystemView view = FileSystemView.getFileSystemView();
  private File theFile = null;
  private Properties props = null; 
  private Icon icon = null; 

  /**
   * Constructor of this file.
   */
  public JFMLocalFile(Object data) {
    super(data);
    initialize();
  }

  private void initialize(){
    theFile = view.createFileObject((String)data); 
    if (theFile == null) theFile = new File((String)data); 
  }

  public void setPath(String path) {
    if (path != null) theFile = view.createFileObject(path);
  }

  public InputStream getInputStream() throws IOException {
    try {
      return new FileInputStream(theFile);
    } catch(Exception exc) {
      //exc.printStackTrace();
      //return null; 
      if (exc instanceof IOException) 
        throw (IOException) exc; 
      else
        throw new IOException(exc.toString()); 
    }
  }

  public JFMFile[] listFiles() { 
    File[] files = view.getFiles(theFile,true);
    if(files == null)
      return null;

    JFMFile[] jfmFiles=new JFMFile[files.length];
    for (int i = 0; i < files.length; i++) {
      jfmFiles[i] = new JFMLocalFile(files[i].getAbsolutePath());
    }

    return jfmFiles;
  }

  public OutputStream getOutputStream() throws IOException {
    try {
      return new FileOutputStream(theFile);
    } catch(Exception exc) {
      //exc.printStackTrace();
      //return null; 
      if (exc instanceof IOException) 
        throw (IOException) exc; 
      else
        throw new IOException(exc.toString()); 
    }
  }

  public String getName() {    
    return theFile.getName();
  }

  public String getParent() {
    try {
      return view.getParentDirectory(theFile).getPath();
    } catch (NullPointerException e) {
      return theFile.getParent(); 
    }
  }

  public JFMFile getParentFile() {
    if(view.isFileSystemRoot(theFile))
      return null;

    //File parentFile = view.getParentDirectory(theFile);
    //if(parentFile != null)
    //  return new JFMLocalFile(parentFile.getAbsolutePath());

    return new JFMLocalFile(getParent());
  }

  public String getPath() {
    return theFile.getPath();
  }

  public boolean isAbsolute() {
    return theFile.isAbsolute();
  }

  public String getAbsolutePath() {
    return theFile.getAbsolutePath();
  }

  public JFMFile getAbsoluteFile() {
    return new JFMLocalFile(theFile.getAbsolutePath());
  }

  public String getCanonicalPath() {
    try{
      return theFile.getCanonicalPath();
    } catch(IOException ex) {
      ex.printStackTrace(); 
      return null; 
    }
  }

  public JFMFile getCanonicalFile() {
    try{
      return new JFMLocalFile(theFile.getCanonicalPath());
    } catch(IOException ex) {
      ex.printStackTrace(); 
      return null; 
    }
  }

  public boolean isDirectory() {
    if(view.isFileSystem(theFile)) 
      return theFile.isDirectory();
    else
      return true;
  }

  public boolean isFile() {
    return theFile.isFile();
  }

  public boolean isHidden() {
    return theFile.isHidden();
  }

  public long lastModified() {
    return theFile.lastModified();
  }

  public long length() {
    return theFile.length();
  }

  public int compareTo(JFMFile pathname) {
    FileSystemView view = FileSystemView.getFileSystemView();

    if(view.isDrive(new File(pathname.getAbsolutePath()))) {
      //prevent a *thing* in windows, that when the isDirectory method is called for a floppy drive
      //an error pops up from javaw. it probably happens the same for a ZIP drive, but i don't have one, so ...
      return this.toString().compareTo(pathname.toString());
    } else {
      if(this.isDirectory() && !pathname.isDirectory()) return -1;
      if(!this.isDirectory() && pathname.isDirectory()) return 1;
      return this.toString().compareTo(pathname.toString());
    }
  }

  public boolean equals(Object obj) {
    return theFile.equals(new File(((JFMFile)obj).getAbsolutePath()));
  }

  public int hashCode() {
    return theFile.hashCode();
  }

  public boolean canRead(){
    return theFile.canRead();
  }

  public boolean canWrite(){
    return theFile.canWrite();
  }

  public boolean exists(){
    return theFile.exists();
  }

  public boolean createNewFile(String name, String path) {
    try{
      File f = new File(theFile, name);
      return f.createNewFile();
    } catch(IOException ex) {
      ex.printStackTrace(); 
      return false; 
    }
  }

  public boolean rename(String name) {
    try {
      File newName = new File(theFile.getParent(), name); 
      return theFile.renameTo(newName); 
    } catch (Exception e) {
      e.printStackTrace(); 
      return false; 
    }
  }

  public boolean renameTo(JFMFile newfile) {
    try {
      return theFile.renameTo(((JFMLocalFile)newfile).theFile); 
    } catch (Exception e) {
      e.printStackTrace(); 
    } 
    return false; 
  }

  public JFMFile mkdir(String name, String path) {
    File newdir= new File(theFile, name);
    if(newdir.exists() || newdir.mkdirs())
      return new JFMLocalFile(newdir.getAbsolutePath());
    else
      return null;
  }

  public JFMFile mkdir(JFMFile dir) {
    if (dir == null || !(dir instanceof JFMLocalFile))
      return null; 
    File newdir = ((JFMLocalFile)dir).theFile;
    if(newdir.exists() || newdir.mkdirs())
      return dir; //new JFMLocalFile(newdir.getAbsolutePath());
    else
      return null;
  }

  public JFMFile createFile(String name, String path) {
    File newfile= new File(theFile,name);
    return new JFMLocalFile(newfile.getAbsolutePath());
  }

  public boolean delete() {
    //return JFMLocalFilesystem.deleteFile(theFile);
    return theFile.delete(); 
  }
      
  public Icon getIcon() {
    if (icon != null) return icon; 
    if (!exists()) return null; 

    //if we're an UP directory, then return null, otherwise ... ask the filesystem.
    if(!"..".equals(this.getDisplayName())) { 
      icon = getRegisteredIcon(this); 
      if (icon == null) {
        icon = view.getSystemIcon(theFile);
        if (icon != null) registerIcon(this, icon); 
      }
    }
    return icon;
  }

  public String getSystemDisplayName() {
    return FileSystemView.getFileSystemView().getSystemDisplayName(theFile);
  }

  public String getFsName() {
    return "local"; 
  }

  public String getFsSchemeName() {
    return "file://"; 
  }

  public Properties getProperties() {
    if (props != null) return props; 
    props = new Properties(); 
    try {
      String path = theFile.getCanonicalPath(); 
      if (path != null) props.setProperty("file.CanonicalPath", path); 
    } catch (Exception ex) {
      ex.printStackTrace(); 
    }
    props.setProperty("file.isAbsolute", Boolean.toString(theFile.isAbsolute())); 
    return props; 
  }

  public boolean canGuessMimeType() {
    return true;
  }

  public File getLocalFile() {
    return theFile; 
  }

  public URI toURI() {
    return theFile.toURI(); 
  }
}
