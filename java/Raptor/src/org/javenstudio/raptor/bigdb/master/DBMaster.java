package org.javenstudio.raptor.bigdb.master;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//import org.apache.commons.cli.CommandLine;
//import org.apache.commons.cli.GnuParser;
//import org.apache.commons.cli.Options;
//import org.apache.commons.cli.ParseException;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.MapWritable;
import org.javenstudio.raptor.io.Text;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.ipc.RemoteException;
import org.javenstudio.raptor.net.DNS;
import org.javenstudio.raptor.bigdb.ClusterStatus;
import org.javenstudio.raptor.bigdb.DBColumnDescriptor;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBMsg;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.DBRegionLocation;
import org.javenstudio.raptor.bigdb.DBServerAddress;
import org.javenstudio.raptor.bigdb.DBServerInfo;
import org.javenstudio.raptor.bigdb.DBServerLoad;
import org.javenstudio.raptor.bigdb.DBTableDescriptor;
import org.javenstudio.raptor.bigdb.KeyValue;
import org.javenstudio.raptor.bigdb.LocalDBCluster;
import org.javenstudio.raptor.bigdb.MasterNotRunningException;
import org.javenstudio.raptor.bigdb.MiniPaxosCluster;
import org.javenstudio.raptor.bigdb.RemoteExceptionHandler;
import org.javenstudio.raptor.bigdb.TableExistsException;
import org.javenstudio.raptor.bigdb.client.DBAdmin;
import org.javenstudio.raptor.bigdb.client.Get;
import org.javenstudio.raptor.bigdb.client.MetaScanner;
import org.javenstudio.raptor.bigdb.client.Result;
import org.javenstudio.raptor.bigdb.client.Scan;
import org.javenstudio.raptor.bigdb.client.ServerConnection;
import org.javenstudio.raptor.bigdb.client.ServerConnectionManager;
import org.javenstudio.raptor.bigdb.client.MetaScanner.MetaScannerVisitor;
import org.javenstudio.raptor.bigdb.io.ImmutableBytesWritable;
import org.javenstudio.raptor.bigdb.ipc.DBRPC;
import org.javenstudio.raptor.bigdb.ipc.DBRPCProtocolVersion;
import org.javenstudio.raptor.bigdb.ipc.DBServer;
import org.javenstudio.raptor.bigdb.ipc.DBMasterInterface;
import org.javenstudio.raptor.bigdb.ipc.DBMasterRegionInterface;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.master.metrics.MasterMetrics;
import org.javenstudio.raptor.bigdb.regionserver.DBRegion;
import org.javenstudio.raptor.bigdb.regionserver.DBRegionServer;
import org.javenstudio.raptor.bigdb.regionserver.wal.DBLog;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.bigdb.util.FSUtils;
import org.javenstudio.raptor.bigdb.util.InfoServer;
import org.javenstudio.raptor.bigdb.util.Pair;
import org.javenstudio.raptor.bigdb.util.Sleeper;
import org.javenstudio.raptor.util.InputSource;
import org.javenstudio.raptor.util.VersionInfo;
import org.javenstudio.raptor.bigdb.util.Writables;
import org.javenstudio.raptor.bigdb.paxos.PaxosWrapper;
import org.javenstudio.raptor.paxos.WatchedEvent;
import org.javenstudio.raptor.paxos.Watcher;
import org.javenstudio.raptor.paxos.Watcher.Event.EventType;
import org.javenstudio.raptor.paxos.Watcher.Event.PaxosState;

import com.google.common.collect.Lists;

/**
 * DBMaster is the "master server" for BigDB. An BigDB cluster has one active
 * master.  If many masters are started, all compete.  Whichever wins goes on to
 * run the cluster.  All others park themselves in their constructor until
 * master or cluster shutdown or until the active master loses its lease in
 * paxos.  Thereafter, all running master jostle to take over master role.
 * @see DBMasterInterface
 * @see DBMasterRegionInterface
 * @see Watcher
 */
