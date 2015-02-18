package org.javenstudio.jfm.filesystems.dfs;

import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI; 
import javax.swing.JOptionPane; 

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.filesystems.JFMFileSystem;
import org.javenstudio.jfm.po.FsLoginDialog; 
import org.javenstudio.jfm.main.Options; 

import org.javenstudio.raptor.conf.Configuration; 
import org.javenstudio.raptor.conf.ConfigurationFactory; 
import org.javenstudio.raptor.fs.FileSystem; 
import org.javenstudio.raptor.fs.Path; 
import org.javenstudio.raptor.fs.FileStatus; 
import org.javenstudio.raptor.fs.permission.FsPermission;
import org.javenstudio.raptor.fs.permission.FsAction;
import org.javenstudio.raptor.dfs.server.namenode.NameNode; 


public class JFMDfsFilesystem extends JFMFileSystem {

  private boolean connected = false; 
 
  public JFMDfsFilesystem() {
    super();
    initialize(); 
  }

  public boolean isConnected() { 
    return connected; 
  }

  private void initialize() {
    synchronized (lockFs) {
      String message = null; 
      try {
        if (Class.forName("org.javenstudio.raptor.conf.Configuration") == null || 
            Class.forName("org.javenstudio.raptor.fs.FileSystem") == null || 
            Class.forName("org.javenstudio.raptor.dfs.server.namenode.NameNode") == null) {
          message = "dfs class: org.javenstudio.raptor.fs.FileSystem can not found."; 
        }
      } catch (Exception e) {
        e.printStackTrace(); 
        message = "Check dfs Classes failed: " + e.toString(); 
      }
      if (message != null) {
        JOptionPane.showMessageDialog(Options.getMainFrame(), 
            message, "ClassNotFound Error", JOptionPane.ERROR_MESSAGE);
        return; 
      }
      initStatic(); 
      if (fs != null) {
        connected = true; 
        return; 
      }
    }

    String prefUri = Options.getPreferences().get("JFM.fs.default.name", "dfs://localhost"); 
    String prefUser = Options.getPreferences().get("JFM.raptor.job.ugi", "root,supergroup"); 
    String defaultUri = conf.get("fs.default.name", prefUri); 
    String userGroup = conf.get("raptor.job.ugi", prefUser); 
    String host = null, user = null, group = null; 
    int port = 0; 
    if (defaultUri != null && defaultUri.length() > 0) {
      try {
        URI uri = new URI(defaultUri); 
        host = uri.getHost(); 
        port = uri.getPort(); 
      } catch (Exception e) {
        host = null; 
      }
    }
    if (userGroup != null && userGroup.length() > 0) {
      int pos = userGroup.indexOf(','); 
      if (pos < 0) {
        user = userGroup; 
        group = null; 
      } else {
        user = userGroup.substring(0, pos); 
        group = userGroup.substring(pos+1); 
      }
    }
    if (port <= 0) port = NameNode.DEFAULT_PORT; 
    if (host == null || host.length() == 0 || host.equals("0.0.0.0")) host = "localhost";
    FsLoginDialog d = FsLoginDialog.showDialog("dfs", host, port, user, group); 
    host = d.getHost(); port = d.getPort(); 
    if (!d.isCanceled() && host != null && host.length() > 0) {
      defaultUri = "dfs://" + d.getHost() + ":" + d.getPort(); 
      userGroup = d.getUser() + "," + d.getGroup(); 
      conf.set("fs.default.name", defaultUri); 
      conf.set("raptor.job.ugi", userGroup); 
      Options.getPreferences().put("JFM.fs.default.name", defaultUri); 
      Options.getPreferences().put("JFM.raptor.job.ugi", userGroup); 
    } else {
      conf.set("fs.default.name", ""); 
      return; 
    }

    synchronized (lockFs) {
      Options.showStatus(Strings.format("connecting to %1$s", defaultUri)); 
      fsDefaultUri = defaultUri; 
      fsUserGroup = userGroup; 
      fsUser = user; 
      fs = null; getFs(true); 
      Options.showStatus(null); 
      if (fs == null) {
        connected = false; 
        String message = Strings.format("Connect to dfs file system (%1$s) failed", defaultUri); 
        if (fsexp != null) { 
          message += "\n\n  " + fsexp.getMessage(); 
        }
        JOptionPane.showMessageDialog(Options.getMainFrame(), 
            message, Strings.get("Connect Error"), JOptionPane.ERROR_MESSAGE);
      } else {
        Options.addHistoryUri("dfs", host, port, user);
        connected = true; 
      }
    }
  }

  private static void initStatic() {
    synchronized (lockFs) {
      if (conf == null) conf = ConfigurationFactory.get(); 
      if (FILE_READONLY == null) 
        FILE_READONLY = FsPermission.createImmutable((short) 0644); // rw-r--r--
      if (DIR_PERMISSION == null) 
        DIR_PERMISSION = FsPermission.createImmutable((short) 0777); // rwx-rwx-rwx
    }
  }

