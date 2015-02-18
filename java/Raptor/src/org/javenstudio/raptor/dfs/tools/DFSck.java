package org.javenstudio.raptor.dfs.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.dfs.DistributedFileSystem;
import org.javenstudio.raptor.dfs.server.namenode.NamenodeFsck;
import org.javenstudio.raptor.fs.FsShell;
import org.javenstudio.raptor.ipc.RPC;
import org.javenstudio.raptor.net.NetUtils;
import org.javenstudio.raptor.util.StringUtils; 
import org.javenstudio.raptor.util.ToolRunner;

/**
 * This class provides rudimentary checking of DFS volumes for errors and
 * sub-optimal conditions.
 * <p>The tool scans all files and directories, starting from an indicated
 *  root path. The following abnormal conditions are detected and handled:</p>
 * <ul>
 * <li>files with blocks that are completely missing from all datanodes.<br/>
 * In this case the tool can perform one of the following actions:
 *  <ul>
 *      <li>none ({@link org.javenstudio.raptor.dfs.server.namenode.NamenodeFsck#FIXING_NONE})</li>
 *      <li>move corrupted files to /lost+found directory on DFS
 *      ({@link org.javenstudio.raptor.dfs.server.namenode.NamenodeFsck#FIXING_MOVE}). Remaining data blocks are saved as a
 *      block chains, representing longest consecutive series of valid blocks.</li>
 *      <li>delete corrupted files ({@link org.javenstudio.raptor.dfs.server.namenode.NamenodeFsck#FIXING_DELETE})</li>
 *  </ul>
 *  </li>
 *  <li>detect files with under-replicated or over-replicated blocks</li>
 *  </ul>
 *  Additionally, the tool collects a detailed overall DFS statistics, and
 *  optionally can print detailed statistics on block locations and replication
 *  factors of each file.
 *  The tool also provides and option to filter open files during the scan.
 *  
 */
public class DFSck extends FsShell {

  DFSck() {}
  
  /**
   * Filesystem checker.
   * @param conf current Configuration
   * @throws Exception
   */
  public DFSck(Configuration conf) throws Exception {
    super(conf);
  }
  
  @SuppressWarnings("deprecation")
  private String getInfoServer() throws IOException {
    DFSShell.initDefaultUri(getConf(), true); 
    return NetUtils.getServerAddress(getConf(), "dfs.info.bindAddress", "dfs.info.port", 
                                     "dfs.http.address");
  }
  
