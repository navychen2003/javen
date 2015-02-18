package org.javenstudio.jfm.filesystems.sftp;

import java.io.File; 
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.FileNotFoundException; 
import java.io.OutputStream; 
import java.util.ArrayList;
import java.util.Vector; 
import java.net.URI; 
import java.awt.*;
import javax.swing.*; 

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.filesystems.JFMFileSystem;
import org.javenstudio.jfm.po.FsLoginDialog; 
import org.javenstudio.jfm.main.Options; 

import com.jcraft.jsch.*;


public class JFMSftpFilesystem extends JFMFileSystem {

  private boolean connected = false; 
 
  public JFMSftpFilesystem() {
    super();
    initialize(); 
  }

  public boolean isConnected() { 
    return connected; 
  }

  private void initialize() {
    synchronized (lockFs) {
      if (sftp != null) {
        connected = true; 
        return; 
      }

      String sftpAddress = Options.getPreferences().get("JFM.sftp.address", "localhost:22");
      String sftpUser = Options.getPreferences().get("JFM.sftp.user", fsUser);
      String sftpPrivateKey = Options.getPreferences().get("JFM.sftp.privatekey", ""); 
      if (sftpAddress != null && sftpAddress.length() > 0) {
        int pos = sftpAddress.indexOf(':'); 
        if (pos > 0) {
          try {
            fsHost = sftpAddress.substring(0, pos); 
            fsPort = Integer.valueOf(sftpAddress.substring(pos+1)).intValue(); 
          } catch (Exception e) {
            fsHost = sftpAddress; 
            fsPort = 22; 
          }
        } else
          fsHost = sftpAddress; 
        if (fsPort <= 0) fsPort = 22; 
      }
      if (sftpUser != null && sftpUser.length() > 0) {
        fsUser = sftpUser; 
      }
      if (sftpPrivateKey != null && sftpPrivateKey.length() > 0) {
        fsPrivateKey = sftpPrivateKey; 
      }

      FsLoginDialog d = FsLoginDialog.showDialog2("sftp", fsHost, fsPort, fsUser, fsPrivateKey); 
      fsHost = d.getHost(); 
      fsPort = d.getPort(); 
      fsUser = d.getUser(); 
      fsPasswd = d.getPassword(); 
      fsPrivateKey = d.getPrivateKey(); 
      if (d.isCanceled() || fsHost == null || fsHost.length() <= 0) 
        return; 

      Options.getPreferences().put("JFM.sftp.address", fsHost+":"+fsPort);
      Options.getPreferences().put("JFM.sftp.user", fsUser);
      Options.getPreferences().put("JFM.sftp.privatekey", fsPrivateKey);

      Options.showStatus("connecting to sftp://"+fsHost+":"+fsPort); 
      sftp = null; 
      getFs(fsHost, fsPort, fsUser, fsPasswd); 
      Options.showStatus(null); 
      if (sftp == null) {
        connected = false; 
        String message = "Connect to sftp://"+fsHost+":"+fsPort+" failed"; 
        if (fsexp != null) { 
          message += "\n\n  " + fsexp.getMessage(); 
        }
        JOptionPane.showMessageDialog(Options.getMainFrame(), 
            message, "Connect Error", JOptionPane.ERROR_MESSAGE);
      } else {
        Options.addHistoryUri("sftp", fsHost, fsPort, fsUser); 
        connected = true; 
      }
    }
  }

  private static String fsHost = "localhost"; 
  private static int fsPort = 22; 
  private static String fsUser = System.getProperty("user.name"); 
  private static String fsPasswd = null; 
  private static String fsPrivateKey = null; 
  private static JSch jsch = new JSch();
  private static Session session = null; 
  private static UserInfo ui = null; 
  private static Channel channel = null; 
  private static ChannelSftp sftp = null; 
  private static Exception fsexp = null; 
  public final static Object lockFs = new Object(); 

