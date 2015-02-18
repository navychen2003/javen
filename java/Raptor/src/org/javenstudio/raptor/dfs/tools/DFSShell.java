package org.javenstudio.raptor.dfs.tools;

import java.io.IOException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.fs.FileSystem; 
import org.javenstudio.raptor.fs.FsShell; 
import org.javenstudio.raptor.dfs.server.namenode.NameNode; 
import org.javenstudio.raptor.util.StringUtils;


/** Provide command line access to a DFS FileSystem. */
public class DFSShell extends FsShell {
  public static final Logger LOG = Logger.getLogger(DFSShell.class);

  /**
   */
  public DFSShell() {
    this(null);
  }

  public DFSShell(Configuration conf) {
    super(conf);
  }
  
  protected void initNamesystem(Configuration conf) {
    initDefaultUri(conf); 
  }

  public static void initDefaultUri(Configuration conf) {
    initDefaultUri(conf, 10, false); 
  }

  public static void initDefaultUri(Configuration conf, boolean force) {
    initDefaultUri(conf, 0, force); 
  }

  private static void initDefaultUri(Configuration conf, int trytimes, boolean force) {
    try {
      initDefaultUri0(conf, trytimes, force); 
    } catch (IOException e) {
      LOG.error(StringUtils.stringifyException(e)); 
      System.exit(-1);
    }
  }

  private static void initDefaultUri0(Configuration conf, int trytimes, boolean force) throws IOException {
    String defaultUri = conf.get(FileSystem.getDefaultUriKey()); 
    if (defaultUri == null || defaultUri.length() == 0 || force) {
      String dfsAddressPort = null;
      //if (!ClusterHelper.isSingledMode(conf)) {
      //  String clusterAddress = ClusterHelper.getClusterAddressPort(conf);
      //  if (clusterAddress != null && clusterAddress.length() > 0) {
      //    ClusterManagedClient client = ClusterManagedClient.get(conf, clusterAddress);
      //    int tryTimes = trytimes, triedTimes = 0;
      //    while (trytimes <= 0 || (tryTimes--) > 0) {
      //      dfsAddressPort = NameNode.getAddressPort(client);
      //      if (dfsAddressPort != null)
      //        break;
      //      LOG.info("Waitting for namenode started, tried "+(++triedTimes)+" times.");
      //      try { Thread.sleep(5 * 1000); } catch (Exception e) {}
      //    }
      //  } 
      //} 
      if (dfsAddressPort == null || dfsAddressPort.length() == 0) 
        dfsAddressPort = NameNode.getAddressPort(conf); 
      if (dfsAddressPort == null || dfsAddressPort.length() == 0) 
        throw new IOException("'" + FileSystem.getDefaultUriKey() + "' not configured or namenode not started.");
      FileSystem.setDefaultUri(conf, "dfs://" + dfsAddressPort);
    }
  }

  /**
   * main() has some simple utility methods
   */
  public static void main(String argv[]) throws Exception {
    Configuration conf = ConfigurationFactory.create(QUIET);
    doMain(conf, argv);
  }
  
  public static void doMain(Configuration conf, String argv[]) throws Exception {
    DFSShell shell = new DFSShell(conf);
    FsShell.doMain(shell, argv);
  }

}