  private static FsPermission FILE_READONLY = null; 
  //  FsPermission.createImmutable((short) 0644); // rw-r--r--

  // directory is world readable/writable/executable
  private static FsPermission DIR_PERMISSION = null; 
  //  FsPermission.createImmutable((short) 0777); // rwx-rwx-rwx

  private static Configuration conf = null; 
  private static FileSystem fs = null; 
  private static String fsDefaultUri = null; 
  @SuppressWarnings("unused")
  private static String fsUserGroup = null; 
  private static String fsUser = null; 
  private static Exception fsexp = null; 
  private static Object lockFs = new Object(); 

  public static FileSystem getFs() {
    return getFs(false); 
  }

  private static FileSystem getFs(boolean force) {
    synchronized (lockFs) {
      try {
        if (fs == null && force) {
          fsexp = null; 
          initStatic(); 
          fs = FileSystem.get(conf); 
        }
        return fs; 
      } catch (Exception e) {
        fs = null; 
        fsexp = e; 
        e.printStackTrace(); 
        return fs; 
      }
    }
  }

  public static boolean setConnectUri(URI uri) {
    if (uri == null || !"dfs".equals(uri.getScheme()))
      return false;

    initStatic(); 

    String prefUser = Options.getPreferences().get("JFM.raptor.job.ugi", "root,supergroup");
    String userGroup = conf.get("raptor.job.ugi", prefUser);
    @SuppressWarnings("unused")
	String host = null, user = null, group = null;
    if (userGroup != null && userGroup.length() > 0) {
      int pos = userGroup.indexOf(',');
      if (pos < 0) {
        user = userGroup;
        group = null;
      } else {
        user = userGroup.substring(0, pos);
        group = userGroup.substring(pos+1);
      }
    }

    String defaultUri = "dfs://" + uri.getHost() + ":" + uri.getPort(); 
    String userInfo = uri.getUserInfo();
    if (userInfo != null) user = userInfo; 
    if (user == null) user = "root"; 
    if (group == null) group = "supergroup"; 
    userGroup = user + "," + group; 

    conf.set("fs.default.name", defaultUri);
    conf.set("raptor.job.ugi", userGroup);
    Options.getPreferences().put("JFM.fs.default.name", defaultUri);
    Options.getPreferences().put("JFM.raptor.job.ugi", userGroup);

    return true;
  }

  public static int tryDisconnect() {
    synchronized (lockFs) {
      if (fs != null) {
        try {
          String str = Strings.format("Are you sure to disconnect from %1$s?", fsDefaultUri);
          Object[] options = { Strings.get("Yes"), Strings.get("No") };
          int foo = JOptionPane.showOptionDialog(null,
                 str, Strings.get("Warning"),
                 JOptionPane.DEFAULT_OPTION,
                 JOptionPane.WARNING_MESSAGE,
                 null, options, options[0]);
          if (foo == 0) {
            fs.close(); fs = null; 
            return 1;
          } else
            return 2; 
        } catch (Exception e) {
          e.printStackTrace();
          fs = null;
          return -1; 
        }
      }
    }
    return 0;
  }

  public static String getConnectName() { 
    synchronized (lockFs) {
      if (fs != null) 
        return Strings.format("Disconnect from %1$s", fsDefaultUri); 
    }
    return null; 
  }

  public static String getLongName() { 
    synchronized (lockFs) {
      if (fs != null) 
        return fsDefaultUri + " - " + fsUser; 
    }
    return "dfs (" + Strings.get("not connected") + ")"; 
  }

  public static String getIconName() {
    return "/images/icons/gnome-fs-nfs.png"; 
  }

  public String getDefaultUri() { return fsDefaultUri; }
  public String getScheme() { return "dfs"; }
  public String getSchemeAuthority() { return fsDefaultUri; }

  public String getAuthority() {
    if (fsDefaultUri != null) {
      int pos = fsDefaultUri.indexOf("://"); 
      if (pos > 0) return fsDefaultUri.substring(pos+3); 
      return fsDefaultUri; 
    }
    return null; 
  }

  public char getPathSeparator() {
    return Path.SEPARATOR_CHAR; 
  }

  public char getSeparator() {
    return Path.SEPARATOR_CHAR; 
  }

  public JFMFile getDefaultRootDirectory() { 
    try {
      JFMFile dir = getFile(getFs().getHomeDirectory());
      if (dir.exists()) return dir; 
    } catch (Exception e) {
      printStackTrace(e); 
    }
    return getFile("/"); 
  }

  public String getName() { 
	return fsDefaultUri; /*getFs().getName();*/ 
  }