  private static void connect(String host, int port, String user, String passwd) throws Exception {
    synchronized (lockFs) {
      // username and password will be given via UserInfo interface.
      ui = null;
      if (fsPrivateKey != null && fsPrivateKey.length() > 0) {
        try {
          File file = new File(fsPrivateKey); 
          if (file.exists()) {
            jsch.addIdentity(file.getAbsolutePath()
//                           , "passphrase"
                             );
            ui = new MyUserInfo(null, passwd); 
          } else 
            Options.showMessage("Private key file: "+fsPrivateKey+" not found!"); 
        } catch (Exception ex) {
          ex.printStackTrace(); 
          Options.showMessage(ex); 
        }
      }
      if (ui == null) 
        ui = new MyUserInfo(passwd);

      session = jsch.getSession(user, host, port);
      session.setUserInfo(ui);
      session.connect();

      channel = session.openChannel("sftp");
      channel.connect();
      sftp = (ChannelSftp)channel;
    }
  }

  public static class MyUserInfo implements UserInfo, UIKeyboardInteractive{
    public MyUserInfo(String passwd) {
      this(passwd, null); 
    }
    public MyUserInfo(String passwd, String passphrase) {
      this.passwd = passwd; 
      this.passphrase = passphrase; 
    }

    public boolean promptYesNo(String str) {
      Object[] options = { " Yes ", " No " };
      int foo = JOptionPane.showOptionDialog(null, 
             str, "Warning", 
             JOptionPane.DEFAULT_OPTION, 
             JOptionPane.WARNING_MESSAGE,
             null, options, options[0]);
      return foo == 0;
    }
  
    String passphrase = null;
    JTextField passphraseField = (JTextField)new JPasswordField(20);

    public String getPassphrase(){ return passphrase; }
    public boolean promptPassphrase(String message){
      if (passphrase != null && passphrase.length() > 0) 
        return true; 
      Object[] ob = {passphraseField};
      int result = JOptionPane.showConfirmDialog(null, ob, message,
          JOptionPane.OK_CANCEL_OPTION);
      if (result == JOptionPane.OK_OPTION) {
        passphrase = passphraseField.getText();
        return true;
      } else { 
        return false; 
      }
    }

    private String passwd = null;
    private JTextField passwordField = (JTextField)new JPasswordField(20);

    public String getPassword() { return passwd; }
    public boolean promptPassword(String message) {
      if (passwd != null && passwd.length() > 0) 
        return true; 
      Object[] ob = {passwordField}; 
      int result = JOptionPane.showConfirmDialog(null, ob, message,
          JOptionPane.OK_CANCEL_OPTION);
      if (result == JOptionPane.OK_OPTION) {
        passwd = passwordField.getText();
        return true;
      } else { 
        return false; 
      }
    }

    public void showMessage(String message) {
      JOptionPane.showMessageDialog(null, message);
    }
    final GridBagConstraints gbc = 
      new GridBagConstraints(0,0,1,1,1,1,
                             GridBagConstraints.NORTHWEST,
                             GridBagConstraints.NONE,
                             new Insets(0,0,0,0),0,0);
    private Container panel;
    public String[] promptKeyboardInteractive(String destination,
                                              String name,
                                              String instruction,
                                              String[] prompt,
                                              boolean[] echo){
      panel = new JPanel();
      panel.setLayout(new GridBagLayout());

      gbc.weightx = 1.0;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.gridx = 0;
      panel.add(new JLabel(instruction), gbc);
      gbc.gridy++;

      gbc.gridwidth = GridBagConstraints.RELATIVE;

      JTextField[] texts = new JTextField[prompt.length];
      for (int i=0; i<prompt.length; i++){
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.weightx = 1;
        panel.add(new JLabel(prompt[i]),gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1;
        if (echo[i]) {
          texts[i] = new JTextField(20);
        }
        else{
          texts[i] = new JPasswordField(20);
        }
        panel.add(texts[i], gbc);
        gbc.gridy++;
      }

      if (JOptionPane.showConfirmDialog(null, panel, 
                                       destination+": "+name,
                                       JOptionPane.OK_CANCEL_OPTION,
                                       JOptionPane.QUESTION_MESSAGE)
          == JOptionPane.OK_OPTION) {
        String[] response = new String[prompt.length];
        for (int i=0; i<prompt.length; i++) {
          response[i] = texts[i].getText();
        }
        return response;
      }
      else {
        return null;  // cancel
      }
    }
  }