public class DBMaster extends Thread implements DBMasterInterface,
    DBMasterRegionInterface, Watcher {
  // MASTER is name of the webapp and the attribute name used stuffing this
  //instance into web context.
  public static final String MASTER = "master";
  private static final Logger LOG = Logger.getLogger(DBMaster.class);

  // We start out with closed flag on.  Its set to off after construction.
  // Use AtomicBoolean rather than plain boolean because we want other threads
  // able to set shutdown flag.  Using AtomicBoolean can pass a reference
  // rather than have them have to know about the hosting Master class.
  final AtomicBoolean closed = new AtomicBoolean(true);
  // TODO: Is this separate flag necessary?
  private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

  private final Configuration conf;
  private final Path rootdir;
  private InfoServer infoServer;
  private final int threadWakeFrequency;
  private final int numRetries;

  // Metrics is set when we call run.
  private final MasterMetrics metrics;

  final Lock splitLogLock = new ReentrantLock();

  // Our zk client.
  private PaxosWrapper paxosWrapper;
  // Watcher for master address and for cluster shutdown.
  private final PaxosMasterAddressWatcher paxosMasterAddressWatcher;
  // A Sleeper that sleeps for threadWakeFrequency; sleep if nothing todo.
  private final Sleeper sleeper;
  // Keep around for convenience.
  private final FileSystem fs;
  // Is the fileystem ok?
  private volatile boolean fsOk = true;
  // The Path to the old logs dir
  private final Path oldLogDir;

  private final DBServer rpcServer;
  private final DBServerAddress address;

  private final ServerConnection connection;
  private final ServerManager serverManager;
  private final RegionManager regionManager;

  private long lastFragmentationQuery = -1L;
  private Map<String, Integer> fragmentation = null;
  private final RegionServerOperationQueue regionServerOperationQueue;
  
  // True if this is the master that started the cluster.
  boolean isClusterStartup;

  /**
   * Constructor
   * @param conf configuration
   * @throws IOException
   */
  public DBMaster(Configuration conf) throws IOException {
    this.conf = conf;
    
    // Figure out if this is a fresh cluster start. This is done by checking the 
    // number of RS ephemeral nodes. RS ephemeral nodes are created only after 
    // the primary master has written the address to ZK. So this has to be done 
    // before we race to write our address to paxos.
    paxosWrapper = PaxosWrapper.createInstance(conf, DBMaster.class.getName());
    isClusterStartup = (paxosWrapper.scanRSDirectory().size() == 0);
    
    // Get my address and create an rpc server instance.  The rpc-server port
    // can be ephemeral...ensure we have the correct info
    DBServerAddress a = new DBServerAddress(getMyAddress(this.conf));
    this.rpcServer = DBRPC.getServer(this,
      new Class<?>[]{DBMasterInterface.class, DBMasterRegionInterface.class},
        a.getBindAddress(), a.getPort(),
        conf.getInt("bigdb.regionserver.handler.count", 10), false, conf);

    this.address = new DBServerAddress(this.rpcServer.getListenerAddress());

    this.numRetries =  conf.getInt("bigdb.client.retries.number", 2);
    this.threadWakeFrequency = conf.getInt(DBConstants.THREAD_WAKE_FREQUENCY,
        10 * 1000);

    this.sleeper = new Sleeper(this.threadWakeFrequency, this.closed);
    this.connection = ServerConnectionManager.getConnection(conf);

    // hack! Maps DFSClient => Master for logs.  HDFS made this 
    // config param for task trackers, but we can piggyback off of it.
    if (this.conf.get("mapred.task.id") == null) {
      this.conf.set("mapred.task.id", "hb_m_" + this.address.toString() +
        "_" + System.currentTimeMillis());
    }

    // Set filesystem to be that of this.rootdir else we get complaints about
    // mismatched filesystems if bigdb.rootdir is hdfs and fs.defaultFS is
    // default localfs.  Presumption is that rootdir is fully-qualified before
    // we get to here with appropriate fs scheme.
    this.rootdir = FSUtils.getRootDir(this.conf);
    // Cover both bases, the old way of setting default fs and the new.
    // We're supposed to run on 0.20 and 0.21 anyways.
    //this.conf.set("fs.default.name", this.rootdir.toString());
    //this.conf.set("fs.defaultFS", this.rootdir.toString());
    this.fs = FSUtils.getFs(this.conf);
    checkRootDir(this.rootdir, this.conf, this.fs);

    // Make sure the region servers can archive their old logs
    this.oldLogDir = new Path(this.rootdir, DBConstants.DBREGION_OLDLOGDIR_NAME);
    if(!this.fs.exists(this.oldLogDir)) {
      this.fs.mkdirs(this.oldLogDir);
    }

    // Get our paxos wrapper and then try to write our address to paxos.
    // We'll succeed if we are only  master or if we win the race when many
    // masters.  Otherwise we park here inside in writeAddressToPaxos.
    // TODO: Bring up the UI to redirect to active Master.
    paxosWrapper.registerListener(this);
    this.paxosMasterAddressWatcher =
      new PaxosMasterAddressWatcher(this.paxosWrapper, this.shutdownRequested);
    paxosWrapper.registerListener(paxosMasterAddressWatcher);

    // if we're a backup master, stall until a primary to writes his address
    if(conf.getBoolean(DBConstants.MASTER_TYPE_BACKUP, DBConstants.DEFAULT_MASTER_TYPE_BACKUP)) {
      // this will only be a minute or so while the cluster starts up,
      // so don't worry about setting watches on the parent znode
      while (!paxosWrapper.masterAddressExists()) {
        try {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Waiting for master address ZNode to be written " +
              "(Also watching cluster state node)");
          }
          Thread.sleep(conf.getInt("paxos.session.timeout", 60 * 1000));
        } catch (InterruptedException e) {
          // interrupted = user wants to kill us.  Don't continue
          throw new IOException("Interrupted waiting for master address");
        }
      }
    }
    this.paxosMasterAddressWatcher.writeAddressToPaxos(this.address, true);
    this.regionServerOperationQueue =
      new RegionServerOperationQueue(this.conf, this.closed);

    serverManager = new ServerManager(this);
    // start the region manager
    regionManager = new RegionManager(this);

    setName(MASTER);
    this.metrics = new MasterMetrics(MASTER);
    // We're almost open for business
    this.closed.set(false);
    if (LOG.isInfoEnabled())
      LOG.info("DBMaster initialized on " + this.address.toString());
  }
  
  /**
   * Returns true if this master process was responsible for starting the 
   * cluster.
   */
  public boolean isClusterStartup() {
    return isClusterStartup;
  }
  
  public void resetClusterStartup() {
    isClusterStartup = false;
  }
  
  public DBServerAddress getDBServerAddress() {
    return address;
  }

  /**
   * Get the rootdir.  Make sure its wholesome and exists before returning.
   * @param rd
   * @param conf
   * @param fs
   * @return bigdb.rootdir (after checks for existence and bootstrapping if
   * needed populating the directory with necessary bootup files).
   * @throws IOException
   */
  private static Path checkRootDir(final Path rd, final Configuration c,
    final FileSystem fs) throws IOException {
    // If FS is in safe mode wait till out of it.
    FSUtils.waitOnSafeMode(c, c.getInt(DBConstants.THREAD_WAKE_FREQUENCY,
        10 * 1000));
    // Filesystem is good. Go ahead and check for bigdb.rootdir.
    if (!fs.exists(rd)) {
      fs.mkdirs(rd);
      FSUtils.setVersion(fs, rd);
    } else {
      FSUtils.checkVersion(fs, rd, true);
    }
    // Make sure the root region directory exists!
    if (!FSUtils.rootRegionExists(fs, rd)) {
      bootstrap(rd, c);
    }
    return rd;
  }

  private static void bootstrap(final Path rd, final Configuration c)
      throws IOException {
	if (LOG.isInfoEnabled())
      LOG.info("BOOTSTRAP: creating ROOT and first META regions");
    try {
      // Bootstrapping, make sure blockcache is off.  Else, one will be
      // created here in bootstap and it'll need to be cleaned up.  Better to
      // not make it in first place.  Turn off block caching for bootstrap.
      // Enable after.
      DBRegionInfo rootHRI = new DBRegionInfo(DBRegionInfo.ROOT_REGIONINFO);
      setInfoFamilyCaching(rootHRI, false);
      DBRegionInfo metaHRI = new DBRegionInfo(DBRegionInfo.FIRST_META_REGIONINFO);
      setInfoFamilyCaching(metaHRI, false);
      DBRegion root = DBRegion.createDBRegion(rootHRI, rd, c);
      DBRegion meta = DBRegion.createDBRegion(metaHRI, rd, c);
      setInfoFamilyCaching(rootHRI, true);
      setInfoFamilyCaching(metaHRI, true);
      // Add first region from the META table to the ROOT region.
      DBRegion.addRegionToMETA(root, meta);
      root.close();
      root.getLog().closeAndDelete();
      meta.close();
      meta.getLog().closeAndDelete();
    } catch (IOException e) {
      e = RemoteExceptionHandler.checkIOException(e);
      if (LOG.isErrorEnabled())
        LOG.error("bootstrap: " + e, e);
      throw e;
    }
  }

  /**
   * @param hri Set all family block caching to <code>b</code>
   * @param b
   */
  private static void setInfoFamilyCaching(final DBRegionInfo hri, final boolean b) {
    for (DBColumnDescriptor hcd: hri.getTableDesc().families.values()) {
      if (Bytes.equals(hcd.getName(), DBConstants.CATALOG_FAMILY)) {
        hcd.setBlockCacheEnabled(b);
        hcd.setInMemory(b);
      }
    }
  }

  /**
   * @return This masters' address.
   * @throws UnknownHostException
   */
  private static String getMyAddress(final Configuration c)
      throws UnknownHostException {
    // Find out our address up in DNS.
    String s = DNS.getDefaultHost(c.get("bigdb.master.dns.interface","default"),
      c.get("bigdb.master.dns.nameserver","default"));
    s += ":" + c.get(DBConstants.MASTER_PORT,
        Integer.toString(DBConstants.DEFAULT_MASTER_PORT));
    return s;
  }

  /**
   * Checks to see if the file system is still accessible.
   * If not, sets closed
   * @return false if file system is not available
   */
  protected boolean checkFileSystem() {
    if (this.fsOk) {
      try {
        FSUtils.checkFileSystemAvailable(this.fs);
      } catch (IOException e) {
    	if (LOG.isFatalEnabled())
          LOG.fatal("Shutting down BigDB cluster: file system not available: " + e, e);
        this.closed.set(true);
        this.fsOk = false;
      }
    }
    return this.fsOk;
  }

  /** @return DBServerAddress of the master server */
  public DBServerAddress getMasterAddress() {
    return this.address;
  }

  public long getProtocolVersion(String protocol, long clientVersion) {
    return DBRPCProtocolVersion.versionID;
  }

  /** @return InfoServer object. Maybe null.*/
  public InfoServer getInfoServer() {
    return this.infoServer;
  }

  /**
   * @return BigDB root dir.
   * @throws IOException
   */
  public Path getRootDir() {
    return this.rootdir;
  }

  public int getNumRetries() {
    return this.numRetries;
  }

  /**
   * @return Server metrics
   */
  public MasterMetrics getMetrics() {
    return this.metrics;
  }

  /**
   * @return Return configuration being used by this server.
   */
  public Configuration getConfiguration() {
    return this.conf;
  }

  public ServerManager getServerManager() {
    return this.serverManager;
  }

  public RegionManager getRegionManager() {
    return this.regionManager;
  }

  int getThreadWakeFrequency() {
    return this.threadWakeFrequency;
  }

  FileSystem getFileSystem() {
    return this.fs;
  }

  AtomicBoolean getShutdownRequested() {
    return this.shutdownRequested;
  }

  AtomicBoolean getClosed() {
    return this.closed;
  }

  boolean isClosed() {
    return this.closed.get();
  }

  ServerConnection getServerConnection() {
    return this.connection;
  }

  /**
   * Get the ZK wrapper object
   * @return the paxos wrapper
   */
  public PaxosWrapper getPaxosWrapper() {
    return this.paxosWrapper;
  }

  // These methods are so don't have to pollute RegionManager with ServerManager.
  SortedMap<DBServerLoad, Set<String>> getLoadToServers() {
    return this.serverManager.getLoadToServers();
  }

  int numServers() {
    return this.serverManager.numServers();
  }

  public double getAverageLoad() {
    return this.serverManager.getAverageLoad();
  }

  public RegionServerOperationQueue getRegionServerOperationQueue () {
    return this.regionServerOperationQueue;
  }

  /**
   * Get the directory where old logs go
   * @return the dir
   */
  public Path getOldLogDir() {
    return this.oldLogDir;
  }

  /**
   * Add to the passed <code>m</code> servers that are loaded less than
   * <code>l</code>.
   * @param l
   * @param m
   */
  void getLightServers(final DBServerLoad l,
      SortedMap<DBServerLoad, Set<String>> m) {
    this.serverManager.getLightServers(l, m);
  }

  /** Main processing loop */
  @Override
  public void run() {
    joinCluster();
    startServiceThreads();
    /* Main processing loop */
    try {
      FINISHED: while (!this.closed.get()) {
        // check if we should be shutting down
        if (this.shutdownRequested.get()) {
          // The region servers won't all exit until we stop scanning the
          // meta regions
          this.regionManager.stopScanners();
          if (this.serverManager.numServers() == 0) {
            startShutdown();
            break;
          } else {
        	if (LOG.isDebugEnabled())
              LOG.debug("Waiting on " + this.serverManager.getServersToServerInfo().keySet().toString());
          }
        }
        switch (this.regionServerOperationQueue.process()) {
        case FAILED:
            // If FAILED op processing, bad. Exit.
          break FINISHED;
        case REQUEUED_BUT_PROBLEM:
          if (!checkFileSystem())
              // If bad filesystem, exit.
            break FINISHED;
          default:
            // Continue run loop if conditions are PROCESSED, NOOP, REQUEUED
          break;
        }
      }
    } catch (Throwable t) {
      if (LOG.isFatalEnabled())
        LOG.fatal("Unhandled exception. Starting shutdown.", t);
      this.closed.set(true);
    }

    // Wait for all the remaining region servers to report in.
    this.serverManager.letRegionServersShutdown();

    /*
     * Clean up and close up shop
     */
    if (this.infoServer != null) {
      if (LOG.isInfoEnabled())
        LOG.info("Stopping infoServer");
      try {
        this.infoServer.stop();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    this.rpcServer.stop();
    this.regionManager.stop();
    this.paxosWrapper.close();
    if (LOG.isInfoEnabled())
      LOG.info("DBMaster main thread exiting");
  }

  /**
   * Joins cluster.  Checks to see if this instance of BigDB is fresh or the
   * master was started following a failover. In the second case, it inspects
   * the region server directory and gets their regions assignment.
   */
  private void joinCluster()  {
	  if (LOG.isDebugEnabled())
        LOG.debug("Checking cluster state...");
      DBServerAddress rootLocation =
        this.paxosWrapper.readRootRegionLocation();
      List<DBServerAddress> addresses = this.paxosWrapper.scanRSDirectory();
      // Check if this is a fresh start of the cluster
      if (addresses.isEmpty()) {
    	if (LOG.isDebugEnabled())
          LOG.debug("Master fresh start, proceeding with normal startup");
        splitLogAfterStartup();
        return;
      }
      // Failover case.
      if (LOG.isInfoEnabled())
        LOG.info("Master failover, ZK inspection begins...");
      boolean isRootRegionAssigned = false;
      Map <byte[], DBRegionInfo> assignedRegions =
        new HashMap<byte[], DBRegionInfo>();
      // We must:
      // - contact every region server to add them to the regionservers list
      // - get their current regions assignment
      // TODO: Run in parallel?
      for (DBServerAddress address : addresses) {
        DBRegionInfo[] regions = null;
        try {
          DBRegionInterface hri =
            this.connection.getDBRegionConnection(address, false);
          DBServerInfo info = hri.getDBServerInfo();
          if (LOG.isDebugEnabled())
            LOG.debug("Inspection found server " + info.getServerName());
          this.serverManager.recordNewServer(info, true);
          regions = hri.getRegionsAssignment();
        } catch (IOException e) {
          if (LOG.isErrorEnabled())
            LOG.error("Failed contacting " + address.toString(), e);
          continue;
        }
        for (DBRegionInfo r: regions) {
          if (r.isRootRegion()) {
            this.connection.setRootRegionLocation(new DBRegionLocation(r, rootLocation));
            this.regionManager.setRootRegionLocation(rootLocation);
            // Undo the unassign work in the RegionManager constructor
            this.regionManager.removeRegion(r);
            isRootRegionAssigned = true;
          } else if (r.isMetaRegion()) {
            MetaRegion m = new MetaRegion(new DBServerAddress(address), r);
            this.regionManager.addMetaRegionToScan(m);
          }
          assignedRegions.put(r.getRegionName(), r);
        }
      }
      if (LOG.isInfoEnabled()) {
        LOG.info("Inspection found " + assignedRegions.size() + " regions, " +
          (isRootRegionAssigned ? "with -ROOT-" : "but -ROOT- was MIA"));
      }
      splitLogAfterStartup();
  }

  /**
   * Inspect the log directory to recover any log file without
   * ad active region server.
   */
  private void splitLogAfterStartup() {
    Path logsDirPath =
      new Path(this.rootdir, DBConstants.DBREGION_LOGDIR_NAME);
    try {
      if (!this.fs.exists(logsDirPath)) return;
    } catch (IOException e) {
      throw new RuntimeException("Could exists for " + logsDirPath, e);
    }
    FileStatus[] logFolders;
    try {
      logFolders = this.fs.listStatus(logsDirPath);
    } catch (IOException e) {
      throw new RuntimeException("Failed listing " + logsDirPath.toString(), e);
    }
    if (logFolders == null || logFolders.length == 0) {
      if (LOG.isDebugEnabled())
        LOG.debug("No log files to split, proceeding...");
      return;
    }
    for (FileStatus status : logFolders) {
      String serverName = status.getPath().getName();
      if (LOG.isInfoEnabled())
        LOG.info("Found log folder : " + serverName);
      if (this.serverManager.getServerInfo(serverName) == null) {
    	if (LOG.isInfoEnabled()) 
          LOG.info("Log folder doesn't belong to a known region server, splitting");
        this.splitLogLock.lock();
        Path logDir =
          new Path(this.rootdir, DBLog.getDBLogDirectoryName(serverName));
        try {
          DBLog.splitLog(this.rootdir, logDir, oldLogDir, this.fs, getConfiguration());
        } catch (IOException e) {
          if (LOG.isErrorEnabled())
            LOG.error("Failed splitting " + logDir.toString() + " : " + e, e);
        } finally {
          this.splitLogLock.unlock();
        }
      } else {
    	if (LOG.isInfoEnabled())
          LOG.info("Log folder belongs to an existing region server");
      }
    }
  }

  /**
   * Start up all services. If any of these threads gets an unhandled exception
   * then they just die with a logged message.  This should be fine because
   * in general, we do not expect the master to get such unhandled exceptions
   *  as OOMEs; it should be lightly loaded. See what DBRegionServer does if
   *  need to install an unexpected exception handler.
   */
  private void startServiceThreads() {
    try {
      this.regionManager.start();
      // Put up info server.
      int port = this.conf.getInt("bigdb.master.info.port", 60010);
      if (port >= 0) {
        String a = this.conf.get("bigdb.master.info.bindAddress", "0.0.0.0");
        this.infoServer = new InfoServer(MASTER, a, port, false);
        this.infoServer.setAttribute(MASTER, this);
        this.infoServer.start();
      }
      // Start the server so everything else is running before we start
      // receiving requests.
      this.rpcServer.start();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Started service threads");
      }
    } catch (IOException e) {
      if (e instanceof RemoteException) {
        try {
          e = RemoteExceptionHandler.decodeRemoteException((RemoteException) e);
        } catch (IOException ex) {
          if (LOG.isWarnEnabled())
            LOG.warn("thread start: " + ex, ex);
        }
      }
      // Something happened during startup. Shut things down.
      this.closed.set(true);
      if (LOG.isErrorEnabled())
       LOG.error("Failed startup: " + e, e);
    }
  }

  /**
   * Start shutting down the master
   */
  void startShutdown() {
    this.closed.set(true);
    this.regionManager.stopScanners();
    this.regionServerOperationQueue.shutdown();
    this.serverManager.notifyServers();
  }

  public MapWritable regionServerStartup(final DBServerInfo serverInfo)
      throws IOException {
    // Set the ip into the passed in serverInfo.  Its ip is more than likely
    // not the ip that the master sees here.  See at end of this method where
    // we pass it back to the regionserver by setting "bigdb.regionserver.address"
    String rsAddress = DBServer.getRemoteAddress();
    serverInfo.setServerAddress(new DBServerAddress(rsAddress,
      serverInfo.getServerAddress().getPort()));
    // Register with server manager
    this.serverManager.regionServerStartup(serverInfo);
    // Send back some config info
    MapWritable mw = createConfigurationSubset();
     mw.put(new Text("bigdb.regionserver.address"), new Text(rsAddress));
    return mw;
  }

  /**
   * @return Subset of configuration to pass initializing regionservers: e.g.
   * the filesystem to use and root directory to use.
   */
  protected MapWritable createConfigurationSubset() {
    MapWritable mw = addConfig(new MapWritable(), DBConstants.BIGDB_DIR);
    return addConfig(mw, "fs.default.name");
  }

  private MapWritable addConfig(final MapWritable mw, final String key) {
    mw.put(new Text(key), new Text(this.conf.get(key)));
    return mw;
  }

  public DBMsg [] regionServerReport(DBServerInfo serverInfo, DBMsg msgs[],
    DBRegionInfo[] mostLoadedRegions) throws IOException {
    return adornRegionServerAnswer(serverInfo,
      this.serverManager.regionServerReport(serverInfo, msgs, mostLoadedRegions));
  }

  /**
   * Override if you'd add messages to return to regionserver <code>hsi</code>
   * or to send an exception.
   * @param msgs Messages to add to
   * @return Messages to return to
   * @throws IOException exceptions that were injected for the region servers
   */
  protected DBMsg [] adornRegionServerAnswer(final DBServerInfo hsi,
      final DBMsg [] msgs) throws IOException {
    return msgs;
  }

  public boolean isMasterRunning() {
    return !this.closed.get();
  }

  public void shutdown() {
	if (LOG.isInfoEnabled())
      LOG.info("Cluster shutdown requested. Starting to quiesce servers");
    this.shutdownRequested.set(true);
    this.paxosWrapper.setClusterState(false);
  }

  public void createTable(DBTableDescriptor desc, byte [][] splitKeys)
      throws IOException {
    if (!isMasterRunning()) {
      throw new MasterNotRunningException();
    }
    DBRegionInfo [] newRegions = null;
    if(splitKeys == null || splitKeys.length == 0) {
      newRegions = new DBRegionInfo [] { new DBRegionInfo(desc, null, null) };
    } else {
      int numRegions = splitKeys.length + 1;
      newRegions = new DBRegionInfo[numRegions];
      byte [] startKey = null;
      byte [] endKey = null;
      for(int i=0;i<numRegions;i++) {
        endKey = (i == splitKeys.length) ? null : splitKeys[i];
        newRegions[i] = new DBRegionInfo(desc, startKey, endKey);
        startKey = endKey;
      }
    }
    for (int tries = 0; tries < this.numRetries; tries++) {
      try {
        // We can not create a table unless meta regions have already been
        // assigned and scanned.
        if (!this.regionManager.areAllMetaRegionsOnline()) {
          throw new NotAllMetaRegionsOnlineException();
        }
        if (!this.serverManager.canAssignUserRegions()) {
          throw new IOException("not enough servers to create table yet");
        }
        createTable(newRegions);
        if (LOG.isInfoEnabled())
          LOG.info("created table " + desc.getNameAsString());
        break;
      } catch (TableExistsException e) {
        throw e;
      } catch (IOException e) {
        if (tries == this.numRetries - 1) {
          throw RemoteExceptionHandler.checkIOException(e);
        }
        this.sleeper.sleep();
      }
    }
  }

  private synchronized void createTable(final DBRegionInfo [] newRegions)
  throws IOException {
    String tableName = newRegions[0].getTableDesc().getNameAsString();
    // 1. Check to see if table already exists. Get meta region where
    // table would sit should it exist. Open scanner on it. If a region
    // for the table we want to create already exists, then table already
    // created. Throw already-exists exception.
    MetaRegion m = regionManager.getFirstMetaRegionForRegion(newRegions[0]);
    byte [] metaRegionName = m.getRegionName();
    DBRegionInterface srvr = this.connection.getDBRegionConnection(m.getServer());
    byte[] firstRowInTable = Bytes.toBytes(tableName + ",,");
    Scan scan = new Scan(firstRowInTable);
    scan.addColumn(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER);
    long scannerid = srvr.openScanner(metaRegionName, scan);
    try {
      Result data = srvr.next(scannerid);
      if (data != null && data.size() > 0) {
        DBRegionInfo info = Writables.getDBRegionInfo(
          data.getValue(DBConstants.CATALOG_FAMILY,
              DBConstants.REGIONINFO_QUALIFIER));
        if (info.getTableDesc().getNameAsString().equals(tableName)) {
          // A region for this table already exists. Ergo table exists.
          throw new TableExistsException(tableName);
        }
      }
    } finally {
      srvr.close(scannerid);
    }
    for(DBRegionInfo newRegion : newRegions) {
      regionManager.createRegion(newRegion, srvr, metaRegionName);
    }
  }

  public void deleteTable(final byte [] tableName) throws IOException {
    if (Bytes.equals(tableName, DBConstants.ROOT_TABLE_NAME)) {
      throw new IOException("Can't delete root table");
    }
    new TableDelete(this, tableName).process();
    LOG.info("deleted table: " + Bytes.toString(tableName));
  }

  public void addColumn(byte [] tableName, DBColumnDescriptor column)
      throws IOException {
    new AddColumn(this, tableName, column).process();
  }

  public void modifyColumn(byte [] tableName, byte [] columnName,
    DBColumnDescriptor descriptor) throws IOException {
    new ModifyColumn(this, tableName, columnName, descriptor).process();
  }

  public void deleteColumn(final byte [] tableName, final byte [] c)
      throws IOException {
    new DeleteColumn(this, tableName, KeyValue.parseColumn(c)[0]).process();
  }

  public void enableTable(final byte [] tableName) throws IOException {
    if (Bytes.equals(tableName, DBConstants.ROOT_TABLE_NAME)) {
      throw new IOException("Can't enable root table");
    }
    new ChangeTableState(this, tableName, true).process();
  }

  public void disableTable(final byte [] tableName) throws IOException {
    if (Bytes.equals(tableName, DBConstants.ROOT_TABLE_NAME)) {
      throw new IOException("Can't disable root table");
    }
    new ChangeTableState(this, tableName, false).process();
  }

  /**
   * Get a list of the regions for a given table. The pairs may have
   * null for their second element in the case that they are not
   * currently deployed.
   * TODO: Redo so this method does not duplicate code with subsequent methods.
   */
  List<Pair<DBRegionInfo,DBServerAddress>> getTableRegions(
      final byte [] tableName) throws IOException {
    final ArrayList<Pair<DBRegionInfo, DBServerAddress>> result =
      Lists.newArrayList();
    MetaScannerVisitor visitor =
      new MetaScannerVisitor() {
        @Override
        public boolean processRow(Result data) throws IOException {
          if (data == null || data.size() <= 0)
            return true;
          Pair<DBRegionInfo, DBServerAddress> pair =
            metaRowToRegionPair(data);
          if (pair == null) return false;
          if (!Bytes.equals(pair.getFirst().getTableDesc().getName(),
                tableName)) {
            return false;
          }
          result.add(pair);
          return true;
        }
    };

    MetaScanner.metaScan(conf, visitor, tableName); 
    return result;
  }
  
  private Pair<DBRegionInfo, DBServerAddress> metaRowToRegionPair(
      Result data) throws IOException {
    DBRegionInfo info = Writables.getDBRegionInfo(
        data.getValue(DBConstants.CATALOG_FAMILY,
            DBConstants.REGIONINFO_QUALIFIER));
    final byte[] value = data.getValue(DBConstants.CATALOG_FAMILY,
        DBConstants.SERVER_QUALIFIER);
    if (value != null && value.length > 0) {
      DBServerAddress server = new DBServerAddress(Bytes.toString(value));
      return new Pair<DBRegionInfo,DBServerAddress>(info, server);
    } else {
      //undeployed
      return new Pair<DBRegionInfo, DBServerAddress>(info, null);
    }    
  }

  /**
   * Return the region and current deployment for the region containing
   * the given row. If the region cannot be found, returns null. If it
   * is found, but not currently deployed, the second element of the pair
   * may be null.
   */
  Pair<DBRegionInfo,DBServerAddress> getTableRegionForRow(
      final byte [] tableName, final byte [] rowKey) throws IOException {
    final AtomicReference<Pair<DBRegionInfo, DBServerAddress>> result =
      new AtomicReference<Pair<DBRegionInfo, DBServerAddress>>(null);
    
    MetaScannerVisitor visitor =
      new MetaScannerVisitor() {
        @Override
        public boolean processRow(Result data) throws IOException {
          if (data == null || data.size() <= 0)
            return true;
          Pair<DBRegionInfo, DBServerAddress> pair =
            metaRowToRegionPair(data);
          if (pair == null) return false;
          if (!Bytes.equals(pair.getFirst().getTableDesc().getName(),
                tableName)) {
            return false;
          }
          result.set(pair);
          return true;
        }
    };

    MetaScanner.metaScan(conf, visitor, tableName, rowKey, 1);
    return result.get();
  }
  
  Pair<DBRegionInfo,DBServerAddress> getTableRegionFromName(
      final byte [] regionName) throws IOException {
    byte [] tableName = DBRegionInfo.parseRegionName(regionName)[0];
    
    Set<MetaRegion> regions = regionManager.getMetaRegionsForTable(tableName);
    for (MetaRegion m: regions) {
      byte [] metaRegionName = m.getRegionName();
      DBRegionInterface srvr = connection.getDBRegionConnection(m.getServer());
      Get get = new Get(regionName);
      get.addColumn(DBConstants.CATALOG_FAMILY,
          DBConstants.REGIONINFO_QUALIFIER);
      get.addColumn(DBConstants.CATALOG_FAMILY, DBConstants.SERVER_QUALIFIER);
      Result data = srvr.get(metaRegionName, get);
      if(data == null || data.size() <= 0) continue;
      return metaRowToRegionPair(data);
    }
    return null;
  }

  /**
   * Get row from meta table.
   * @param row
   * @param family
   * @return Result
   * @throws IOException
   */
  protected Result getFromMETA(final byte [] row, final byte [] family)
      throws IOException {
    MetaRegion meta = this.regionManager.getMetaRegionForRow(row);
    DBRegionInterface srvr = getMETAServer(meta);
    Get get = new Get(row);
    get.addFamily(family);
    return srvr.get(meta.getRegionName(), get);
  }

  /*
   * @param meta
   * @return Server connection to <code>meta</code> .META. region.
   * @throws IOException
   */
  private DBRegionInterface getMETAServer(final MetaRegion meta)
      throws IOException {
    return this.connection.getDBRegionConnection(meta.getServer());
  }

  public void modifyTable(final byte[] tableName, DBConstants.Modify op,
      Writable[] args) throws IOException {
    switch (op) {
    case TABLE_SET_HTD:
      if (args == null || args.length < 1 ||
          !(args[0] instanceof DBTableDescriptor))
        throw new IOException("SET_HTD request requires an DBTableDescriptor");
      DBTableDescriptor htd = (DBTableDescriptor) args[0];
      if (LOG.isInfoEnabled())
        LOG.info("modifyTable(SET_HTD): " + htd);
      new ModifyTableMeta(this, tableName, htd).process();
      break;

    case TABLE_SPLIT:
    case TABLE_COMPACT:
    case TABLE_MAJOR_COMPACT:
    case TABLE_FLUSH:
      if (args != null && args.length > 0) {
        if (!(args[0] instanceof ImmutableBytesWritable))
          throw new IOException("request argument must be ImmutableBytesWritable");
        Pair<DBRegionInfo,DBServerAddress> pair = null;
        if(tableName == null) {
          byte [] regionName = ((ImmutableBytesWritable)args[0]).get();
          pair = getTableRegionFromName(regionName);
        } else {
          byte [] rowKey = ((ImmutableBytesWritable)args[0]).get();
          pair = getTableRegionForRow(tableName, rowKey);
        }
        if (LOG.isInfoEnabled())
          LOG.info("About to " + op.toString() + " on " + Bytes.toString(tableName) + " and pair is " + pair);
        if (pair != null && pair.getSecond() != null) {
          this.regionManager.startAction(pair.getFirst().getRegionName(),
            pair.getFirst(), pair.getSecond(), op);
        }
      } else {
        for (Pair<DBRegionInfo,DBServerAddress> pair: getTableRegions(tableName)) {
          if (pair.getSecond() == null) continue; // undeployed
          this.regionManager.startAction(pair.getFirst().getRegionName(),
            pair.getFirst(), pair.getSecond(), op);
        }
      }
      break;

    case CLOSE_REGION:
      if (args == null || args.length < 1 || args.length > 2) {
        throw new IOException("Requires at least a region name; " +
          "or cannot have more than region name and servername");
      }
      // Arguments are regionname and an optional server name.
      byte [] regionname = ((ImmutableBytesWritable)args[0]).get();
      if (LOG.isDebugEnabled())
        LOG.debug("Attempting to close region: " + Bytes.toStringBinary(regionname));
      String hostnameAndPort = null;
      if (args.length == 2) {
        hostnameAndPort = Bytes.toString(((ImmutableBytesWritable)args[1]).get());
      }
      // Need hri
      Result rr = getFromMETA(regionname, DBConstants.CATALOG_FAMILY);
      DBRegionInfo hri = getDBRegionInfo(rr.getRow(), rr);
      if (hostnameAndPort == null) {
        // Get server from the .META. if it wasn't passed as argument
        hostnameAndPort =
          Bytes.toString(rr.getValue(DBConstants.CATALOG_FAMILY,
              DBConstants.SERVER_QUALIFIER));
      }
      // Take region out of the intransistions in case it got stuck there doing
      // an open or whatever.
      this.regionManager.clearFromInTransition(regionname);
      // If hostnameAndPort is still null, then none, exit.
      if (hostnameAndPort == null) break;
      long startCode =
        Bytes.toLong(rr.getValue(DBConstants.CATALOG_FAMILY,
            DBConstants.STARTCODE_QUALIFIER));
      String name = DBServerInfo.getServerName(hostnameAndPort, startCode);
      if (LOG.isInfoEnabled()) {
        LOG.info("Marking " + hri.getRegionNameAsString() +
          " as closing on " + name + "; cleaning SERVER + STARTCODE; " +
          "master will tell regionserver to close region on next heartbeat");
      }
      this.regionManager.setClosing(name, hri, hri.isOffline());
      MetaRegion meta = this.regionManager.getMetaRegionForRow(regionname);
      DBRegionInterface srvr = getMETAServer(meta);
      DBRegion.cleanRegionInMETA(srvr, meta.getRegionName(), hri);
      break;

    default:
      throw new IOException("unsupported modifyTable op " + op);
    }
  }

  /**
   * @return cluster status
   */
  public ClusterStatus getClusterStatus() {
    ClusterStatus status = new ClusterStatus();
    status.setDBVersion(VersionInfo.getVersion());
    status.setServerInfo(serverManager.getServersToServerInfo().values());
    status.setDeadServers(serverManager.getDeadServers());
    status.setRegionsInTransition(this.regionManager.getRegionsInTransition());
    return status;
  }

  // TODO ryan rework this function
  /**
   * Get DBRegionInfo from passed META map of row values.
   * Returns null if none found (and logs fact that expected COL_REGIONINFO
   * was missing).  Utility method used by scanners of META tables.
   * @param row name of the row
   * @param map Map to do lookup in.
   * @return Null or found DBRegionInfo.
   * @throws IOException
   */
  DBRegionInfo getDBRegionInfo(final byte [] row, final Result res)
      throws IOException {
    byte[] regioninfo = res.getValue(DBConstants.CATALOG_FAMILY,
        DBConstants.REGIONINFO_QUALIFIER);
    if (regioninfo == null) {
      StringBuilder sb =  new StringBuilder();
      NavigableMap<byte[], byte[]> infoMap =
        res.getFamilyMap(DBConstants.CATALOG_FAMILY);
      for (byte [] e: infoMap.keySet()) {
        if (sb.length() > 0) {
          sb.append(", ");
        }
        sb.append(Bytes.toString(DBConstants.CATALOG_FAMILY) + ":"
            + Bytes.toString(e));
      }
      if (LOG.isWarnEnabled()) {
        LOG.warn(Bytes.toString(DBConstants.CATALOG_FAMILY) + ":" +
          Bytes.toString(DBConstants.REGIONINFO_QUALIFIER)
          + " is empty for row: " + Bytes.toString(row) + "; has keys: "
          + sb.toString());
      }
      return null;
    }
    return Writables.getDBRegionInfo(regioninfo);
  }

  /**
   * When we find rows in a meta region that has an empty DBRegionInfo, we
   * clean them up here.
   *
   * @param s connection to server serving meta region
   * @param metaRegionName name of the meta region we scanned
   * @param emptyRows the row keys that had empty DBRegionInfos
   */
  protected void deleteEmptyMetaRows(DBRegionInterface s,
      byte [] metaRegionName, List<byte []> emptyRows) {
    for (byte [] regionName: emptyRows) {
      try {
        DBRegion.removeRegionFromMETA(s, metaRegionName, regionName);
        if (LOG.isWarnEnabled()) {
          LOG.warn("Removed region: " + Bytes.toString(regionName) +
            " from meta region: " +
            Bytes.toString(metaRegionName) + " because DBRegionInfo was empty");
        }
      } catch (IOException e) {
    	if (LOG.isErrorEnabled()) {
          LOG.error("deleting region: " + Bytes.toString(regionName) +
            " from meta region: " + Bytes.toString(metaRegionName), e);
    	}
      }
    }
  }

  /**
   * @see Watcher#process(WatchedEvent)
   */
  @Override
  public void process(WatchedEvent event) {
	if (LOG.isDebugEnabled()) {
      LOG.debug("Event " + event.getType() + 
              " with state " + event.getState() +  
              " with path " + event.getPath());
	}
    // Master should kill itself if its session expired or if its
    // znode was deleted manually (usually for testing purposes)
    if(event.getState() == PaxosState.Expired ||
      (event.getType().equals(EventType.NodeDeleted) &&
        event.getPath().equals(this.paxosWrapper.getMasterElectionZNode())) &&
        !shutdownRequested.get()) {

      if (LOG.isInfoEnabled())
        LOG.info("Master lost its znode, trying to get a new one");

      // Can we still be the master? If not, goodbye

      paxosWrapper.close();
      try {
        paxosWrapper =
            PaxosWrapper.createInstance(conf, DBMaster.class.getName());
        paxosWrapper.registerListener(this);
        this.paxosMasterAddressWatcher.setPaxos(paxosWrapper);
        if (!this.paxosMasterAddressWatcher.
            writeAddressToPaxos(this.address,false)) {
          throw new Exception("Another Master is currently active");
        }

        // we are a failed over master, reset the fact that we started the 
        // cluster
        resetClusterStartup();
        // Verify the cluster to see if anything happened while we were away
        joinCluster();
      } catch (Exception e) {
    	if (LOG.isErrorEnabled())
          LOG.error("Killing master because of", e);
        System.exit(1);
      }
    }
  }

  @SuppressWarnings("unused")
  private static void printUsageAndExit() {
    System.err.println("Usage: Master [opts] start|stop");
    System.err.println(" start  Start Master. If local mode, start Master and RegionServer in same JVM");
    System.err.println(" stop   Start cluster shutdown; Master signals RegionServer shutdown");
    System.err.println(" where [opts] are:");
    System.err.println("   --minServers=<servers>    Minimum RegionServers needed to host user tables.");
    System.err.println("   -D opt=<value>            Override BigDB configuration settings.");
    System.exit(0);
  }

  /**
   * Utility for constructing an instance of the passed DBMaster class.
   * @param masterClass
   * @param conf
   * @return DBMaster instance.
   */
  public static DBMaster constructMaster(Class<? extends DBMaster> masterClass,
      final Configuration conf)  {
    try {
      Constructor<? extends DBMaster> c =
        masterClass.getConstructor(Configuration.class);
      return c.newInstance(conf);
    } catch (Exception e) {
      throw new RuntimeException("Failed construction of " +
        "Master: " + masterClass.toString() +
        ((e.getCause() != null)? e.getCause().getMessage(): ""), e);
    }
  }

  /**
   * Version of master that will shutdown the passed zk cluster on its way out.
   */
  static class LocalDBMaster extends DBMaster {
    private MiniPaxosCluster paxosCluster = null;

    public LocalDBMaster(Configuration conf) throws IOException {
      super(conf);
    }

    @Override
    public void run() {
      super.run();
      if (this.paxosCluster != null) {
        try {
          this.paxosCluster.shutdown();
        } catch (IOException e) {
          if (LOG.isErrorEnabled())
            LOG.error("paxos shutdown error: " + e.toString(), e);
        }
      }
    }

    void setPaxosCluster(final MiniPaxosCluster paxosCluster) {
      this.paxosCluster = paxosCluster;
    }
  }

  public static interface Instance { 
	  public void shutdown();
  }
  
  public static interface Callback { 
	public void onInstanceCreated(Instance instance);
  }
  
  public static void doMain(Configuration conf, 
		  InputSource source, Callback callback) { 
	doMain(conf, source, callback, DBMaster.class);
  }
  
  private static void doMain(Configuration conf, 
		  InputSource source, Callback callback, 
		  Class<? extends DBMaster> masterClass) {
    /*Configuration conf = ConfigurationFactory.get();

    Options opt = new Options();
    opt.addOption("minServers", true, "Minimum RegionServers needed to host user tables");
    opt.addOption("D", true, "Override BigDB Configuration Settings");
    opt.addOption("backup", false, "Do not try to become DBMaster until the primary fails");
    try {
      CommandLine cmd = new GnuParser().parse(opt, args);

      if (cmd.hasOption("minServers")) {
        String val = cmd.getOptionValue("minServers");
        conf.setInt("bigdb.regions.server.count.min",
            Integer.valueOf(val));
        LOG.debug("minServers set to " + val);
      }

      if (cmd.hasOption("D")) {
        for (String confOpt : cmd.getOptionValues("D")) {
          String[] kv = confOpt.split("=", 2);
          if (kv.length == 2) {
            conf.set(kv[0], kv[1]);
            LOG.debug("-D configuration override: " + kv[0] + "=" + kv[1]);
          } else {
            throw new ParseException("-D option format invalid: " + confOpt);
          }
        }
      }
      
      // check if we are the backup master - override the conf if so
      if (cmd.hasOption("backup")) {
        conf.setBoolean(DBConstants.MASTER_TYPE_BACKUP, true);
      }
	*/
      //if (cmd.equals("start")) {
        try {
          // Print out vm stats before starting up.
          //RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
          //if (runtime != null) {
          //  LOG.info("vmName=" + runtime.getVmName() + ", vmVendor=" +
          //    runtime.getVmVendor() + ", vmVersion=" + runtime.getVmVersion());
          //  LOG.info("vmInputArguments=" + runtime.getInputArguments());
          //}
          
          // If 'local', defer to LocalDBCluster instance.  Starts master
          // and regionserver both in the one JVM.
          if (LocalDBCluster.isLocal(conf)) {
            final MiniPaxosCluster paxosCluster = new MiniPaxosCluster();
            //File zkDataPath = new File(conf.get("bigdb.paxos.property.dataDir"));
            //int zkClientPort = conf.getInt("bigdb.paxos.property.clientPort", 0);
            //if (zkClientPort == 0) {
            //  throw new IOException("No config value for bigdb.paxos.property.clientPort");
            //}
            //paxosCluster.setTickTime(conf.getInt("bigdb.paxos.property.tickTime", 3000));
            //paxosCluster.setClientPort(zkClientPort);
            int clientPort = paxosCluster.startup(source);
            //if (clientPort != zkClientPort) {
            //  String errorMsg = "Couldnt start ZK at requested address of " +
            //      zkClientPort + ", instead got: " + clientPort + ". Aborting. Why? " +
            //      "Because clients (eg shell) wont be able to find this ZK quorum";
            //  System.err.println(errorMsg);
            //  throw new IOException(errorMsg);
            //}
            conf.set("bigdb.paxos.property.clientPort",
              Integer.toString(clientPort));
            
            // Need to have the zk cluster shutdown when master is shutdown.
            // Run a subclass that does the zk cluster shutdown on its way out.
            final LocalDBCluster cluster = new LocalDBCluster(conf, 1,
              LocalDBMaster.class, DBRegionServer.class);
            ((LocalDBMaster)cluster.getMaster()).setPaxosCluster(paxosCluster);
            
            if (callback != null) { 
              callback.onInstanceCreated(new Instance() {
				  @Override
				  public void shutdown() {
					cluster.shutdown();
				  }
        	    });
            }
            cluster.startup();
            
          } else {
            final DBMaster master = constructMaster(masterClass, conf);
            if (master.shutdownRequested.get()) {
              if (LOG.isInfoEnabled())
                LOG.info("Won't bring the Master up as a shutdown is requested");
              return;
            }
            
            if (callback != null) { 
              callback.onInstanceCreated(new Instance() {
			      @Override
			      public void shutdown() {
				    master.shutdown();
			      }
      	        });
            }
            master.start();
          }
        } catch (Throwable t) {
          if (LOG.isErrorEnabled())
            LOG.error("Failed to start master: " + t, t);
          //System.exit(-1);
        }
      //} else if (cmd.equals("stop")) {
      //  DBAdmin adm = null;
      //  try {
      //    adm = new DBAdmin(conf);
      //  } catch (MasterNotRunningException e) {
      //    LOG.error("Master not running");
      //    //System.exit(0);
      //  }
      //  try {
      //    adm.shutdown();
      //  } catch (Throwable t) {
      //    LOG.error("Failed to stop master", t);
      //    //System.exit(-1);
      //  }
      //} else {
      //  throw new ParseException("Unknown argument(s): " +
      //      org.apache.commons.lang.StringUtils.join(cmd.getArgs(), " "));
      //}
    //} catch (ParseException e) {
    //  LOG.error("Could not parse: ", e);
    //  printUsageAndExit();
    //}
  }

  public static void doStop(Configuration conf) { 
    DBAdmin adm = null;
    try {
      adm = new DBAdmin(conf);
    } catch (MasterNotRunningException e) {
      if (LOG.isErrorEnabled())
        LOG.error("Master not running: " + e);
      //System.exit(0);
    }
    try {
      adm.shutdown();
    } catch (Throwable t) {
      if (LOG.isErrorEnabled())
        LOG.error("Failed to stop master: " + t, t);
      //System.exit(-1);
    }
  }
  
  public Map<String, Integer> getTableFragmentation() throws IOException {
    long now = System.currentTimeMillis();
    // only check every two minutes by default
    int check = this.conf.getInt("bigdb.master.fragmentation.check.frequency", 2 * 60 * 1000);
    if (lastFragmentationQuery == -1 || now - lastFragmentationQuery > check) {
      fragmentation = FSUtils.getTableFragmentation(this);
      lastFragmentationQuery = now;
    }
    return fragmentation;
  }

  /**
   * Main program
   * @param args
   */
  //public static void main(String [] args) {
    //doMain(args, DBMaster.class);
  //}
}

