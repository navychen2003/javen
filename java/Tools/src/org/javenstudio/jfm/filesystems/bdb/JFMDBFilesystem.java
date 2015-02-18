package org.javenstudio.jfm.filesystems.bdb;

import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.javenstudio.common.util.Logger;
import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.datum.bdb.BdbAdmin;
import org.javenstudio.falcon.datum.bdb.BdbTable;
import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.filesystems.JFMFileSystem;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.po.FsLoginDialog;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;

public class JFMDBFilesystem extends JFMFileSystem {
  private static final Logger LOG = Logger.getLogger(JFMDBFilesystem.class);
  
  private boolean mConnected = false; 
  
  public JFMDBFilesystem() {
    super();
    initialize(); 
  }

  public boolean isConnected() { 
    return mConnected; 
  }

  private void initialize() {
    synchronized (sLock) {
      String message = null; 
      try {
        if (Class.forName("org.javenstudio.raptor.conf.Configuration") == null || 
            Class.forName("org.javenstudio.raptor.bigdb.client.DBAdmin") == null) {
          message = "bdb class: org.javenstudio.raptor.bigdb.client.DBAdmin can not found."; 
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
      
      if (sAdmin != null) {
        mConnected = true; 
        return; 
      } else if (sConf == null) { 
    	mConnected = false;
    	return;
      }
    }
    
    String prefUri = Options.getPreferences().get("JFM.bdb.default.name", "bdb://localhost"); 
    String defaultUri = sConf.get("bdb.default.name", prefUri); 
    String host = null; 
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
    if (port <= 0) port = BdbAdmin.DEFAULT_PORT; 
    if (host == null || host.length() == 0 || host.equals("0.0.0.0")) host = "localhost";
    FsLoginDialog d = FsLoginDialog.showDialog("bdb", host, port); 
    host = d.getHost(); port = d.getPort(); 
    if (!d.isCanceled() && host != null && host.length() > 0) {
      host = d.getHost();
      port = d.getPort();
      defaultUri = "bdb://" + host + ":" + port;
      sConf.set("bdb.default.name", defaultUri); 
      Options.getPreferences().put("JFM.bdb.default.name", defaultUri); 
      System.setProperty("paxos.server.host", host);
      System.setProperty("paxos.server.port", ""+port);
    } else {
      sConf.set("bdb.default.name", ""); 
      return; 
    }
    
    synchronized (sLock) {
      Options.showStatus(Strings.format("connecting to %1$s", defaultUri)); 
      try { 
    	BdbAdmin admin = new BdbAdmin(sConf);
    	Options.showStatus(null); 
    	sAdmin = admin;
    	sConnectUri = defaultUri;
    	sConnectHost = host;
    	sConnectPort = port;
    	Options.addHistoryUri("bdb", host, port, "root");
    	mConnected = true;
    	loadTables();
      } catch (Throwable e) { 
    	sAdmin = null;
    	sConnectUri = null;
    	sConnectHost = null;
    	sConnectPort = 0;
      	mConnected = false;
      	
      	if (LOG.isWarnEnabled())
      	  LOG.warn("initialize: connect to bdb error: " + e, e);
      	
        String message = Strings.format("Connect to bdb database (%1$s) failed", host); 
        message += "\n\n  " + e.getMessage(); 
        JOptionPane.showMessageDialog(Options.getMainFrame(), 
        	message, Strings.get("Connect Error"), JOptionPane.ERROR_MESSAGE);
      }
    }
  }
  
  private static void initStatic() {
    synchronized (sLock) {
      if (sConf == null) sConf = ConfigurationFactory.get(); 
    }
  }
  
  private static Configuration sConf = null; 
  private static BdbAdmin sAdmin = null;
  private static String sConnectUri = null;
  @SuppressWarnings("unused")
  private static String sConnectHost = null;
  @SuppressWarnings("unused")
  private static int sConnectPort = 0;
  private static Object sLock = new Object();

  public static boolean setConnectUri(URI uri) {
    if (uri == null || !"bdb".equals(uri.getScheme()))
        return false;

    initStatic(); 
	
    String prefUri = Options.getPreferences().get("JFM.bdb.default.name", "bdb://localhost"); 
    String defaultUri = sConf.get("bdb.default.name", prefUri); 
    String host = null; 
    int port = 0; 
    if (defaultUri != null && defaultUri.length() > 0) {
      try {
        URI uri2 = new URI(defaultUri); 
        host = uri2.getHost(); 
        port = uri2.getPort(); 
      } catch (Exception e) {
        host = null; 
      }
    }
    if (port <= 0) port = BdbAdmin.DEFAULT_PORT; 
    if (host == null || host.length() == 0 || host.equals("0.0.0.0")) host = "localhost";
    host = uri.getHost(); port = uri.getPort(); 
    if (host != null && host.length() > 0) {
      defaultUri = "bdb://" + uri.getHost() + ":" + uri.getPort();
      sConf.set("bdb.default.name", defaultUri); 
      Options.getPreferences().put("JFM.bdb.default.name", defaultUri); 
      System.setProperty("paxos.server.host", host);
      System.setProperty("paxos.server.port", ""+port);
    }
    
	return true;
  }
  
  public static int tryDisconnect() {
	synchronized (sLock) {
	  if (sAdmin != null) { 
		//sAdmin.close();
	  }
	}
	return 0;
  }
  
  public static String getConnectName() { 
	synchronized (sLock) {
	  if (sAdmin != null && sConnectUri != null) { 
		return Strings.format("Disconnect from %1$s", sConnectUri);
	  }
	}
	return null;
  }
  
  public static String getLongName() { 
	synchronized (sLock) {
	  if (sAdmin != null && sConnectUri != null) { 
		return sConnectUri + " - root";
	  }
	}
	return "bdb (" + Strings.get("not connected") + ")"; 
  }
  
  public static String getIconName() {
    return "/images/icons/gnome-fs-nfs.png"; 
  }
  
  public String getDefaultUri() { return sConnectUri; }
  public String getScheme() { return "bdb"; }
  public String getSchemeAuthority() { return sConnectUri; }
  
  public static BdbAdmin getAdmin() { 
	synchronized (sLock) {
	  return sAdmin;
	}
  }
  
  @Override
  public JFMFile getDefaultRootDirectory() {
	return null;
  }

  @Override
  public String getName() {
	return sConnectUri;
  }

  @Override
  public char getPathSeparator() {
	return 0;
  }

  @Override
  public char getSeparator() {
	return 0;
  }

  private JFMDBTable[] mTables = null;
  
  private void loadTables() { 
	synchronized (sLock) {
	  ArrayList<JFMDBTable> list = new ArrayList<JFMDBTable>();
      try {
	    BdbTable[] tables = getAdmin().getTables();
	    
	    for (int i=0; tables != null && i < tables.length; i++) { 
	    	BdbTable table = tables[i];
		  if (table != null) 
		    list.add(new JFMDBTable(this, table));
	    }
	  } catch (Throwable e) { 
	    if (LOG.isErrorEnabled())
	      LOG.error("listRoots: error: " + e, e);
	  }
      if (list.size() == 0)
    	list.add(new JFMDBTable(this, null));
      mTables = list.toArray(new JFMDBTable[list.size()]);
	}
  }
  
  @Override
  public JFMDBTable[] listRoots() {
	loadTables();
	return mTables;
  }

  @Override
  public JFMDBFile[] listFiles(JFMFile rootFile) throws IOException {
	return null;
  }

  @Override
  public JFMDBFile[] listFiles(JFMFile rootFile, FilenameFilter filter)
		throws IOException {
	return null;
  }

  @Override
  public boolean mkdir(JFMFile parent, String name) throws IOException {
	return false;
  }

  @Override
  public boolean createNewFile(JFMFile parent, String name) throws IOException {
	return false;
  }

  @Override
  public boolean setLastModified(JFMFile file, long time) throws IOException {
	return false;
  }

  @Override
  public boolean setReadOnly(JFMFile file) throws IOException {
	return false;
  }

  @Override
  public JFMDBTable getStartDirectory() {
	loadTables();
	JFMDBTable[] tables = mTables;
	return tables != null && tables.length > 0 ? tables[0] : null;
  }

  @Override
  public JFMDBFile getFile(String pathName) {
	return null;
  } 
  
}