  public static ChannelSftp getFs() {
    return getFs(fsHost, fsPort, fsUser, fsPasswd); 
  }

  private static ChannelSftp getFs(String host, int port, String user, String passwd) {
    synchronized (lockFs) {
      try {
        if (sftp == null) {
          fsexp = null; 
          connect(host, port, user, passwd); 
        }
        return sftp; 
      } catch (Exception e) {
        sftp = null; 
        fsexp = e; 
        e.printStackTrace(); 
        return sftp; 
      }
    }
  }

  public char getPathSeparator() {
    return '/'; 
  }

  public char getSeparator() {
    return '/'; 
  }

  public JFMFile getDefaultRootDirectory() {    
    try {
      synchronized (lockFs) {
        return getFile(getFs().getHome());
      }
    } catch (Exception e) {
      printStackTrace(e); 
      return getFile("/"); 
    }
  }

  public static boolean setConnectUri(URI uri) {
    if (uri == null || !"sftp".equals(uri.getScheme())) 
      return false; 

    Options.getPreferences().put("JFM.sftp.address", uri.getHost()+":"+uri.getPort());
    Options.getPreferences().put("JFM.sftp.user", uri.getUserInfo());

    return true; 
  }

  public static int tryDisconnect() {
    synchronized (lockFs) {
      if (sftp != null) {
        try {
          String str = Strings.format("Are you sure to disconnect from %1$s?", getSchemeAuthority()); 
          Object[] options = { Strings.get("Yes"), Strings.get("No") };
          int foo = JOptionPane.showOptionDialog(null,
                 str, Strings.get("Warning"),
                 JOptionPane.DEFAULT_OPTION,
                 JOptionPane.WARNING_MESSAGE,
                 null, options, options[0]);
          if (foo == 0) {
            session.disconnect(); 
            sftp = null; 
            return 1; 
          } else
            return 2; 
        } catch (Exception e) {
          e.printStackTrace(); 
          sftp = null; 
          return -1; 
        }
      }
    }
    return 0; 
  }

  public static String getConnectName() {
    synchronized (lockFs) {
      if (sftp != null)
        return Strings.format("Disconnect from %1$s", getSchemeAuthority());
    }
    return null; 
  }

  public static String getLongName() {
    synchronized (lockFs) {
      if (sftp != null) 
        return getSchemeAuthority() + " - " + fsUser;
    }
    return "sftp (" + Strings.get("not connected") + ")";
  }

  public static String getIconName() {
    return "/images/icons/gnome-fs-ftp.png";
  }

  public String getName() { return getSchemeAuthority(); }
  public String getHost() { return fsHost; }
  public int getPort() { return fsPort; }
  public static String getScheme() { return "sftp"; }
  public static String getAuthority() { return fsHost + ":" + fsPort; } 
  public static String getSchemeAuthority() { return getScheme() + "://" + getAuthority(); } 

  public JFMFile[] listRoots() {
    return new JFMFile[]{getFile("/")}; 
  }

  public JFMFile[] listFiles(JFMFile root) throws IOException {
    return listFiles(root, null); 
  }

  @SuppressWarnings("rawtypes")
  public JFMFile[] listFiles(JFMFile rootFile, FilenameFilter filter) throws IOException {
    try {
      synchronized (lockFs) {
        JFMFile[] localFiles = null; 
        Vector paths = getFs().ls(rootFile.getAbsolutePath()); 
        if (paths != null) {
          ArrayList<JFMFile> files = new ArrayList<JFMFile>(); 
          for (int i=0; i < paths.size(); i++) {
            String filename = ((ChannelSftp.LsEntry)paths.elementAt(i)).getFilename(); 
            if (".".equals(filename) || "..".equals(filename)) 
              continue; 
            files.add(getFile(rootFile.getAbsolutePath() + "/" + filename)); 
          }
          if (files.size() > 0) 
            localFiles = files.toArray(new JFMFile[files.size()]); 
        }
        return localFiles;
      }
    } catch (Exception e) {
      printStackTrace(e); 
      if (e instanceof IOException) 
        throw (IOException)e; 
      else
        throw new IOException(e.toString()); 
    }
  }

