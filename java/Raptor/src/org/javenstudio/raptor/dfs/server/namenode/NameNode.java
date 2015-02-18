package org.javenstudio.raptor.dfs.server.namenode;

import java.io.*;
import java.net.*;
import java.util.Collection;
import java.util.Iterator;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.fs.ContentSummary;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.fs.Trash;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.permission.FsPermission;
import org.javenstudio.raptor.fs.permission.PermissionStatus;
import org.javenstudio.raptor.dfs.DFSPolicyProvider;
import org.javenstudio.raptor.dfs.protocol.*;
import org.javenstudio.raptor.dfs.server.common.DfsConstants.StartupOption;
import org.javenstudio.raptor.dfs.server.common.IncorrectVersionException;
import org.javenstudio.raptor.dfs.server.common.StartupOptions;
import org.javenstudio.raptor.dfs.server.common.UpgradeStatusReport;
import org.javenstudio.raptor.dfs.server.namenode.FSNamesystem.CompleteFileStatus;
import org.javenstudio.raptor.dfs.server.namenode.metrics.NameNodeMetrics;
import org.javenstudio.raptor.dfs.server.protocol.BlocksWithLocations;
import org.javenstudio.raptor.dfs.server.protocol.DatanodeCommand;
import org.javenstudio.raptor.dfs.server.protocol.DatanodeProtocol;
import org.javenstudio.raptor.dfs.server.protocol.DatanodeRegistration;
import org.javenstudio.raptor.dfs.server.protocol.NamenodeProtocol;
import org.javenstudio.raptor.dfs.server.protocol.NamespaceInfo;
import org.javenstudio.raptor.dfs.server.protocol.UpgradeCommand;
import org.javenstudio.raptor.ipc.*;
import org.javenstudio.raptor.util.ReflectionUtils;
import org.javenstudio.raptor.util.StringUtils;
import org.javenstudio.raptor.net.NetUtils;
import org.javenstudio.raptor.net.NetworkTopology;
import org.javenstudio.raptor.security.SecurityUtil;
import org.javenstudio.raptor.security.UserGroupInformation;
import org.javenstudio.raptor.security.authorize.AuthorizationException;
import org.javenstudio.raptor.security.authorize.ConfiguredPolicy;
import org.javenstudio.raptor.security.authorize.PolicyProvider;
import org.javenstudio.raptor.security.authorize.RefreshAuthorizationPolicyProtocol;
import org.javenstudio.raptor.security.authorize.ServiceAuthorizationManager;


/**********************************************************
 * NameNode serves as both directory namespace manager and
 * "inode table" for the Hadoop DFS.  There is a single NameNode
 * running in any DFS deployment.  (Well, except when there
 * is a second backup/failover NameNode.)
 *
 * The NameNode controls two critical tables:
 *   1)  filename->blocksequence (namespace)
 *   2)  block->machinelist ("inodes")
 *
 * The first table is stored on disk and is very precious.
 * The second table is rebuilt every time the NameNode comes
 * up.
 *
 * 'NameNode' refers to both this class as well as the 'NameNode server'.
 * The 'FSNamesystem' class actually performs most of the filesystem
 * management.  The majority of the 'NameNode' class itself is concerned
 * with exposing the IPC interface and the http server to the outside world,
 * plus some configuration management.
 *
 * NameNode implements the ClientProtocol interface, which allows
 * clients to ask for DFS services.  ClientProtocol is not
 * designed for direct use by authors of DFS client code.  End-users
 * should instead use the org.javenstudio.raptor.fs.FileSystem class.
 *
 * NameNode also implements the DatanodeProtocol interface, used by
 * DataNode programs that actually store DFS data blocks.  These
 * methods are invoked repeatedly and automatically by all the
 * DataNodes in a DFS deployment.
 *
 * NameNode also implements the NamenodeProtocol interface, used by
 * secondary namenodes or rebalancing processes to get partial namenode's
 * state, for example partial blocksMap etc.
 **********************************************************/
