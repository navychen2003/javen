package org.javenstudio.jfm.filesystems.local;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.net.InetAddress; 

import javax.swing.filechooser.FileSystemView;

import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.filesystems.JFMFileSystem;


public class JFMLocalFilesystem extends JFMFileSystem {
  private FileSystemView view = FileSystemView.getFileSystemView();
    
  public JFMLocalFilesystem() {
    super();
  }

  private static String hostname = null; 
  private static String username = null; 
  private static String longname = null; 
  static {
    try { 
      hostname = InetAddress.getLocalHost().getHostName(); 
    } catch (Exception uhe) { 
      hostname = "localhost"; 
    }
    username = System.getProperty("user.name"); 
    longname = "local (" + hostname + ") - " + username; 
  }

  public static String getLongName() {
    return longname; 
  }

  public static String getIconName() {
    return "/images/icons/network_local.png"; 
  }

  public boolean isConnected() { 
    return true; 
  }

  public char getPathSeparator() {
    return File.pathSeparatorChar;
  }

  public char getSeparator() {
    return File.separatorChar;
  }

  private JFMFile[] getJFMArrayFromFiles(File[] files) { 
    if(files == null) return null;    
    JFMFile[] jfmFiles = new JFMFile[files.length];
    for(int i=0; i<files.length; i++) {
      try{
        jfmFiles[i] = new JFMLocalFile(files[i].getAbsolutePath());
      } catch(Exception ignored) {
        ignored.printStackTrace();
      }
    }
    Arrays.sort(jfmFiles);
    return jfmFiles;
  }

  public JFMFile getDefaultRootDirectory() { 
    //return as the users default root dir the root dir of the users home dir        
    return getRootDriveFile(new JFMLocalFile(view.getHomeDirectory()));
  }

  public String getName() { return "local"; }

  public JFMFile[] listRoots() {
    File[] roots = File.listRoots();
    return getJFMArrayFromFiles(roots);
  }

  public JFMFile[] listFiles(JFMFile root) {
    File f = new File(root.getAbsolutePath());
    File[] localFiles = view.getFiles(f,true);
    return getJFMArrayFromFiles(localFiles);
  }

  public JFMFile[] listFiles(JFMFile rootFile,FilenameFilter filter) {
    File f = new File(rootFile.getAbsolutePath());
    
    File[] localFiles = f.listFiles(filter);
    return getJFMArrayFromFiles(localFiles);
  }

  public boolean mkdir(JFMFile file) {
    File f = new File(file.getAbsolutePath());
    return f.mkdirs();
  }

  public boolean mkdirs(JFMFile file) {
    File f = new File(file.getAbsolutePath());
    return f.mkdirs();
  }

  public boolean delete(JFMFile file) {
    return delete(file, false); 
  }

  public boolean delete(JFMFile file, boolean recurse) {
    File f = new File(file.getAbsolutePath());
    return recurse ? deleteFile(f) : f.delete(); 
  }

  public static boolean deleteFile(File file) {
    if (file == null || !file.exists()) 
      return true; 
    if (file.isDirectory()) {
      boolean result = true; 
      File[] files = file.listFiles(); 
      for (int i=0; files != null && i < files.length; i++) {
        if (deleteFile(files[i]) == false) 
          result = false; 
      }
      if (result) result = file.delete(); 
      return result; 
    } else 
      return file.delete();
  }

  public boolean createNewFile(JFMFile file) {
    try{
      File f = new File(file.getAbsolutePath());
      return f.createNewFile();
    } catch(IOException ex) {
      ex.printStackTrace(); 
      return false; 
    }
  }

  public boolean setLastModified(JFMFile file, long time) {
    File f = new File(file.getAbsolutePath());
    return f.setLastModified(time);
  }

  public boolean setReadOnly(JFMFile file) {
    File f = new File(file.getAbsolutePath());
    return f.setReadOnly();
  }

  public boolean canRead(JFMFile file) {
    File f = new File(file.getAbsolutePath());
    return f.canRead();
  }

  public boolean canWrite(JFMFile file) {
    File f = new File(file.getAbsolutePath());
    return f.canWrite();
  }

  public boolean exists(JFMFile file) {
    File f = new File(file.getAbsolutePath());
    return f.exists();
  }

  public JFMFile getStartDirectory(){
    return getFile(System.getProperty("user.home"));
  }

  public JFMFile getFile(String pathName){
    return new JFMLocalFile(pathName);
  }

  @Override
  public boolean mkdir(JFMFile parent, String name) {
    File parentFile = new File(parent.getAbsolutePath(), name); 
    return parentFile.mkdirs();
  }

  @Override
  public boolean createNewFile(JFMFile parent, String name) {
    try {
      File f = new File(parent.getAbsolutePath(), name);
      return f.createNewFile();
    } catch(IOException ex) {
      ex.printStackTrace(); 
      return false; 
    }
  }
    
  @Override
  public boolean isLocal() {
    return true; 
  }

}