  public boolean mkdir(JFMFile file) throws IOException {
    try {
      synchronized (lockFs) {
        String path = file.getAbsolutePath(); 
        if (!path.endsWith("/")) path += "/"; 
        getFs().mkdir(path); 
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

  public boolean mkdirs(JFMFile file) throws IOException {
    return mkdir(file); 
  }

  public boolean delete(JFMFile file) throws IOException {
    try {
      synchronized (lockFs) {
        if (file.isDirectory()) 
          getFs().rmdir(file.getAbsolutePath()); 
        else 
          getFs().rm(file.getAbsolutePath()); 
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

  public boolean createNewFile(JFMFile file) throws IOException {
    try {
      synchronized (lockFs) {
        if (!file.exists()) {
          OutputStream os = file.getOutputStream(); 
          os.close(); 
          return true; 
        } else 
          throw new IOException("file: "+file.getPath()+" already existed"); 
      }
    } catch(IOException ex) {
      printStackTrace(ex); 
      if (ex instanceof IOException) 
        throw (IOException)ex; 
      else
        throw new IOException(ex.toString()); 
    }
  }

  public boolean setLastModified(JFMFile file, long time) throws IOException {
    try {
      throw new IOException("not implemented"); 
    } catch (Exception ex) {
      printStackTrace(ex); 
      if (ex instanceof IOException) 
        throw (IOException)ex; 
      else
        throw new IOException(ex.toString()); 
    }
  }

  private static int getMode(String s) {
    if (s == null || s.length() != 3) 
      return -1; 

    byte[] bar = s.getBytes(); 
    int foo = 0; 
    for (int j=0; j<bar.length; j++) {
      int k = bar[j];
      if (k < '0' || k > '7') {
        foo = -1; break;
      }
      foo <<= 3;
      foo |= (k-'0');
    }
    return foo; 
  }

  public boolean setReadOnly(JFMFile file) throws IOException {
    try {
      synchronized (lockFs) {
        int mode = getMode("444"); 
        if (mode > 0) {
          getFs().chmod(mode, file.getAbsolutePath()); 
          return true; 
        }
      }
    } catch (Exception ex) {
      printStackTrace(ex); 
      if (ex instanceof IOException) 
        throw (IOException)ex; 
      else
        throw new IOException(ex.toString()); 
    }
    return false; 
  }

  public boolean canRead(JFMFile file) {
    try {
      return ((JFMSftpFile)file).canRead(); 
    } catch (Exception e) {
      printStackTrace(e); 
      return false; 
    }
  }

  public boolean canWrite(JFMFile file) {
    try {
      return ((JFMSftpFile)file).canWrite(); 
    } catch (Exception e) {
      printStackTrace(e); 
      return false; 
    }
  }

  public boolean exists(JFMFile file) {
    return ((JFMSftpFile)file).exists(); 
  }

  public JFMFile getStartDirectory(){
    return getDefaultRootDirectory(); 
  }

  public JFMFile getFile(String pathName) {
    return new JFMSftpFile(this, pathName);
  }

  @Override
  public boolean mkdir(JFMFile parent, String name) throws IOException {
    if (parent.isDirectory()) 
      return mkdir(getFile(parent.getAbsolutePath() + "/" + name)); 
    else
      return false; 
  }

  @Override
  public boolean createNewFile(JFMFile parent, String name) throws IOException {
    if (parent.isDirectory()) 
      return createNewFile(getFile(parent.getAbsolutePath() + "/" + name)); 
    else
      return false; 
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  private void printStackTrace(Exception e) {
    if (e != null && !(e instanceof FileNotFoundException))
      e.printStackTrace(); 
  }

}