public class NameNode implements ClientProtocol, DatanodeProtocol,
                                 NamenodeProtocol, FSConstants,
                                 RefreshAuthorizationPolicyProtocol {

  public static final Logger LOG = Logger.getLogger(NameNode.class);
  public static final Logger stateChangeLog = LOG; //Logger.getLogger(StateChange.class);

  public static final int DEFAULT_PORT = 10090; 
  public static final int DEFAULT_HTTPPORT = 10070; 
  static{
    //Configuration.addDefaultResource("dfs-default.xml");
    //Configuration.addDefaultResource("dfs-site.xml");
  }
  
  public long getProtocolVersion(String protocol, 
                                 long clientVersion) throws IOException { 
    if (protocol.equals(ClientProtocol.class.getName())) {
      return ClientProtocol.versionID; 
    } else if (protocol.equals(DatanodeProtocol.class.getName())){
      return DatanodeProtocol.versionID;
    } else if (protocol.equals(NamenodeProtocol.class.getName())){
      return NamenodeProtocol.versionID;
    } else if (protocol.equals(RefreshAuthorizationPolicyProtocol.class.getName())){
      return RefreshAuthorizationPolicyProtocol.versionID;
    } else {
      throw new IOException("Unknown protocol to name node: " + protocol);
    }
  }

  private Configuration conf;
  private FSNamesystem namesystem; // TODO: This should private. Use getNamesystem() instead. 
  /** RPC server */
  private Server server;
  /** RPC server address */
  private InetSocketAddress serverAddress = null;
  /** httpServer */
  //private StatusHttpServer httpServer;
  /** HTTP server address */
  private InetSocketAddress httpAddress = null;
  private Thread emptier;
  /** only used for testing purposes  */
  private boolean stopRequested = false;
  /** Is service level authorization enabled? */
  private boolean serviceAuthEnabled = false;

  //private ServiceClient client = null;
  //private ServiceInfo serviceInfo = null; 
  private long startTime = 0; 
  
  /** Format a new filesystem.  Destroys any filesystem that may already
   * exist at this location.  **/
  public static void format(Configuration conf) throws IOException {
    format(conf, false);
  }

  static NameNodeMetrics myMetrics;

  public FSNamesystem getNamesystem() {
    return namesystem;
  }

  public static NameNodeMetrics getNameNodeMetrics() {
    return myMetrics;
  }
  
  public static InetSocketAddress getAddress(String address) {
    return NetUtils.createSocketAddr(address, DEFAULT_PORT);
  }

  public static InetSocketAddress getAddress(Configuration conf) {
    return getAddress(FileSystem.getDefaultUri(conf).getAuthority());
  }

  public static URI getUri(InetSocketAddress namenode) {
    int port = namenode.getPort();
    String portString = port == DEFAULT_PORT ? "" : (":"+port);
    return URI.create("dfs://"+ namenode.getHostName()+portString);
  }

  public static String getAddressPort(InetSocketAddress namenode) {
    int port = namenode.getPort();
    String portString = port == DEFAULT_PORT ? "" : (":"+port);
    return namenode.getHostName() + portString;
  }
  
  public Configuration getConf() { return this.conf; }
  
  /**
   * Initialize name-node.
   * 
   * @param conf the configuration
   */
  private void initialize(Configuration conf) throws IOException {
	this.conf = conf;
	
    InetSocketAddress socAddr = NameNode.getAddress(conf);
    int handlerCount = conf.getInt("dfs.namenode.handler.count", 10);
    
    // set service-level authorization security policy
    if (serviceAuthEnabled = 
          conf.getBoolean(
            ServiceAuthorizationManager.SERVICE_AUTHORIZATION_CONFIG, false)) {
      PolicyProvider policyProvider = 
        (PolicyProvider)(ReflectionUtils.newInstance(
            conf.getClass(PolicyProvider.POLICY_PROVIDER_CONFIG, 
                DFSPolicyProvider.class, PolicyProvider.class), 
            conf));
      SecurityUtil.setPolicy(new ConfiguredPolicy(conf, policyProvider));
    }

    // create rpc server 
    this.server = RPC.getServer(this, socAddr.getHostName(), socAddr.getPort(),
                                handlerCount, false, conf);

    // The rpc-server port can be ephemeral... ensure we have the correct info
    this.serverAddress = this.server.getListenerAddress(); 
    //FileSystem.setDefaultUri(conf, getUri(serverAddress));
    LOG.info("Namenode up at: " + this.serverAddress);

    myMetrics = new NameNodeMetrics(conf, this);
    this.namesystem = new FSNamesystem(this, conf);
    //this.serviceInfo = ServiceInfo.create(this, socAddr.getPort(), conf); 
    //this.serviceInfo.addServer(this.getClass().getSimpleName(), 
    //    serverAddress.getAddress().getHostAddress(), serverAddress.getPort(), handlerCount, 
    //    new String[]{NamenodeProtocol.class.getName(), 
    //                 DatanodeProtocol.class.getName(), 
    //                 ClientProtocol.class.getName(), 
    //                 RefreshAuthorizationPolicyProtocol.class.getName()}); 

    //startHttpServer(conf);
    this.server.start();  //start RPC server   
    startTrashEmptier(conf);

    this.startTime = System.currentTimeMillis(); 
    initServiceClient(conf); 
  }

  private void startTrashEmptier(Configuration conf) throws IOException {
    this.emptier = new Thread(new Trash(conf).getEmptier(), "Trash Emptier");
    this.emptier.setDaemon(true);
    this.emptier.start();
  }
/*
  private void startHttpServer(Configuration conf) throws IOException {
    String infoAddr = 
      NetUtils.getServerAddress(conf, "dfs.info.bindAddress", "dfs.info.port", "dfs.http.address");
    InetSocketAddress infoSocAddr = NetUtils.createSocketAddr(infoAddr);
    String infoHost = infoSocAddr.getHostName();
    int infoPort = infoSocAddr.getPort();
    this.httpServer = StatusHttpServer.createNamenodeInfoServer(conf, infoHost, infoPort);
    if (conf.getBoolean("dfs.https.enable", false)) {
      boolean needClientAuth = conf.getBoolean("dfs.https.need.client.auth", false);
      InetSocketAddress secInfoSocAddr = NetUtils.createSocketAddr(conf.get(
          "dfs.https.address", infoHost + ":" + 0));
      Configuration sslConf = ConfigurationFactory.create(conf);
      sslConf.addResource(conf.get("dfs.https.server.keystore.resource",
          "ssl-server.xml"));
      this.httpServer.addSslListener(secInfoSocAddr, sslConf, needClientAuth);
      // assume same ssl port for all datanodes
      InetSocketAddress datanodeSslPort = NetUtils.createSocketAddr(conf.get(
          "dfs.datanode.https.address", infoHost + ":" + 50475));
      this.httpServer.setAttribute("datanode.https.port", datanodeSslPort
          .getPort());
    }
    this.httpServer.setAttribute("name.node", this);
    this.httpServer.setAttribute("name.node.address", getNameNodeAddress());
    this.httpServer.setAttribute("name.system.image", getFSImage());
    this.httpServer.setAttribute("name.conf", conf);
    this.httpServer.addInternalServlet("fsck", "/fsck", FsckServlet.class);
    this.httpServer.addInternalServlet("getimage", "/getimage", GetImageServlet.class);
    this.httpServer.addInternalServlet("listPaths", "/listPaths/*", ListPathsServlet.class);
    this.httpServer.addInternalServlet("data", "/data/*", FileDataServlet.class);
    this.httpServer.addInternalServlet("checksum", "/fileChecksum/*",
        FileChecksumServlets.RedirectServlet.class);
    this.httpServer.start();

    // The web-server port can be ephemeral... ensure we have the correct info
    infoPort = this.httpServer.getPort();
    this.httpAddress = new InetSocketAddress(infoHost, infoPort);
    conf.set("dfs.http.address", infoHost + ":" + infoPort);
    LOG.info("Web-server up at: " + this.httpAddress);

    this.serviceInfo.addServer(ServerInfo.Protocol.HTTP, 
        serverAddress.getAddress().getHostAddress(), infoPort); 
  }*/

  /**
   * Start NameNode.
   * <p>
   * The name-node can be started with one of the following startup options:
   * <ul> 
   * <li>{@link StartupOption#REGULAR REGULAR} - normal name node startup</li>
   * <li>{@link StartupOption#FORMAT FORMAT} - format name node</li>
   * <li>{@link StartupOption#UPGRADE UPGRADE} - start the cluster  
   * upgrade and create a snapshot of the current file system state</li> 
   * <li>{@link StartupOption#ROLLBACK ROLLBACK} - roll the  
   *            cluster back to the previous state</li>
   * </ul>
   * The option is passed via configuration field: 
   * <tt>dfs.namenode.startup</tt>
   * 
   * The conf will be modified to reflect the actual ports on which 
   * the NameNode is up and running if the user passes the port as
   * <code>zero</code> in the conf.
   * 
   * @param conf  confirguration
   * @throws IOException
   */
  public NameNode(Configuration conf) throws IOException {
    try {
      initialize(conf);
    } catch (IOException e) {
      this.stop();
      throw e;
    }
  }

  /**
   * Wait for service to finish.
   * (Normally, it runs forever.)
   */
  public void join() {
    try {
      this.server.join();
    } catch (InterruptedException ie) {
    }
  }

  /**
   * Stop all NameNode threads and wait for all to finish.
   */
  public void stop() {
    if (stopRequested)
      return;
    stopRequested = true;
    //try {
    //  if (httpServer != null) httpServer.stop();
    //} catch (Exception e) {
    //  LOG.error(StringUtils.stringifyException(e));
    //}
    if(namesystem != null) namesystem.close();
    if(emptier != null) emptier.interrupt();
    if(server != null) server.stop();
    if (myMetrics != null) {
      myMetrics.shutdown();
    }
    if (namesystem != null) {
      namesystem.shutdown();
    }
  }
  
  /////////////////////////////////////////////////////
  // NamenodeProtocol
  /////////////////////////////////////////////////////
  /**
   * return a list of blocks & their locations on <code>datanode</code> whose
   * total size is <code>size</code>
   * 
   * @param datanode on which blocks are located
   * @param size total size of blocks
   */
  public BlocksWithLocations getBlocks(DatanodeInfo datanode, long size)
      throws IOException {
    if(size <= 0) {
      throw new IllegalArgumentException(
        "Unexpected not positive size: "+size);
    }

    return namesystem.getBlocks(datanode, size); 
  }
  
  /////////////////////////////////////////////////////
  // ClientProtocol
  /////////////////////////////////////////////////////
  /** {@inheritDoc} */
  public LocatedBlocks   getBlockLocations(String src, 
                                          long offset, 
                                          long length) throws IOException {
    myMetrics.numGetBlockLocations.inc();
    return namesystem.getBlockLocations(getClientMachine(), 
                                        src, offset, length);
  }
  
  private static String getClientMachine() {
    String clientMachine = Server.getRemoteAddress();
    if (clientMachine == null) {
      clientMachine = "";
    }
    return clientMachine;
  }

  /** {@inheritDoc} */
  public void create(String src, 
                     FsPermission masked,
                   	 String clientName, 
                 	 boolean overwrite,
                   	 short replication,
                     long blockSize
                    ) throws IOException {
    String clientMachine = getClientMachine();
    if (stateChangeLog.isDebugEnabled()) {
      stateChangeLog.debug("*DIR* NameNode.create: file "
                         +src+" for "+clientName+" at "+clientMachine);
    }
    if (!checkPathLength(src)) {
      throw new IOException("create: Pathname too long.  Limit " 
                            + MAX_PATH_LENGTH + " characters, " + MAX_PATH_DEPTH + " levels.");
    }
    namesystem.startFile(src,
        new PermissionStatus(UserGroupInformation.getCurrentUGI().getUserName(),
            null, masked),
        clientName, clientMachine, overwrite, replication, blockSize);
    myMetrics.numFilesCreated.inc();
    myMetrics.numCreateFileOps.inc();
  }

  /** {@inheritDoc} */
  public LocatedBlock append(String src, String clientName) throws IOException {
    String clientMachine = getClientMachine();
    if (stateChangeLog.isDebugEnabled()) {
      stateChangeLog.debug("*DIR* NameNode.append: file "
          +src+" for "+clientName+" at "+clientMachine);
    }
    LocatedBlock info = namesystem.appendFile(src, clientName, clientMachine);
    myMetrics.numFilesAppended.inc();
    return info;
  }

  /** {@inheritDoc} */
  public boolean setReplication(String src, 
                                short replication
                                ) throws IOException {
    return namesystem.setReplication(src, replication);
  }
    
  /** {@inheritDoc} */
  public void setPermission(String src, FsPermission permissions
      ) throws IOException {
    namesystem.setPermission(src, permissions);
  }

  /** {@inheritDoc} */
  public void setOwner(String src, String username, String groupname
      ) throws IOException {
    namesystem.setOwner(src, username, groupname);
  }

  /**
   */
  public LocatedBlock addBlock(String src, 
                               String clientName) throws IOException {
    stateChangeLog.debug("*BLOCK* NameNode.addBlock: file "
                         +src+" for "+clientName);
    LocatedBlock locatedBlock = namesystem.getAdditionalBlock(src, clientName);
    if (locatedBlock != null)
      myMetrics.numAddBlockOps.inc();
    return locatedBlock;
  }

  /**
   * The client needs to give up on the block.
   */
  public void abandonBlock(Block b, String src, String holder
      ) throws IOException {
    stateChangeLog.debug("*BLOCK* NameNode.abandonBlock: "
                         +b+" of file "+src);
    if (!namesystem.abandonBlock(b, src, holder)) {
      throw new IOException("Cannot abandon block during write to " + src);
    }
  }

  /** {@inheritDoc} */
  public boolean complete(String src, String clientName) throws IOException {
    stateChangeLog.debug("*DIR* NameNode.complete: " + src + " for " + clientName);
    CompleteFileStatus returnCode = namesystem.completeFile(src, clientName);
    if (returnCode == CompleteFileStatus.STILL_WAITING) {
      return false;
    } else if (returnCode == CompleteFileStatus.COMPLETE_SUCCESS) {
      return true;
    } else {
      throw new IOException("Could not complete write to file " + src + " by " + clientName);
    }
  }

  /**
   * The client has detected an error on the specified located blocks 
   * and is reporting them to the server.  For now, the namenode will 
   * mark the block as corrupt.  In the future we might 
   * check the blocks are actually corrupt. 
   */
  public void reportBadBlocks(LocatedBlock[] blocks) throws IOException {
    stateChangeLog.info("*DIR* NameNode.reportBadBlocks");
    for (int i = 0; i < blocks.length; i++) {
      Block blk = blocks[i].getBlock();
      DatanodeInfo[] nodes = blocks[i].getLocations();
      for (int j = 0; j < nodes.length; j++) {
        DatanodeInfo dn = nodes[j];
        namesystem.markBlockAsCorrupt(blk, dn);
      }
    }
  }

  /** {@inheritDoc} */
  public long nextGenerationStamp(Block block) throws IOException{
    return namesystem.nextGenerationStampForBlock(block);
  }

  /** {@inheritDoc} */
  public void commitBlockSynchronization(Block block,
      long newgenerationstamp, long newlength,
      boolean closeFile, boolean deleteblock, DatanodeID[] newtargets
      ) throws IOException {
    namesystem.commitBlockSynchronization(block,
        newgenerationstamp, newlength, closeFile, deleteblock, newtargets);
  }
  
  public long getPreferredBlockSize(String filename) throws IOException {
    return namesystem.getPreferredBlockSize(filename);
  }
    
  /**
   */
  public boolean rename(String src, String dst) throws IOException {
    stateChangeLog.debug("*DIR* NameNode.rename: " + src + " to " + dst);
    if (!checkPathLength(dst)) {
      throw new IOException("rename: Pathname too long.  Limit " 
                            + MAX_PATH_LENGTH + " characters, " + MAX_PATH_DEPTH + " levels.");
    }
    boolean ret = namesystem.renameTo(src, dst);
    if (ret) {
      myMetrics.numFilesRenamed.inc();
    }
    return ret;
  }

  /**
   */
  @Deprecated
  public boolean delete(String src) throws IOException {
    return delete(src, true);
  }

  /** {@inheritDoc} */
  public boolean delete(String src, boolean recursive) throws IOException {
    if (stateChangeLog.isDebugEnabled()) {
      stateChangeLog.debug("*DIR* Namenode.delete: src=" + src
          + ", recursive=" + recursive);
    }
    boolean ret = namesystem.delete(src, recursive);
    if (ret) 
      myMetrics.numDeleteFileOps.inc();
    return ret;
  }

  /**
   * Check path length does not exceed maximum.  Returns true if
   * length and depth are okay.  Returns false if length is too long 
   * or depth is too great.
   * 
   */
  private boolean checkPathLength(String src) {
    Path srcPath = new Path(src);
    return (src.length() <= MAX_PATH_LENGTH &&
            srcPath.depth() <= MAX_PATH_DEPTH);
  }
    
  /** {@inheritDoc} */
  public boolean mkdirs(String src, FsPermission masked) throws IOException {
    stateChangeLog.debug("*DIR* NameNode.mkdirs: " + src);
    if (!checkPathLength(src)) {
      throw new IOException("mkdirs: Pathname too long.  Limit " 
                            + MAX_PATH_LENGTH + " characters, " + MAX_PATH_DEPTH + " levels.");
    }
    return namesystem.mkdirs(src,
        new PermissionStatus(UserGroupInformation.getCurrentUGI().getUserName(),
            null, masked));
  }

  /**
   */
  public void renewLease(String clientName) throws IOException {
    namesystem.renewLease(clientName);        
  }

  /**
   */
  public FileStatus[] getListing(String src) throws IOException {
    FileStatus[] files = namesystem.getListing(src);
    if (files != null) {
      myMetrics.numGetListingOps.inc();
    }
    return files;
  }

  /**
   * Get the file info for a specific file.
   * @param src The string representation of the path to the file
   * @throws IOException if permission to access file is denied by the system
   * @return object containing information regarding the file
   *         or null if file not found
   */
  public FileStatus getFileInfo(String src)  throws IOException {
    myMetrics.numFileInfoOps.inc();
    return namesystem.getFileInfo(src);
  }

  /** @inheritDoc */
  public long[] getStats() throws IOException {
    return namesystem.getStats();
  }

  /**
   */
  public DatanodeInfo[] getDatanodeReport(DatanodeReportType type)
      throws IOException {
    DatanodeInfo results[] = namesystem.datanodeReport(type);
    if (results == null ) {
      throw new IOException("Cannot find datanode report");
    }
    return results;
  }
    
  /**
   * @inheritDoc
   */
  public boolean setSafeMode(SafeModeAction action) throws IOException {
    return namesystem.setSafeMode(action);
  }

  /**
   * Is the cluster currently in safe mode?
   */
  public boolean isInSafeMode() {
    return namesystem.isInSafeMode();
  }

  /**
   * @inheritDoc
   */
  public void saveNamespace() throws IOException {
    namesystem.saveNamespace();
  }

  /**
   * Refresh the list of datanodes that the namenode should allow to  
   * connect.  Re-reads conf by creating new Configuration object and 
   * uses the files list in the configuration to update the list. 
   */
  public void refreshNodes() throws IOException {
    namesystem.refreshNodes(ConfigurationFactory.create());
  }

  /**
   * Returns the size of the current edit log.
   */
  public long getEditLogSize() throws IOException {
    return namesystem.getEditLogSize();
  }

  /**
   * Roll the edit log.
   */
  public CheckpointSignature rollEditLog() throws IOException {
    return namesystem.rollEditLog();
  }

  /**
   * Roll the image 
   */
  public void rollFsImage() throws IOException {
    namesystem.rollFSImage();
  }
    
  public void finalizeUpgrade() throws IOException {
    namesystem.finalizeUpgrade();
  }

  public UpgradeStatusReport distributedUpgradeProgress(UpgradeAction action
                                                        ) throws IOException {
    return namesystem.distributedUpgradeProgress(action);
  }

  /**
   * Dumps namenode state into specified file
   */
  public void metaSave(String filename) throws IOException {
    namesystem.metaSave(filename);
  }

  /** {@inheritDoc} */
  public ContentSummary getContentSummary(String path) throws IOException {
    return namesystem.getContentSummary(path);
  }

  /** {@inheritDoc} */
  public void setQuota(String path, long namespaceQuota, long diskspaceQuota) 
                       throws IOException {
    namesystem.setQuota(path, namespaceQuota, diskspaceQuota);
  }
  
  /** {@inheritDoc} */
  public void fsync(String src, String clientName) throws IOException {
    namesystem.fsync(src, clientName);
  }

  /** @inheritDoc */
  public void setTimes(String src, long mtime, long atime) throws IOException {
    namesystem.setTimes(src, mtime, atime);
  }

  /** {@inheritDoc} */
  public String fsck(String path, String params) throws IOException { 
	return NamenodeFsck.fsckPath(this, path, params);
  }
  
  ////////////////////////////////////////////////////////////////
  // DatanodeProtocol
  ////////////////////////////////////////////////////////////////
  /** 
   */
  public DatanodeRegistration register(DatanodeRegistration nodeReg
                                       ) throws IOException {
    verifyVersion(nodeReg.getVersion());
    namesystem.registerDatanode(nodeReg);
      
    return nodeReg;
  }

  /**
   * Data node notify the name node that it is alive 
   * Return an array of block-oriented commands for the datanode to execute.
   * This will be either a transfer or a delete operation.
   */
  public DatanodeCommand[] sendHeartbeat(DatanodeRegistration nodeReg,
                                       long capacity,
                                       long dfsUsed,
                                       long remaining,
                                       int xmitsInProgress,
                                       int xceiverCount) throws IOException {
    verifyRequest(nodeReg);
    return namesystem.handleHeartbeat(nodeReg, capacity, dfsUsed, remaining,
        xceiverCount, xmitsInProgress);
  }

  public DatanodeCommand blockReport(DatanodeRegistration nodeReg,
                                     long[] blocks) throws IOException {
    verifyRequest(nodeReg);
    BlockListAsLongs blist = new BlockListAsLongs(blocks);
    stateChangeLog.debug("*BLOCK* NameNode.blockReport: "
           +"from "+nodeReg.getName()+" "+blist.getNumberOfBlocks() +" blocks");

    namesystem.processReport(nodeReg, blist);
    if (getFSImage().isUpgradeFinalized())
      return DatanodeCommand.FINALIZE;
    return null;
  }

  public void blockReceived(DatanodeRegistration nodeReg, 
                            Block blocks[],
                            String delHints[]) throws IOException {
    verifyRequest(nodeReg);
    stateChangeLog.debug("*BLOCK* NameNode.blockReceived: "
                         +"from "+nodeReg.getName()+" "+blocks.length+" blocks.");
    for (int i = 0; i < blocks.length; i++) {
      namesystem.blockReceived(nodeReg, blocks[i], delHints[i]);
    }
  }

  /**
   */
  public void errorReport(DatanodeRegistration nodeReg,
                          int errorCode, 
                          String msg) throws IOException {
    // Log error message from datanode
    String dnName = (nodeReg == null ? "unknown DataNode" : nodeReg.getName());
    LOG.info("Error report from " + dnName + ": " + msg);
    if (errorCode == DatanodeProtocol.NOTIFY) {
      return;
    }
    verifyRequest(nodeReg);
    if (errorCode == DatanodeProtocol.DISK_ERROR) {
      namesystem.removeDatanode(nodeReg);            
    }
  }
    
  public NamespaceInfo versionRequest() throws IOException {
    return namesystem.getNamespaceInfo();
  }

  public UpgradeCommand processUpgradeCommand(UpgradeCommand comm) throws IOException {
    return namesystem.processDistributedUpgradeCommand(comm);
  }

  /** 
   * Verify request.
   * 
   * Verifies correctness of the datanode version, registration ID, and 
   * if the datanode does not need to be shutdown.
   * 
   * @param nodeReg data node registration
   * @throws IOException
   */
  public void verifyRequest(DatanodeRegistration nodeReg) throws IOException {
    verifyVersion(nodeReg.getVersion());
    if (!namesystem.getRegistrationID().equals(nodeReg.getRegistrationID()))
      throw new UnregisteredDatanodeException(nodeReg);
  }
    
  /**
   * Verify version.
   * 
   * @param version
   * @throws IOException
   */
  public void verifyVersion(int version) throws IOException {
    if (version != LAYOUT_VERSION)
      throw new IncorrectVersionException(version, "data node");
  }

  /**
   * Returns the name of the fsImage file
   */
  public File getFsImageName() throws IOException {
    return getFSImage().getFsImageName();
  }
    
  public FSImage getFSImage() {
    return namesystem.dir.fsImage;
  }

  /**
   * Returns the name of the fsImage file uploaded by periodic
   * checkpointing
   */
  public File[] getFsImageNameCheckpoint() throws IOException {
    return getFSImage().getFsImageNameCheckpoint();
  }

  /**
   * Returns the address on which the NameNodes is listening to.
   * @return the address on which the NameNodes is listening to.
   */
  public InetSocketAddress getNameNodeAddress() {
    return serverAddress;
  }

  /**
   * Returns the address of the NameNodes http server, 
   * which is used to access the name-node web UI.
   * 
   * @return the http address.
   */
  public InetSocketAddress getHttpAddress() {
    return httpAddress;
  }

  NetworkTopology getNetworkTopology() {
    return this.namesystem.clusterMap;
  }

  /**
   * Verify that configured directories exist, then
   * Interactively confirm that formatting is desired 
   * for each existing directory and format them.
   * 
   * @param conf
   * @param isConfirmationNeeded
   * @return true if formatting was aborted, false otherwise
   * @throws IOException
   */
  private static boolean format(Configuration conf,
                                boolean isConfirmationNeeded
                                ) throws IOException {
    Collection<File> dirsToFormat = FSNamesystem.getNamespaceDirs(conf);
    Collection<File> editDirsToFormat = 
                 FSNamesystem.getNamespaceEditsDirs(conf);
    for(Iterator<File> it = dirsToFormat.iterator(); it.hasNext();) {
      File curDir = it.next();
      if (!curDir.exists())
        continue;
      if (isConfirmationNeeded) {
        System.err.print("Re-format filesystem in " + curDir +" ? (Y or N) ");
        if (!(System.in.read() == 'Y')) {
          System.err.println("Format aborted in "+ curDir);
          return true;
        }
        while(System.in.read() != '\n'); // discard the enter-key
      }
    }

    FSNamesystem nsys = new FSNamesystem(new FSImage(dirsToFormat,
                                         editDirsToFormat), conf);
    nsys.dir.fsImage.format();
    return false;
  }

  private static boolean finalize(Configuration conf,
                               boolean isConfirmationNeeded
                               ) throws IOException {
    Collection<File> dirsToFormat = FSNamesystem.getNamespaceDirs(conf);
    Collection<File> editDirsToFormat = 
                               FSNamesystem.getNamespaceEditsDirs(conf);
    FSNamesystem nsys = new FSNamesystem(new FSImage(dirsToFormat,
                                         editDirsToFormat), conf);
    System.err.print(
        "\"finalize\" will remove the previous state of the files system.\n"
        + "Recent upgrade will become permanent.\n"
        + "Rollback option will not be available anymore.\n");
    if (isConfirmationNeeded) {
      System.err.print("Finalize filesystem state ? (Y or N) ");
      if (!(System.in.read() == 'Y')) {
        System.err.println("Finalize aborted.");
        return true;
      }
      while(System.in.read() != '\n'); // discard the enter-key
    }
    nsys.dir.fsImage.finalizeUpgrade();
    return false;
  }

  @Override
  public void refreshServiceAcl() throws IOException {
    if (!serviceAuthEnabled) {
      throw new AuthorizationException("Service Level Authorization not enabled!");
    }

    SecurityUtil.getPolicy().refresh();
  }

  public static String getHttpAddressPort(Configuration conf) throws IOException {
    //if (ClusterHelper.isSingledMode(conf)) {
    //  NamenodeOptions options = StartupOptions.getNamenodeOptions();
    //  return ClusterHelper.getAddressSetting(conf, NameNode.class, 
    //    options, options.HTTPADDRESS); 
    //} else {
    //  return ClusterHelper.getAddressPort(conf, NameNode.class, "http"); 
    //}
	  return null;
  }

  //public static String getHttpAddressPort(ClusterManagedClient client) throws IOException {
  //  return ClusterHelper.getAddressPort(client, NameNode.class, "http"); 
  //}

  public static String getAddressPort(Configuration conf) throws IOException {
    //if (ClusterHelper.isSingledMode(conf)) {
    //  NamenodeOptions options = StartupOptions.getNamenodeOptions();
    //  return ClusterHelper.getAddressSetting(conf, NameNode.class, 
    //    options, options.NAMENODEADDRESS); 
    //} else {
    //  return ClusterHelper.getAddressPort(conf, NameNode.class, 
    //    NamenodeProtocol.class.getName());
    //}
	return getAddressPort(getAddress(conf));
  }

  //public static String getAddressPort(ClusterManagedClient client) throws IOException {
  //  return ClusterHelper.getAddressPort(client, NameNode.class, NamenodeProtocol.class.getName()); 
  //}

  private void initServiceClient(Configuration conf) throws IOException {
    //if (this.client != null) return; 

    //serviceInfo.setStartTime(getStartTime());
    //serviceInfo.setUserName(VersionInfo.getUserName());

    //this.client = new ServiceClient(conf, serviceInfo); 
    //if (!conf.getBoolean("namenode.singled.mode", false)) 
    //  this.client.startup(); 
  }

  public long getStartTime() {
    return this.startTime; 
  }

  @SuppressWarnings("unused")
  private static StartupOption parseStartupOption(String args[]) {
    int argsLen = (args == null) ? 0 : args.length;
    StartupOption startOpt = StartupOption.REGULAR;
    for(int i=0; i < argsLen; i++) {
      String cmd = args[i];
      if (StartupOption.FORMAT.getName().equalsIgnoreCase(cmd)) {
        startOpt = StartupOption.FORMAT;
      } else if (StartupOption.REGULAR.getName().equalsIgnoreCase(cmd)) {
        startOpt = StartupOption.REGULAR;
      } else if (StartupOption.UPGRADE.getName().equalsIgnoreCase(cmd)) {
        startOpt = StartupOption.UPGRADE;
      } else if (StartupOption.ROLLBACK.getName().equalsIgnoreCase(cmd)) {
        startOpt = StartupOption.ROLLBACK;
      } else if (StartupOption.FINALIZE.getName().equalsIgnoreCase(cmd)) {
        startOpt = StartupOption.FINALIZE;
      } else if (StartupOption.IMPORT.getName().equalsIgnoreCase(cmd)) {
        startOpt = StartupOption.IMPORT;
      } 
    }
    return startOpt;
  }

  @SuppressWarnings("unused")
  private static void setStartupOption(Configuration conf, StartupOption opt) {
    conf.set("dfs.namenode.startup", opt.toString());
  }

  static StartupOption getStartupOption(Configuration conf) {
    return StartupOption.valueOf(conf.get("dfs.namenode.startup",
                                          StartupOption.REGULAR.toString()));
  }

  public static NameNode createNameNode(String argv[], 
                                 Configuration conf) throws IOException {
    if (conf == null)
      conf = ConfigurationFactory.create();

    //try {
    //  String[] args = ClusterHelper.getServiceArgs(conf, NameNode.class.getName()); 
    //  argv = ClusterHelper.mergeParameterArgs(argv, args); 
    //} catch (Exception e) {
    //  LOG.warn("lookup service arguments error: "+e); 
    //}

    StartupOptions.NamenodeOptions options = StartupOptions.getNamenodeOptions(); 
    options.parse(conf, argv); 

    /*
    StartupOption startOpt = parseStartupOption(argv); 
    setStartupOption(conf, startOpt);

    if (options.hasOption(options.SINGLED)) {
      conf.setBoolean("namenode.singled.mode", true);
    }
    if (options.hasOption(options.NODEADDRESS)) {
      String val = options.getOptionValue(options.NODEADDRESS); 
      String address = ClusterHelper.parseAddressPort(conf, val);
      conf.set("node.address", address); 
    }
    if (options.hasOption(options.NAMENODEADDRESS)) {
      String val = options.getOptionValue(options.NAMENODEADDRESS); 
      String address = ClusterHelper.parseAddressPort(conf, val, DEFAULT_PORT);
      FileSystem.setDefaultUri(conf, address); 
    }
    if (options.hasOption(options.HTTPADDRESS)) {
      String val = options.getOptionValue(options.HTTPADDRESS); 
      String address = ClusterHelper.parseAddressPort(conf, val, DEFAULT_HTTPPORT);
      conf.set("dfs.http.address", address); 
    }
    if (options.hasOption(options.THREADCOUNT)) {
      String val = options.getOptionValue(options.THREADCOUNT); 
      int threadcount = Integer.valueOf(val).intValue(); 
      if (threadcount <= 0)
        throw new IllegalArgumentException("thread input wrong: "+val); 
      conf.setInt("dfs.namenode.handler.count", threadcount); 
    }
    if (options.hasOption(options.NAMEDIRS)) {
      String[] vals = options.getOptionValues(options.NAMEDIRS);
      StringBuffer sbuf = new StringBuffer();
      for (int i=0; vals != null && i < vals.length; i++) {
        if (sbuf.length() > 0) sbuf.append(',');
        sbuf.append(vals[i]);
      }
      conf.set("dfs.name.dir", sbuf.toString());
      conf.set("dfs.name.edits.dir", sbuf.toString());
    }
    if (options.hasOption(options.EDITDIRS)) {
      String[] vals = options.getOptionValues(options.EDITDIRS);
      StringBuffer sbuf = new StringBuffer();
      for (int i=0; vals != null && i < vals.length; i++) {
        if (sbuf.length() > 0) sbuf.append(',');
        sbuf.append(vals[i]);
      }
      conf.set("dfs.name.edits.dir", sbuf.toString());
    }
*/
    
    if (options.hasOption(StartupOptions.NamenodeOptions.FORMAT)) {
      boolean aborted = format(conf, true);
      System.exit(aborted ? 1 : 0);
    } else if (options.hasOption(StartupOptions.NamenodeOptions.FINALIZE)) {
      boolean aborted = finalize(conf, true);
      System.exit(aborted ? 1 : 0);
    }

    NameNode namenode = new NameNode(conf);
    return namenode;
  }
 
  /**
   */
  public static void main(String argv[]) throws Exception {
	doMain(null, argv);
  }
  
  public static void doMain(Configuration conf, String argv[]) throws Exception {
    try {
      //ConfigurationFactory.setQuietMode(StartupOptions.getNamenodeOptions().hasCommandOption(argv)); 
      StringUtils.startupShutdownMessage(NameNode.class, argv, LOG);
      NameNode namenode = createNameNode(argv, conf);
      if (namenode != null)
        namenode.join();
    } catch (Throwable e) {
      LOG.error(StringUtils.stringifyException(e));
      System.exit(-1);
    }
  }
}