  /**
   * Print fsck usage information
   */
  static void printUsage() {
    System.err.println("Usage: DFSck <path> [-move | -delete | -openforwrite] [-files [-blocks [-locations | -racks]]]");
    System.err.println(" <path>           start checking from this path");
    System.err.println(" -move            move corrupted files to /lost+found");
    System.err.println(" -delete          delete corrupted files");
    System.err.println(" -files           print out files being checked");
    System.err.println(" -openforwrite    print out files opened for write");
    System.err.println(" -blocks          print out block report");
    System.err.println(" -locations       print out locations for every block");
    System.err.println(" -racks           print out network topology for data-node locations");
    System.err.println("                  By default fsck ignores files opened for write, \n" +
                       "                  use -openforwrite to report such files. They are usually \n" +
                       "                  tagged CORRUPT or HEALTHY depending on their block \n" +
                       "                  allocation status\n");
    //ToolRunner.printGenericCommandUsage(System.err);
  }
  /**
   * @param args
   */
  public int run(String[] args) throws Exception {
    if (args.length == 0) {
      printUsage(); return -1;
    }
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-help")) { 
        printUsage(); return -1;
      }
    }
    String fsName = getInfoServer();
    if (fsName == null) { 
      //throw new NullPointerException("namenode info server not configured"); 
      return runClient(args);
    }
    StringBuffer url = new StringBuffer("http://"+fsName+"/fsck?path=");
    String dir = "/";
    // find top-level dir first
    for (int idx = 0; idx < args.length; idx++) {
      if (!args[idx].startsWith("-")) { dir = args[idx]; break; }
    }
    url.append(URLEncoder.encode(dir, StringUtils.getCharacterEncoding()));
    for (int idx = 0; idx < args.length; idx++) {
      if (args[idx].equals("-move")) { url.append("&move=1"); }
      else if (args[idx].equals("-delete")) { url.append("&delete=1"); }
      else if (args[idx].equals("-files")) { url.append("&files=1"); }
      else if (args[idx].equals("-openforwrite")) { url.append("&openforwrite=1"); }
      else if (args[idx].equals("-blocks")) { url.append("&blocks=1"); }
      else if (args[idx].equals("-locations")) { url.append("&locations=1"); }
      else if (args[idx].equals("-racks")) { url.append("&racks=1"); }
    }
    URL path = new URL(url.toString());
    URLConnection connection = path.openConnection();
    InputStream stream = connection.getInputStream();
    BufferedReader input = new BufferedReader(new InputStreamReader(
                                              stream, StringUtils.getCharacterEncoding()));
    String line = null;
    String lastLine = null;
    int errCode = -1;
    try {
      while ((line = input.readLine()) != null) {
        System.out.println(line);
        lastLine = line;
      }
    } finally {
      input.close();
    }
    if (lastLine.endsWith(NamenodeFsck.HEALTHY_STATUS)) {
      errCode = 0;
    } else if (lastLine.endsWith(NamenodeFsck.CORRUPT_STATUS)) {
      errCode = 1;
    } else if (lastLine.endsWith(NamenodeFsck.NONEXISTENT_STATUS)) {
      errCode = 0;
    }
    return errCode;
  }

  @Override
  protected void initNamesystem(Configuration conf) {
    DFSShell.initDefaultUri(conf);
  }
  
  private int runClient(String[] args) throws Exception { 
	int exitCode = -1;
    try {
      init();
    } catch (RPC.VersionMismatch v) {
      System.err.println("Version Mismatch between client and server"
                         + "... command aborted.");
      return exitCode;
    } catch (IOException e) {
      System.err.println("Bad connection to DFS... command aborted.");
      return exitCode;
    }

    exitCode = 0;
    
    String dir = "/";
    StringBuilder params = new StringBuilder();
    // find top-level dir first
    for (int idx = 0; idx < args.length; idx++) {
      if (!args[idx].startsWith("-")) { dir = args[idx]; break; }
    }
    for (int idx = 0; idx < args.length; idx++) {
      if (args[idx].equals("-move")) { params.append("&move=1"); }
      else if (args[idx].equals("-delete")) { params.append("&delete=1"); }
      else if (args[idx].equals("-files")) { params.append("&files=1"); }
      else if (args[idx].equals("-openforwrite")) { params.append("&openforwrite=1"); }
      else if (args[idx].equals("-blocks")) { params.append("&blocks=1"); }
      else if (args[idx].equals("-locations")) { params.append("&locations=1"); }
      else if (args[idx].equals("-racks")) { params.append("&racks=1"); }
    }
    
    if (fs instanceof DistributedFileSystem) {
      DistributedFileSystem dfs = (DistributedFileSystem) fs;
      System.out.println(dfs.fsck(dir, params.toString()));
    }
    
	return exitCode;
  }
  
  static{
    //Configuration.addDefaultResource("hdfs-default.xml");
    //Configuration.addDefaultResource("hdfs-site.xml");
  }
  
  public static void main(String[] args) throws Exception {
	doMain(ConfigurationFactory.create(true), args);
  }
  
  public static void doMain(Configuration conf, String[] args) throws Exception {
    // -files option is also used by GenericOptionsParser
    // Make sure that is not the first argument for fsck
    int res = -1;
    if ((args.length == 0 ) || ("-files".equals(args[0]))) 
      printUsage();
    else
      res = ToolRunner.run(new DFSck(conf), args);
    System.exit(res);
  }
}