  public JFMFile[] listRoots() {
    return new JFMFile[]{getFile("/")}; 
  }

  public JFMFile[] listFiles(JFMFile root) throws IOException {
    return listFiles(root, null); 
  }

  public JFMFile[] listFiles(JFMFile rootFile, FilenameFilter filter) throws IOException {
    try {
      JFMFile[] localFiles = null; 
      FileStatus[] paths = getFs().listStatus(new Path(rootFile.getAbsolutePath())); 
      if (paths != null) {
        localFiles = new JFMFile[paths.length]; 
        for (int i=0; i < paths.length; i++) 
          localFiles[i] = getFile(paths[i]); 
      }
      return localFiles;
    } catch (Exception ex) {
      printStackTrace(ex); 
      if (ex instanceof IOException) 
        throw (IOException)ex; 
      else
        throw new IOException(ex.toString()); 
    }
  }

  public boolean mkdir(JFMFile file) throws IOException {
    return mkdirs(file); 
  }

  public boolean mkdirs(JFMFile file) throws IOException {
    try {
      String path = file.getAbsolutePath(); 
      if (!path.endsWith("/")) path += "/"; 
      return getFs().mkdirs(new Path(path)); 
    } catch (Exception ex) {
      printStackTrace(ex); 
      if (ex instanceof IOException) 
        throw (IOException)ex; 
      else
        throw new IOException(ex.toString()); 
    }
  }

  @SuppressWarnings("deprecation")
  public boolean delete(JFMFile file) throws IOException {
    try {
      return getFs().delete(new Path(file.getAbsolutePath())); 
    } catch (Exception ex) {
      printStackTrace(ex); 
      if (ex instanceof IOException) 
        throw (IOException)ex; 
      else
        throw new IOException(ex.toString()); 
    }
  }

  public boolean createNewFile(JFMFile file) throws IOException {
    try{
      return getFs().create(new Path(file.getAbsolutePath()), false) != null; 
    }catch(IOException ex){
      printStackTrace(ex); 
      if (ex instanceof IOException) 
        throw (IOException)ex; 
      else
        throw new IOException(ex.toString()); 
    }
  }

  public boolean setLastModified(JFMFile file, long time) throws IOException {
    try {
      getFs().setTimes(new Path(file.getAbsolutePath()), time, -1); 
      return true; 
    } catch (Exception ex) {
      printStackTrace(ex); 
      if (ex instanceof IOException) 
        throw (IOException)ex; 
      else
        throw new IOException(ex.toString()); 
    }
  }

  public boolean setReadOnly(JFMFile file) throws IOException {
    try {
      getFs().setPermission(new Path(file.getAbsolutePath()), FILE_READONLY); 
      return true; 
    } catch (Exception ex) {
      printStackTrace(ex); 
      if (ex instanceof IOException) 
        throw (IOException)ex; 
      else
        throw new IOException(ex.toString()); 
    }
  }

  public boolean canRead(JFMFile file) {
    try {
      FileStatus status = getFs().getFileStatus(new Path(file.getAbsolutePath())); 
      FsPermission permission = status.getPermission(); 
      FsAction action = permission.getUserAction(); 
      return action.implies(FsAction.READ); 
    } catch (Exception e) {
      printStackTrace(e); 
      return false; 
    }
  }

  public boolean canWrite(JFMFile file) {
    try {
      FileStatus status = getFs().getFileStatus(new Path(file.getAbsolutePath())); 
      FsPermission permission = status.getPermission(); 
      FsAction action = permission.getUserAction(); 
      return action.implies(FsAction.WRITE); 
    } catch (Exception e) {
      printStackTrace(e); 
      return false; 
    }
  }

  public boolean exists(JFMFile file) {
    try {
      return getFs().exists(new Path(file.getAbsolutePath())); 
    } catch (Exception e) {
      printStackTrace(e); 
      return false; 
    }
  }

  public JFMFile getStartDirectory(){
    return getDefaultRootDirectory(); 
  }

  public JFMFile getFile(String pathName) {
    return new JFMDfsFile(this, pathName);
  }

  public JFMFile getFile(Path pathName) {
    return new JFMDfsFile(this, pathName.toUri().getPath());
  }

  public JFMFile getFile(FileStatus status) {
    return new JFMDfsFile(this, status);
  }

  @Override
  public boolean mkdir(JFMFile parent, String name) throws IOException {
    return mkdir(getFile(new Path(parent.getAbsolutePath(), name))); 
  }

  @Override
  public boolean createNewFile(JFMFile parent, String name) throws IOException {
    return createNewFile(getFile(new Path(parent.getAbsolutePath(), name))); 
  }
    
  @Override
  public boolean isLocal() {
    return false;
  }

  private void printStackTrace(Exception e) {
    if (e != null) e.printStackTrace(); 
  }
}
