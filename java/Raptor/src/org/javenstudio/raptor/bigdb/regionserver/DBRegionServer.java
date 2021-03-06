package org.javenstudio.raptor.bigdb.regionserver;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Constructor;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//import org.apache.commons.cli.CommandLine;
//import org.apache.commons.cli.GnuParser;
//import org.apache.commons.cli.Options;
//import org.apache.commons.cli.ParseException;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.MapWritable;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.net.DNS;
import org.javenstudio.raptor.util.Progressable;
import org.javenstudio.raptor.util.StringUtils;
import org.javenstudio.raptor.bigdb.Chore;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBMsg;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.DBRegionLocation;
import org.javenstudio.raptor.bigdb.DBServerAddress;
import org.javenstudio.raptor.bigdb.DBServerInfo;
import org.javenstudio.raptor.bigdb.DBServerLoad;
import org.javenstudio.raptor.bigdb.DBTableDescriptor;
import org.javenstudio.raptor.bigdb.KeyValue;
import org.javenstudio.raptor.bigdb.LeaseListener;
import org.javenstudio.raptor.bigdb.Leases;
import org.javenstudio.raptor.bigdb.NotServingRegionException;
import org.javenstudio.raptor.bigdb.RemoteExceptionHandler;
import org.javenstudio.raptor.bigdb.UnknownRowLockException;
import org.javenstudio.raptor.bigdb.UnknownScannerException;
import org.javenstudio.raptor.bigdb.YouAreDeadException;
import org.javenstudio.raptor.bigdb.DBConstants.OperationStatusCode;
import org.javenstudio.raptor.bigdb.DBMsg.Type;
import org.javenstudio.raptor.bigdb.Leases.LeaseStillHeldException;
import org.javenstudio.raptor.bigdb.client.Delete;
import org.javenstudio.raptor.bigdb.client.Get;
import org.javenstudio.raptor.bigdb.client.MultiPut;
import org.javenstudio.raptor.bigdb.client.MultiPutResponse;
import org.javenstudio.raptor.bigdb.client.Put;
import org.javenstudio.raptor.bigdb.client.Result;
import org.javenstudio.raptor.bigdb.client.Scan;
import org.javenstudio.raptor.bigdb.client.ServerConnection;
import org.javenstudio.raptor.bigdb.client.ServerConnectionManager;
import org.javenstudio.raptor.bigdb.io.dbfile.LruBlockCache;
import org.javenstudio.raptor.bigdb.ipc.DBRPC;
import org.javenstudio.raptor.bigdb.ipc.DBRPCErrorHandler;
import org.javenstudio.raptor.bigdb.ipc.DBRPCProtocolVersion;
import org.javenstudio.raptor.bigdb.ipc.DBServer;
import org.javenstudio.raptor.bigdb.ipc.DBMasterRegionInterface;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.regionserver.metrics.RegionServerMetrics;
import org.javenstudio.raptor.bigdb.regionserver.wal.DBLog;
import org.javenstudio.raptor.bigdb.replication.regionserver.Replication;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.bigdb.util.FSUtils;
import org.javenstudio.raptor.bigdb.util.InfoServer;
import org.javenstudio.raptor.bigdb.util.Pair;
import org.javenstudio.raptor.bigdb.util.Sleeper;
import org.javenstudio.raptor.bigdb.util.Threads;
import org.javenstudio.raptor.bigdb.paxos.PaxosWrapper;
import org.javenstudio.raptor.paxos.WatchedEvent;
import org.javenstudio.raptor.paxos.Watcher;
import org.javenstudio.raptor.paxos.Watcher.Event.EventType;
import org.javenstudio.raptor.paxos.Watcher.Event.PaxosState;

/**
 * DBRegionServer makes a set of DBRegions available to clients.  It checks in with
 * the DBMaster. There are many DBRegionServers in a single BigDB deployment.
 */
public class DBRegionServer implements DBRegionInterface,
    DBRPCErrorHandler, Runnable, Watcher, Stoppable, OnlineRegions {
  public static final Logger LOG = Logger.getLogger(DBRegionServer.class);
  private static final DBMsg REPORT_EXITING = new DBMsg(Type.MSG_REPORT_EXITING);
  private static final DBMsg REPORT_QUIESCED = new DBMsg(Type.MSG_REPORT_QUIESCED);
  private static final DBMsg [] EMPTY_HMSG_ARRAY = new DBMsg [] {};

  // Set when a report to the master comes back with a message asking us to
  // shutdown.  Also set by call to stop when debugging or running unit tests
  // of DBRegionServer in isolation. We use AtomicBoolean rather than
  // plain boolean so we can pass a reference to Chore threads.  Otherwise,
  // Chore threads need to know about the hosting class.
  protected final AtomicBoolean stopRequested = new AtomicBoolean(false);

  protected final AtomicBoolean quiesced = new AtomicBoolean(false);

  // Go down hard.  Used if file system becomes unavailable and also in
  // debugging and unit tests.
  protected volatile boolean abortRequested;

  private volatile boolean killed = false;

  // If false, the file system has become unavailable
  protected volatile boolean fsOk;

  protected DBServerInfo serverInfo;
  protected final Configuration conf;

  private final ServerConnection connection;
  protected final AtomicBoolean haveRootRegion = new AtomicBoolean(false);
  private FileSystem fs;
  private Path rootDir;
  private final Random rand = new Random();

  // Key is Bytes.hashCode of region name byte array and the value is DBRegion
  // in both of the maps below.  Use Bytes.mapKey(byte []) generating key for
  // below maps.
  protected final Map<Integer, DBRegion> onlineRegions =
    new ConcurrentHashMap<Integer, DBRegion>();

  protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final LinkedBlockingQueue<DBMsg> outboundMsgs =
    new LinkedBlockingQueue<DBMsg>();

  final int numRetries;
  protected final int threadWakeFrequency;
  private final int msgInterval;

  protected final int numRegionsToReport;

  private final long maxScannerResultSize;

  // Remote DBMaster
  private DBMasterRegionInterface bigdbMaster;

  // Server to handle client requests.  Default access so can be accessed by
  // unit tests.
  DBServer server;

  // Leases
  private Leases leases;

  // Request counter
  private volatile AtomicInteger requestCount = new AtomicInteger();

  // Info server.  Default access so can be used by unit tests.  REGIONSERVER
  // is name of the webapp and the attribute name used stuffing this instance
  // into web context.
  InfoServer infoServer;

  /** region server process name */
  public static final String REGIONSERVER = "regionserver";

  /*
   * Space is reserved in HRS constructor and then released when aborting
   * to recover from an OOME. See BIGDB-706.  TODO: Make this percentage of the
   * heap or a minimum.
   */
  private final LinkedList<byte[]> reservedSpace = new LinkedList<byte []>();

  private RegionServerMetrics metrics;

  // Compactions
  CompactSplitThread compactSplitThread;

  // Cache flushing
  MemStoreFlusher cacheFlusher;

  /* Check for major compactions.
   */
  Chore majorCompactionChecker;

  // DBLog and DBLog roller.  log is protected rather than private to avoid
  // eclipse warning when accessed by inner classes
  protected volatile DBLog hlog;
  LogRoller hlogRoller;

  // flag set after we're done setting up server threads (used for testing)
  protected volatile boolean isOnline;

  final Map<String, InternalScanner> scanners =
    new ConcurrentHashMap<String, InternalScanner>();

  private PaxosWrapper paxosWrapper;

  // A sleeper that sleeps for msgInterval.
  private final Sleeper sleeper;

  private final long rpcTimeout;

  // Address passed in to constructor.  This is not always the address we run
  // with.  For example, if passed port is 0, then we are to pick a port.  The
  // actual address we run with is in the #serverInfo data member.
  private final DBServerAddress address;

  // The main region server thread.
  private Thread regionServerThread;

  private final String machineName;

  // Replication-related attributes
  private Replication replicationHandler;
  // End of replication

  /**
   * Starts a DBRegionServer at the default location
   * @param conf
   * @throws IOException
   */
  public DBRegionServer(Configuration conf) throws IOException {
    machineName = DNS.getDefaultHost(
        conf.get("bigdb.regionserver.dns.interface","default"),
        conf.get("bigdb.regionserver.dns.nameserver","default"));
    String addressStr = machineName + ":" +
      conf.get(DBConstants.REGIONSERVER_PORT,
          Integer.toString(DBConstants.DEFAULT_REGIONSERVER_PORT));
    // This is not necessarily the address we will run with.  The address we
    // use will be in #serverInfo data member.  For example, we may have been
    // passed a port of 0 which means we should pick some ephemeral port to bind
    // to.
    address = new DBServerAddress(addressStr);
    LOG.info("My address is " + address);

    this.abortRequested = false;
    this.fsOk = true;
    this.conf = conf;
    this.connection = ServerConnectionManager.getConnection(conf);

    this.isOnline = false;

    // Config'ed params
    this.numRetries =  conf.getInt("bigdb.client.retries.number", 2);
    this.threadWakeFrequency = conf.getInt(DBConstants.THREAD_WAKE_FREQUENCY,
        10 * 1000);
    this.msgInterval = conf.getInt("bigdb.regionserver.msginterval", 1 * 1000);

    sleeper = new Sleeper(this.msgInterval, this.stopRequested);

    this.maxScannerResultSize = conf.getLong(
            DBConstants.BIGDB_CLIENT_SCANNER_MAX_RESULT_SIZE_KEY,
            DBConstants.DEFAULT_BIGDB_CLIENT_SCANNER_MAX_RESULT_SIZE);

    // Task thread to process requests from Master
    this.worker = new Worker();

    this.numRegionsToReport =
      conf.getInt("bigdb.regionserver.numregionstoreport", 10);

    this.rpcTimeout =
      conf.getLong(DBConstants.BIGDB_REGIONSERVER_LEASE_PERIOD_KEY,
          DBConstants.DEFAULT_BIGDB_REGIONSERVER_LEASE_PERIOD);

    reinitialize();
  }

  /**
   * Creates all of the state that needs to be reconstructed in case we are
   * doing a restart. This is shared between the constructor and restart().
   * Both call it.
   * @throws IOException
   */
  private void reinitialize() throws IOException {
    this.abortRequested = false;
    this.stopRequested.set(false);

    // Server to handle client requests
    this.server = DBRPC.getServer(this,
        new Class<?>[]{DBRegionInterface.class, DBRPCErrorHandler.class,
        OnlineRegions.class}, address.getBindAddress(),
        address.getPort(), conf.getInt("bigdb.regionserver.handler.count", 10),
        false, conf);
    this.server.setErrorHandler(this);
    // Address is giving a default IP for the moment. Will be changed after
    // calling the master.
    this.serverInfo = new DBServerInfo(new DBServerAddress(
      new InetSocketAddress(address.getBindAddress(),
      this.server.getListenerAddress().getPort())), System.currentTimeMillis(),
      this.conf.getInt("bigdb.regionserver.info.port", 60030), machineName);
    if (this.serverInfo.getServerAddress() == null) {
      throw new NullPointerException("Server address cannot be null; " +
        "bigdb-958 debugging");
    }
    reinitializeThreads();
    reinitializePaxos();
    int nbBlocks = conf.getInt("bigdb.regionserver.nbreservationblocks", 4);
    for(int i = 0; i < nbBlocks; i++)  {
      reservedSpace.add(new byte[DBConstants.DEFAULT_SIZE_RESERVATION_BLOCK]);
    }
  }

  private void reinitializePaxos() throws IOException {
    paxosWrapper =
        PaxosWrapper.createInstance(conf, serverInfo.getServerName());
    paxosWrapper.registerListener(this);
    watchMasterAddress();
  }

  private void reinitializeThreads() {
    this.workerThread = new Thread(worker);

    // Cache flushing thread.
    this.cacheFlusher = new MemStoreFlusher(conf, this);

    // Compaction thread
    this.compactSplitThread = new CompactSplitThread(this);

    // Log rolling thread
    this.hlogRoller = new LogRoller(this);

    // Background thread to check for major compactions; needed if region
    // has not gotten updates in a while.  Make it run at a lesser frequency.
    int multiplier = this.conf.getInt(DBConstants.THREAD_WAKE_FREQUENCY +
        ".multiplier", 1000);
    this.majorCompactionChecker = new MajorCompactionChecker(this,
      this.threadWakeFrequency * multiplier,  this.stopRequested);

    this.leases = new Leases(
        (int) conf.getLong(DBConstants.BIGDB_REGIONSERVER_LEASE_PERIOD_KEY,
            DBConstants.DEFAULT_BIGDB_REGIONSERVER_LEASE_PERIOD),
        this.threadWakeFrequency);
  }

  /**
   * We register ourselves as a watcher on the master address ZNode. This is
   * called by Paxos when we get an event on that ZNode. When this method
   * is called it means either our master has died, or a new one has come up.
   * Either way we need to update our knowledge of the master.
   * @param event WatchedEvent from Paxos.
   */
  public void process(WatchedEvent event) {
    EventType type = event.getType();
    PaxosState state = event.getState();
    LOG.info("Got Paxos event, state: " + state + ", type: " +
      type + ", path: " + event.getPath());

    // Ignore events if we're shutting down.
    if (this.stopRequested.get()) {
      LOG.debug("Ignoring Paxos event while shutting down");
      return;
    }

    if (state == PaxosState.Expired) {
      LOG.error("Paxos session expired");
      boolean restart =
        this.conf.getBoolean("bigdb.regionserver.restart.on.zk.expire", false);
      if (restart) {
        restart();
      } else {
        abort("Paxos session expired");
      }
    } else if (type == EventType.NodeDeleted) {
      watchMasterAddress();
    } else if (type == EventType.NodeCreated) {
      getMaster();

      // Paxos watches are one time only, so we need to re-register our watch.
      watchMasterAddress();
    }
  }

  private void watchMasterAddress() {
    while (!stopRequested.get() && !paxosWrapper.watchMasterAddress(this)) {
      LOG.warn("Unable to set watcher on Paxos master address. Retrying.");
      sleeper.sleep();
    }
  }

  private void restart() {
    abort("Restarting region server");
    Threads.shutdown(regionServerThread);
    boolean done = false;
    while (!done) {
      try {
        reinitialize();
        done = true;
      } catch (IOException e) {
        LOG.debug("Error trying to reinitialize Paxos", e);
      }
    }
    Thread t = new Thread(this);
    String name = regionServerThread.getName();
    t.setName(name);
    t.start();
  }

  /** @return PaxosWrapper used by RegionServer. */
  public PaxosWrapper getPaxosWrapper() {
    return paxosWrapper;
  }

  /**
   * The DBRegionServer sticks in this loop until closed. It repeatedly checks
   * in with the DBMaster, sending heartbeats & reports, and receiving DBRegion
   * load/unload instructions.
   */
  public void run() {
    regionServerThread = Thread.currentThread();
    boolean quiesceRequested = false;
    try {
      MapWritable w = null;
      while (!stopRequested.get()) {
        w = reportForDuty();
        if (w != null) {
          init(w);
          break;
        }
        sleeper.sleep();
        LOG.warn("No response from master on reportForDuty. Sleeping and " +
          "then trying again.");
      }
      List<DBMsg> outboundMessages = new ArrayList<DBMsg>();
      long lastMsg = 0;
      // Now ask master what it wants us to do and tell it what we have done
      for (int tries = 0; !stopRequested.get() && isHealthy();) {
        // Try to get the root region location from the master.
        if (!haveRootRegion.get()) {
          DBServerAddress rootServer = paxosWrapper.readRootRegionLocation();
          if (rootServer != null) {
            // By setting the root region location, we bypass the wait imposed on
            // HTable for all regions being assigned.
            this.connection.setRootRegionLocation(
                new DBRegionLocation(DBRegionInfo.ROOT_REGIONINFO, rootServer));
            haveRootRegion.set(true);
          }
        }
        long now = System.currentTimeMillis();
        // Drop into the send loop if msgInterval has elapsed or if something
        // to send.  If we fail talking to the master, then we'll sleep below
        // on poll of the outboundMsgs blockingqueue.
        if ((now - lastMsg) >= msgInterval || !outboundMessages.isEmpty()) {
          try {
            doMetrics();
            MemoryUsage memory =
              ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            DBServerLoad hsl = new DBServerLoad(requestCount.get(),
              (int)(memory.getUsed()/1024/1024),
              (int)(memory.getMax()/1024/1024));
            for (DBRegion r: onlineRegions.values()) {
              hsl.addRegionInfo(createRegionLoad(r));
            }
            this.serverInfo.setLoad(hsl);
            this.requestCount.set(0);
            addOutboundMsgs(outboundMessages);
            DBMsg msgs[] = this.bigdbMaster.regionServerReport(
              serverInfo, outboundMessages.toArray(EMPTY_HMSG_ARRAY),
              getMostLoadedRegions());
            lastMsg = System.currentTimeMillis();
            updateOutboundMsgs(outboundMessages);
            outboundMessages.clear();
            if (this.quiesced.get() && onlineRegions.size() == 0) {
              // We've just told the master we're exiting because we aren't
              // serving any regions. So set the stop bit and exit.
              LOG.info("Server quiesced and not serving any regions. " +
                "Starting shutdown");
              stopRequested.set(true);
              this.outboundMsgs.clear();
              continue;
            }

            // Queue up the DBMaster's instruction stream for processing
            boolean restart = false;
            for(int i = 0;
                !restart && !stopRequested.get() && i < msgs.length;
                i++) {
              LOG.info(msgs[i].toString());
              this.connection.unsetRootRegionLocation();
              switch(msgs[i].getType()) {

              case MSG_REGIONSERVER_STOP:
                stopRequested.set(true);
                break;

              case MSG_REGIONSERVER_QUIESCE:
                if (!quiesceRequested) {
                  try {
                    toDo.put(new ToDoEntry(msgs[i]));
                  } catch (InterruptedException e) {
                    throw new RuntimeException("Putting into msgQueue was " +
                        "interrupted.", e);
                  }
                  quiesceRequested = true;
                }
                break;

              default:
                if (fsOk) {
                  try {
                    toDo.put(new ToDoEntry(msgs[i]));
                  } catch (InterruptedException e) {
                    throw new RuntimeException("Putting into msgQueue was " +
                        "interrupted.", e);
                  }
                }
              }
            }
            // Reset tries count if we had a successful transaction.
            tries = 0;

            if (restart || this.stopRequested.get()) {
              toDo.clear();
              continue;
            }
          } catch (Exception e) { // FindBugs REC_CATCH_EXCEPTION
            // Two special exceptions could be printed out here,
            // PleaseHoldException and YouAreDeadException
            if (e instanceof IOException) {
              e = RemoteExceptionHandler.checkIOException((IOException) e);
            }
            if (e instanceof YouAreDeadException) {
              // This will be caught and handled as a fatal error below
              throw e;
            }
            tries++;
            if (tries > 0 && (tries % this.numRetries) == 0) {
              // Check filesystem every so often.
              checkFileSystem();
            }
            if (this.stopRequested.get()) {
              LOG.info("Stop requested, clearing toDo despite exception");
              toDo.clear();
              continue;
            }
            LOG.warn("Attempt=" + tries, e);
            // No point retrying immediately; this is probably connection to
            // master issue.  Doing below will cause us to sleep.
            lastMsg = System.currentTimeMillis();
          }
        }
        now = System.currentTimeMillis();
        DBMsg msg = this.outboundMsgs.poll((msgInterval - (now - lastMsg)),
          TimeUnit.MILLISECONDS);
        // If we got something, add it to list of things to send.
        if (msg != null) outboundMessages.add(msg);
        // Do some housekeeping before going back around
        housekeeping();
      } // for
    } catch (Throwable t) {
      if (!checkOOME(t)) {
        abort("Unhandled exception", t);
      }
    }
    this.leases.closeAfterLeasesExpire();
    this.worker.stop();
    this.server.stop();
    if (this.infoServer != null) {
      LOG.info("Stopping infoServer");
      try {
        this.infoServer.stop();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    // Send cache a shutdown.
    LruBlockCache c = (LruBlockCache)StoreFile.getBlockCache(this.conf);
    if (c != null) c.shutdown();

    // Send interrupts to wake up threads if sleeping so they notice shutdown.
    // TODO: Should we check they are alive?  If OOME could have exited already
    cacheFlusher.interruptIfNecessary();
    compactSplitThread.interruptIfNecessary();
    hlogRoller.interruptIfNecessary();
    this.majorCompactionChecker.interrupt();

    if (killed) {
      // Just skip out w/o closing regions.
    } else if (abortRequested) {
      if (this.fsOk) {
        // Only try to clean up if the file system is available
        try {
          if (this.hlog != null) {
            this.hlog.close();
            LOG.info("On abort, closed hlog");
          }
        } catch (Throwable e) {
          LOG.error("Unable to close log in abort",
            RemoteExceptionHandler.checkThrowable(e));
        }
        closeAllRegions(); // Don't leave any open file handles
      }
      LOG.info("aborting server at: " + this.serverInfo.getServerName());
    } else {
      ArrayList<DBRegion> closedRegions = closeAllRegions();
      try {
        if (this.hlog != null) {
          hlog.closeAndDelete();
        }
      } catch (Throwable e) {
        LOG.error("Close and delete failed",
          RemoteExceptionHandler.checkThrowable(e));
      }
      try {
        DBMsg[] exitMsg = new DBMsg[closedRegions.size() + 1];
        exitMsg[0] = REPORT_EXITING;
        // Tell the master what regions we are/were serving
        int i = 1;
        for (DBRegion region: closedRegions) {
          exitMsg[i++] = new DBMsg(DBMsg.Type.MSG_REPORT_CLOSE,
              region.getRegionInfo());
        }

        LOG.info("telling master that region server is shutting down at: " +
            serverInfo.getServerName());
        bigdbMaster.regionServerReport(serverInfo, exitMsg, (DBRegionInfo[])null);
      } catch (Throwable e) {
        LOG.warn("Failed to send exiting message to master: ",
          RemoteExceptionHandler.checkThrowable(e));
      }
      LOG.info("stopping server at: " + this.serverInfo.getServerName());
    }

    // Make sure the proxy is down.
    if (this.bigdbMaster != null) {
      DBRPC.stopProxy(this.bigdbMaster);
      this.bigdbMaster = null;
    }

    if (!killed) {
      this.paxosWrapper.close();
      join();
    }
    LOG.info(Thread.currentThread().getName() + " exiting");
  }

  /*
   * Add to the passed <code>msgs</code> messages to pass to the master.
   * @param msgs Current outboundMsgs array; we'll add messages to this List.
   */
  private void addOutboundMsgs(final List<DBMsg> msgs) {
    if (msgs.isEmpty()) {
      this.outboundMsgs.drainTo(msgs);
      return;
    }
    OUTER: for (DBMsg m: this.outboundMsgs) {
      for (DBMsg mm: msgs) {
        // Be careful don't add duplicates.
        if (mm.equals(m)) {
          continue OUTER;
        }
      }
      msgs.add(m);
    }
  }

  /*
   * Remove from this.outboundMsgs those messsages we sent the master.
   * @param msgs Messages we sent the master.
   */
  private void updateOutboundMsgs(final List<DBMsg> msgs) {
    if (msgs.isEmpty()) return;
    for (DBMsg m: this.outboundMsgs) {
      for (DBMsg mm: msgs) {
        if (mm.equals(m)) {
          this.outboundMsgs.remove(m);
          break;
        }
      }
    }
  }

  /*
   * Run init. Sets up hlog and starts up all server threads.
   * @param c Extra configuration.
   */
  protected void init(final MapWritable c) throws IOException {
    try {
      for (Map.Entry<Writable, Writable> e: c.entrySet()) {
        String key = e.getKey().toString();
        String value = e.getValue().toString();
        if (LOG.isDebugEnabled()) {
          LOG.debug("Config from master: " + key + "=" + value);
        }
        this.conf.set(key, value);
      }
      // Master may have sent us a new address with the other configs.
      // Update our address in this case. See BIGDB-719
      String hra = conf.get("bigdb.regionserver.address");
      // TODO: The below used to be this.address != null.  Was broken by what
      // looks like a mistake in:
      //
      // BIGDB-1215 migration; metautils scan of meta region was broken; wouldn't see first row
      // ------------------------------------------------------------------------
      // r796326 | stack | 2009-07-21 07:40:34 -0700 (Tue, 21 Jul 2009) | 38 lines
      if (hra != null) {
        DBServerAddress hsa = new DBServerAddress (hra,
          this.serverInfo.getServerAddress().getPort());
        LOG.info("Master passed us address to use. Was=" +
          this.serverInfo.getServerAddress() + ", Now=" + hra);
        this.serverInfo.setServerAddress(hsa);
      }

      // hack! Maps DFSClient => RegionServer for logs.  HDFS made this
      // config param for task trackers, but we can piggyback off of it.
      if (this.conf.get("mapred.task.id") == null) {
        this.conf.set("mapred.task.id", 
            "hb_rs_" + this.serverInfo.getServerName() + "_" +
            System.currentTimeMillis());
      }

      // Master sent us bigdb.rootdir to use. Should be fully qualified
      // path with file system specification included.  Set 'fs.defaultFS'
      // to match the filesystem on bigdb.rootdir else underlying hadoop hdfs
      // accessors will be going against wrong filesystem (unless all is set
      // to defaults).
      this.conf.set("fs.defaultFS", this.conf.get("bigdb.rootdir"));
      // Get fs instance used by this RS
      this.fs = FSUtils.getFs(this.conf);
      this.rootDir = new Path(this.conf.get(DBConstants.BIGDB_DIR));
      this.hlog = setupDBLog();
      // Init in here rather than in constructor after thread name has been set
      this.metrics = new RegionServerMetrics();
      startServiceThreads();
      isOnline = true;
    } catch (Throwable e) {
      this.isOnline = false;
      this.stopRequested.set(true);
      throw convertThrowableToIOE(cleanup(e, "Failed init"),
        "Region server startup failed");
    }
  }

  /**
   * @param r Region to get RegionLoad for.
   * @return RegionLoad instance.
   * @throws IOException
   */
  private DBServerLoad.RegionLoad createRegionLoad(final DBRegion r) {
    byte[] name = r.getRegionName();
    int stores = 0;
    int storefiles = 0;
    int storefileSizeMB = 0;
    int memstoreSizeMB = (int)(r.memstoreSize.get()/1024/1024);
    int storefileIndexSizeMB = 0;
    synchronized (r.stores) {
      stores += r.stores.size();
      for (Store store: r.stores.values()) {
        storefiles += store.getStorefilesCount();
        storefileSizeMB +=
          (int)(store.getStorefilesSize()/1024/1024);
        storefileIndexSizeMB +=
          (int)(store.getStorefilesIndexSize()/1024/1024);
      }
    }
    return new DBServerLoad.RegionLoad(name, stores, storefiles,
      storefileSizeMB, memstoreSizeMB, storefileIndexSizeMB);
  }

  /**
   * @param regionName
   * @return An instance of RegionLoad.
   * @throws IOException
   */
  public DBServerLoad.RegionLoad createRegionLoad(final byte [] regionName) {
    return createRegionLoad(this.onlineRegions.get(Bytes.mapKey(regionName)));
  }

  /*
   * Cleanup after Throwable caught invoking method.  Converts <code>t</code>
   * to IOE if it isn't already.
   * @param t Throwable
   * @return Throwable converted to an IOE; methods can only let out IOEs.
   */
  private Throwable cleanup(final Throwable t) {
    return cleanup(t, null);
  }

  /*
   * Cleanup after Throwable caught invoking method.  Converts <code>t</code>
   * to IOE if it isn't already.
   * @param t Throwable
   * @param msg Message to log in error.  Can be null.
   * @return Throwable converted to an IOE; methods can only let out IOEs.
   */
  private Throwable cleanup(final Throwable t, final String msg) {
    // Don't log as error if NSRE; NSRE is 'normal' operation.
    if (t instanceof NotServingRegionException) {
      LOG.debug("NotServingRegionException; " +  t.getMessage());
      return t;
    }
    if (msg == null) {
      LOG.error("", RemoteExceptionHandler.checkThrowable(t));
    } else {
      LOG.error(msg, RemoteExceptionHandler.checkThrowable(t));
    }
    if (!checkOOME(t)) {
      checkFileSystem();
    }
    return t;
  }

  /*
   * @param t
   * @return Make <code>t</code> an IOE if it isn't already.
   */
  private IOException convertThrowableToIOE(final Throwable t) {
    return convertThrowableToIOE(t, null);
  }

  /*
   * @param t
   * @param msg Message to put in new IOE if passed <code>t</code> is not an IOE
   * @return Make <code>t</code> an IOE if it isn't already.
   */
  private IOException convertThrowableToIOE(final Throwable t,
      final String msg) {
    return (t instanceof IOException? (IOException)t:
      msg == null || msg.length() == 0?
        new IOException(t): new IOException(msg, t));
  }
  /*
   * Check if an OOME and if so, call abort.
   * @param e
   * @return True if we OOME'd and are aborting.
   */
  public boolean checkOOME(final Throwable e) {
    boolean stop = false;
    if (e instanceof OutOfMemoryError ||
      (e.getCause() != null && e.getCause() instanceof OutOfMemoryError) ||
      (e.getMessage() != null &&
        e.getMessage().contains("java.lang.OutOfMemoryError"))) {
      abort("OutOfMemoryError, aborting", e);
      stop = true;
    }
    return stop;
  }


  /**
   * Checks to see if the file system is still accessible.
   * If not, sets abortRequested and stopRequested
   *
   * @return false if file system is not available
   */
  protected boolean checkFileSystem() {
    if (this.fsOk && this.fs != null) {
      try {
        FSUtils.checkFileSystemAvailable(this.fs);
      } catch (IOException e) {
        abort("File System not available", e);
        this.fsOk = false;
      }
    }
    return this.fsOk;
  }

  /*
   * Inner class that runs on a long period checking if regions need major
   * compaction.
   */
  private static class MajorCompactionChecker extends Chore {
    private final DBRegionServer instance;

    MajorCompactionChecker(final DBRegionServer h,
        final int sleepTime, final AtomicBoolean stopper) {
      super("MajorCompactionChecker", sleepTime, stopper);
      this.instance = h;
      LOG.info("Runs every " + sleepTime + "ms");
    }

    @Override
    protected void chore() {
      Set<Integer> keys = this.instance.onlineRegions.keySet();
      for (Integer i: keys) {
        DBRegion r = this.instance.onlineRegions.get(i);
        try {
          if (r != null && r.isMajorCompaction()) {
            // Queue a compaction.  Will recognize if major is needed.
            this.instance.compactSplitThread.
              compactionRequested(r, getName() + " requests major compaction");
          }
        } catch (IOException e) {
          LOG.warn("Failed major compaction check on " + r, e);
        }
      }
    }
  }

  /**
   * Report the status of the server. A server is online once all the startup
   * is completed (setting up filesystem, starting service threads, etc.). This
   * method is designed mostly to be useful in tests.
   * @return true if online, false if not.
   */
  public boolean isOnline() {
    return isOnline;
  }

  private DBLog setupDBLog() throws IOException {
    final Path oldLogDir = new Path(rootDir, DBConstants.DBREGION_OLDLOGDIR_NAME);
    Path logdir = new Path(rootDir, DBLog.getDBLogDirectoryName(this.serverInfo));
    if (LOG.isDebugEnabled()) {
      LOG.debug("Log dir " + logdir);
    }
    if (fs.exists(logdir)) {
      throw new RegionServerRunningException("region server already " +
        "running at " + this.serverInfo.getServerName() +
        " because logdir " + logdir.toString() + " exists");
    }
    this.replicationHandler = new Replication(this.conf,this.serverInfo,
        this.fs, logdir, oldLogDir, stopRequested);
    DBLog log = instantiateDBLog(logdir, oldLogDir);
    this.replicationHandler.addLogEntryVisitor(log);
    return log;
  }

  // instantiate
  protected DBLog instantiateDBLog(Path logdir, Path oldLogDir) throws IOException {
    return new DBLog(this.fs, logdir, oldLogDir, this.conf, this.hlogRoller,
      this.replicationHandler.getReplicationManager(),
        this.serverInfo.getServerAddress().toString());
  }


  protected LogRoller getLogRoller() {
    return hlogRoller;
  }

  /*
   * @param interval Interval since last time metrics were called.
   */
  protected void doMetrics() {
    try {
      metrics();
    } catch (Throwable e) {
      LOG.warn("Failed metrics", e);
    }
  }

  protected void metrics() {
    this.metrics.regions.set(this.onlineRegions.size());
    this.metrics.incrementRequests(this.requestCount.get());
    // Is this too expensive every three seconds getting a lock on onlineRegions
    // and then per store carried?  Can I make metrics be sloppier and avoid
    // the synchronizations?
    int stores = 0;
    int storefiles = 0;
    long memstoreSize = 0;
    long storefileIndexSize = 0;
    synchronized (this.onlineRegions) {
      for (Map.Entry<Integer, DBRegion> e: this.onlineRegions.entrySet()) {
        DBRegion r = e.getValue();
        memstoreSize += r.memstoreSize.get();
        synchronized (r.stores) {
          stores += r.stores.size();
          for(Map.Entry<byte [], Store> ee: r.stores.entrySet()) {
            Store store = ee.getValue();
            storefiles += store.getStorefilesCount();
            storefileIndexSize += store.getStorefilesIndexSize();
          }
        }
      }
    }
    this.metrics.stores.set(stores);
    this.metrics.storefiles.set(storefiles);
    this.metrics.memstoreSizeMB.set((int)(memstoreSize/(1024*1024)));
    this.metrics.storefileIndexSizeMB.set((int)(storefileIndexSize/(1024*1024)));
    this.metrics.compactionQueueSize.set(compactSplitThread.
      getCompactionQueueSize());

    LruBlockCache lruBlockCache = (LruBlockCache)StoreFile.getBlockCache(conf);
    if (lruBlockCache != null) {
      this.metrics.blockCacheCount.set(lruBlockCache.size());
      this.metrics.blockCacheFree.set(lruBlockCache.getFreeSize());
      this.metrics.blockCacheSize.set(lruBlockCache.getCurrentSize());
      double ratio = lruBlockCache.getStats().getHitRatio();
      int percent = (int) (ratio * 100);
      this.metrics.blockCacheHitRatio.set(percent);
    }
  }

  /**
   * @return Region server metrics instance.
   */
  public RegionServerMetrics getMetrics() {
    return this.metrics;
  }

  /*
   * Start maintanence Threads, Server, Worker and lease checker threads.
   * Install an UncaughtExceptionHandler that calls abort of RegionServer if we
   * get an unhandled exception.  We cannot set the handler on all threads.
   * Server's internal Listener thread is off limits.  For Server, if an OOME,
   * it waits a while then retries.  Meantime, a flush or a compaction that
   * tries to run should trigger same critical condition and the shutdown will
   * run.  On its way out, this server will shut down Server.  Leases are sort
   * of inbetween. It has an internal thread that while it inherits from
   * Chore, it keeps its own internal stop mechanism so needs to be stopped
   * by this hosting server.  Worker logs the exception and exits.
   */
  private void startServiceThreads() throws IOException {
    String n = Thread.currentThread().getName();
    UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {
      public void uncaughtException(Thread t, Throwable e) {
        abort("Uncaught exception in service thread " + t.getName(), e);
      }
    };
    Threads.setDaemonThreadRunning(this.hlogRoller, n + ".logRoller",
        handler);
    Threads.setDaemonThreadRunning(this.cacheFlusher, n + ".cacheFlusher",
      handler);
    Threads.setDaemonThreadRunning(this.compactSplitThread, n + ".compactor",
        handler);
    Threads.setDaemonThreadRunning(this.workerThread, n + ".worker", handler);
    Threads.setDaemonThreadRunning(this.majorCompactionChecker,
        n + ".majorCompactionChecker", handler);

    // Leases is not a Thread. Internally it runs a daemon thread.  If it gets
    // an unhandled exception, it will just exit.
    this.leases.setName(n + ".leaseChecker");
    this.leases.start();
    // Put up info server.
    int port = this.conf.getInt("bigdb.regionserver.info.port", 60030);
    // -1 is for disabling info server
    if (port >= 0) {
      String addr = this.conf.get("bigdb.regionserver.info.bindAddress", "0.0.0.0");
      // check if auto port bind enabled
      boolean auto = this.conf.getBoolean("bigdb.regionserver.info.port.auto",
          false);
      while (true) {
        try {
          this.infoServer = new InfoServer("regionserver", addr, port, false);
          this.infoServer.setAttribute("regionserver", this);
          this.infoServer.start();
          break;
        } catch (BindException e) {
          if (!auto){
            // auto bind disabled throw BindException
            throw e;
          }
          // auto bind enabled, try to use another port
          LOG.info("Failed binding http info server to port: " + port);
          port++;
          // update HRS server info port.
          this.serverInfo = new DBServerInfo(this.serverInfo.getServerAddress(),
            this.serverInfo.getStartCode(), port,
            this.serverInfo.getHostname());
        }
      }
    }

    this.replicationHandler.startReplicationServices();

    // Start Server.  This service is like leases in that it internally runs
    // a thread.
    this.server.start();
    LOG.info("DBRegionServer started at: " +
      this.serverInfo.getServerAddress().toString());
  }

  /*
   * Verify that server is healthy
   */
  private boolean isHealthy() {
    if (!fsOk) {
      // File system problem
      return false;
    }
    // Verify that all threads are alive
    if (!(leases.isAlive() && compactSplitThread.isAlive() &&
        cacheFlusher.isAlive() && hlogRoller.isAlive() &&
        workerThread.isAlive() && this.majorCompactionChecker.isAlive())) {
      // One or more threads are no longer alive - shut down
      stop();
      return false;
    }
    return true;
  }

  /*
   * Run some housekeeping tasks.
   */
  private void housekeeping() {
    // If the todo list has > 0 messages, iterate looking for open region
    // messages. Send the master a message that we're working on its
    // processing so it doesn't assign the region elsewhere.
    if (this.toDo.isEmpty()) {
      return;
    }
    // This iterator isn't safe if elements are gone and HRS.Worker could
    // remove them (it already checks for null there). Goes from oldest.
    for (ToDoEntry e: this.toDo) {
      if(e == null) {
        LOG.warn("toDo gave a null entry during iteration");
        break;
      }
      DBMsg msg = e.msg;
      if (msg != null) {
        if (msg.isType(DBMsg.Type.MSG_REGION_OPEN)) {
          addProcessingMessage(msg.getRegionInfo());
        }
      } else {
        LOG.warn("Message is empty: " + e);
      }
    }
  }

  /** @return the DBLog */
  public DBLog getLog() {
    return this.hlog;
  }

  /**
   * Sets a flag that will cause all the DBRegionServer threads to shut down
   * in an orderly fashion.  Used by unit tests.
   */
  public void stop() {
    this.stopRequested.set(true);
    synchronized(this) {
      // Wakes run() if it is sleeping
      notifyAll(); // FindBugs NN_NAKED_NOTIFY
    }
  }

  /**
   * Cause the server to exit without closing the regions it is serving, the
   * log it is using and without notifying the master.
   * Used unit testing and on catastrophic events such as HDFS is yanked out
   * from under bigdb or we OOME.
   * @param reason the reason we are aborting
   * @param cause the exception that caused the abort, or null
   */
  public void abort(String reason, Throwable cause) {
    if (cause != null) {
      LOG.fatal("Aborting region server " + this + ": " + reason, cause);
    } else {
      LOG.fatal("Aborting region server " + this + ": " + reason);
    }
    this.abortRequested = true;
    this.reservedSpace.clear();
    if (this.metrics != null) {
      LOG.info("Dump of metrics: " + this.metrics);
    }
    stop();
  }
  
  /**
   * @see DBRegionServer#abort(String, Throwable)
   */
  public void abort(String reason) {
    abort(reason, null);
  }

  /*
   * Simulate a kill -9 of this server.
   * Exits w/o closing regions or cleaninup logs but it does close socket in
   * case want to bring up server on old hostname+port immediately.
   */
  protected void kill() {
    this.killed = true;
    abort("Simulated kill");
  }

  /**
   * Wait on all threads to finish.
   * Presumption is that all closes and stops have already been called.
   */
  protected void join() {
    Threads.shutdown(this.majorCompactionChecker);
    Threads.shutdown(this.workerThread);
    Threads.shutdown(this.cacheFlusher);
    Threads.shutdown(this.compactSplitThread);
    Threads.shutdown(this.hlogRoller);
    this.replicationHandler.join();
  }

  private boolean getMaster() {
    DBServerAddress masterAddress = null;
    while (masterAddress == null) {
      if (stopRequested.get()) {
        return false;
      }
      try {
        masterAddress = paxosWrapper.readMasterAddressOrThrow();
      } catch (IOException e) {
        LOG.warn("Unable to read master address from Paxos. Retrying." +
                 " Error was:", e);
        sleeper.sleep();
      }
    }

    LOG.info("Telling master at " + masterAddress + " that we are up");
    DBMasterRegionInterface master = null;
    while (!stopRequested.get() && master == null) {
      try {
        // Do initial RPC setup.  The final argument indicates that the RPC
        // should retry indefinitely.
        master = (DBMasterRegionInterface)DBRPC.waitForProxy(
          DBMasterRegionInterface.class, DBRPCProtocolVersion.versionID,
          masterAddress.getInetSocketAddress(), this.conf, -1, this.rpcTimeout);
      } catch (IOException e) {
        LOG.warn("Unable to connect to master. Retrying. Error was:", e);
        sleeper.sleep();
      }
    }
    this.bigdbMaster = master;
    return true;
  }

  /*
   * Let the master know we're here
   * Run initialization using parameters passed us by the master.
   */
  private MapWritable reportForDuty() {
    while (!stopRequested.get() && !getMaster()) {
      sleeper.sleep();
      LOG.warn("Unable to get master for initialization");
    }

    MapWritable result = null;
    long lastMsg = 0;
    while(!stopRequested.get()) {
      try {
        this.requestCount.set(0);
        MemoryUsage memory =
          ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        DBServerLoad hsl = new DBServerLoad(0, (int)memory.getUsed()/1024/1024,
          (int)memory.getMax()/1024/1024);
        this.serverInfo.setLoad(hsl);
        if (LOG.isDebugEnabled())
          LOG.debug("sending initial server load: " + hsl);
        lastMsg = System.currentTimeMillis();
        boolean startCodeOk = false;
        while(!startCodeOk) {
          this.serverInfo = createServerInfoWithNewStartCode(this.serverInfo);
          startCodeOk = paxosWrapper.writeRSLocation(this.serverInfo);
          if(!startCodeOk) {
           LOG.debug("Start code already taken, trying another one");
          }
        }
        result = this.bigdbMaster.regionServerStartup(this.serverInfo);
        break;
      } catch (IOException e) {
        LOG.warn("error telling master we are up", e);
      }
      sleeper.sleep(lastMsg);
    }
    return result;
  }

  private DBServerInfo createServerInfoWithNewStartCode(final DBServerInfo hsi) {
    return new DBServerInfo(hsi.getServerAddress(), hsi.getInfoPort(),
      hsi.getHostname());
  }

  /* Add to the outbound message buffer */
  private void reportOpen(DBRegionInfo region) {
    this.outboundMsgs.add(new DBMsg(DBMsg.Type.MSG_REPORT_OPEN, region));
  }

  /* Add to the outbound message buffer */
  private void reportClose(DBRegionInfo region) {
    reportClose(region, null);
  }

  /* Add to the outbound message buffer */
  private void reportClose(final DBRegionInfo region, final byte[] message) {
    this.outboundMsgs.add(new DBMsg(DBMsg.Type.MSG_REPORT_CLOSE, region, message));
  }

  /**
   * Add to the outbound message buffer
   *
   * When a region splits, we need to tell the master that there are two new
   * regions that need to be assigned.
   *
   * We do not need to inform the master about the old region, because we've
   * updated the meta or root regions, and the master will pick that up on its
   * next rescan of the root or meta tables.
   */
  void reportSplit(DBRegionInfo oldRegion, DBRegionInfo newRegionA,
      DBRegionInfo newRegionB) {
    this.outboundMsgs.add(new DBMsg(DBMsg.Type.MSG_REPORT_SPLIT_INCLUDES_DAUGHTERS,
      oldRegion, newRegionA, newRegionB,
      Bytes.toBytes("Daughters; " +
          newRegionA.getRegionNameAsString() + ", " +
          newRegionB.getRegionNameAsString())));
  }

  //////////////////////////////////////////////////////////////////////////////
  // DBMaster-given operations
  //////////////////////////////////////////////////////////////////////////////

  /*
   * Data structure to hold a DBMsg and retries count.
   */
  private static final class ToDoEntry {
    protected final AtomicInteger tries = new AtomicInteger(0);
    protected final DBMsg msg;

    ToDoEntry(final DBMsg msg) {
      this.msg = msg;
    }
  }

  final BlockingQueue<ToDoEntry> toDo = new LinkedBlockingQueue<ToDoEntry>();
  private Worker worker;
  private Thread workerThread;

  /** Thread that performs long running requests from the master */
  class Worker implements Runnable {
    void stop() {
      synchronized(toDo) {
        toDo.notifyAll();
      }
    }

    public void run() {
      try {
        while(!stopRequested.get()) {
          ToDoEntry e = null;
          try {
            e = toDo.poll(threadWakeFrequency, TimeUnit.MILLISECONDS);
            if(e == null || stopRequested.get()) {
              continue;
            }
            LOG.info("Worker: " + e.msg);
            DBRegion region = null;
            DBRegionInfo info = e.msg.getRegionInfo();
            switch(e.msg.getType()) {

            case MSG_REGIONSERVER_QUIESCE:
              closeUserRegions();
              break;

            case MSG_REGION_OPEN:
              // Open a region
              if (!haveRootRegion.get() && !info.isRootRegion()) {
                // root region is not online yet. requeue this task
                LOG.info("putting region open request back into queue because" +
                    " root region is not yet available");
                try {
                  toDo.put(e);
                } catch (InterruptedException ex) {
                  LOG.warn("insertion into toDo queue was interrupted", ex);
                  break;
                }
              }
              openRegion(info);
              break;

            case MSG_REGION_CLOSE:
              // Close a region
              closeRegion(e.msg.getRegionInfo(), true);
              break;

            case MSG_REGION_CLOSE_WITHOUT_REPORT:
              // Close a region, don't reply
              closeRegion(e.msg.getRegionInfo(), false);
              break;

            case MSG_REGION_SPLIT:
              region = getRegion(info.getRegionName());
              region.flushcache();
              region.shouldSplit(true);
              // force a compaction; split will be side-effect.
              compactSplitThread.compactionRequested(region,
                e.msg.getType().name());
              break;

            case MSG_REGION_MAJOR_COMPACT:
            case MSG_REGION_COMPACT:
              // Compact a region
              region = getRegion(info.getRegionName());
              compactSplitThread.compactionRequested(region,
                e.msg.isType(Type.MSG_REGION_MAJOR_COMPACT),
                e.msg.getType().name());
              break;

            case MSG_REGION_FLUSH:
              region = getRegion(info.getRegionName());
              region.flushcache();
              break;

            case TESTING_MSG_BLOCK_RS:
              while (!stopRequested.get()) {
                Threads.sleep(1000);
                LOG.info("Regionserver blocked by " +
                  DBMsg.Type.TESTING_MSG_BLOCK_RS + "; " + stopRequested.get());
              }
              break;

            default:
              throw new AssertionError(
                  "Impossible state during msg processing.  Instruction: "
                  + e.msg.toString());
            }
          } catch (InterruptedException ex) {
            LOG.warn("Processing Worker queue", ex);
          } catch (Exception ex) {
            if (ex instanceof IOException) {
              ex = RemoteExceptionHandler.checkIOException((IOException) ex);
            }
            if(e != null && e.tries.get() < numRetries) {
              LOG.warn("error: " + ex);
              e.tries.incrementAndGet();
              try {
                toDo.put(e);
              } catch (InterruptedException ie) {
                throw new RuntimeException("Putting into msgQueue was " +
                    "interrupted.", ex);
              }
            } else {
              LOG.error("unable to process message" +
                  (e != null ? (": " + e.msg.toString()) : ""), ex);
              if (!checkFileSystem()) {
                break;
              }
            }
          }
        }
      } catch(Throwable t) {
        if (!checkOOME(t)) {
          LOG.fatal("Unhandled exception", t);
        }
      } finally {
        LOG.info("worker thread exiting");
      }
    }
  }

  void openRegion(final DBRegionInfo regionInfo) {
    Integer mapKey = Bytes.mapKey(regionInfo.getRegionName());
    DBRegion region = this.onlineRegions.get(mapKey);
    if (region == null) {
      try {
        region = instantiateRegion(regionInfo, this.hlog);
        // Startup a compaction early if one is needed, if region has references
        // or if a store has too many store files
        if (region.hasReferences() || region.hasTooManyStoreFiles()) {
          this.compactSplitThread.compactionRequested(region,
            region.hasReferences() ? "Region has references on open" :
                                     "Region has too many store files");
        }
      } catch (Throwable e) {
        Throwable t = cleanup(e,
          "Error opening " + regionInfo.getRegionNameAsString());
        // TODO: add an extra field in DBRegionInfo to indicate that there is
        // an error. We can't do that now because that would be an incompatible
        // change that would require a migration
        reportClose(regionInfo, StringUtils.stringifyException(t).getBytes());
        return;
      }
      addToOnlineRegions(region);
    }
    reportOpen(regionInfo);
  }

  /**
   * @param regionInfo RegionInfo for the Region we're to instantiate and
   * initialize.
   * @param wal Set into here the regions' seqid.
   * @return
   * @throws IOException
   */
  protected DBRegion instantiateRegion(final DBRegionInfo regionInfo, final DBLog wal)
  throws IOException {
    Path dir =
      DBTableDescriptor.getTableDir(rootDir, regionInfo.getTableDesc().getName());
    DBRegion r = DBRegion.newDBRegion(dir, this.hlog, this.fs, conf, regionInfo,
      this.cacheFlusher);
    long seqid = r.initialize(new Progressable() {
      public void progress() {
        addProcessingMessage(regionInfo);
      }
    });
    // If seqid  > current wal seqid, the wal seqid is updated.
    if (wal != null) wal.setSequenceNumber(seqid);
    return r;
  }

  /**
   * Add a MSG_REPORT_PROCESS_OPEN to the outbound queue.
   * This method is called while region is in the queue of regions to process
   * and then while the region is being opened, it is called from the Worker
   * thread that is running the region open.
   * @param hri Region to add the message for
   */
  public void addProcessingMessage(final DBRegionInfo hri) {
    getOutboundMsgs().add(new DBMsg(DBMsg.Type.MSG_REPORT_PROCESS_OPEN, hri));
  }

  protected void closeRegion(final DBRegionInfo hri, final boolean reportWhenCompleted)
  throws IOException {
    DBRegion region = this.removeFromOnlineRegions(hri);
    if (region != null) {
      region.close();
      if(reportWhenCompleted) {
        reportClose(hri);
      }
    }
  }

  /** Called either when the master tells us to restart or from stop() */
  ArrayList<DBRegion> closeAllRegions() {
    ArrayList<DBRegion> regionsToClose = new ArrayList<DBRegion>();
    this.lock.writeLock().lock();
    try {
      regionsToClose.addAll(onlineRegions.values());
      onlineRegions.clear();
    } finally {
      this.lock.writeLock().unlock();
    }
    // Close any outstanding scanners.  Means they'll get an UnknownScanner
    // exception next time they come in.
    for (Map.Entry<String, InternalScanner> e: this.scanners.entrySet()) {
      try {
        e.getValue().close();
      } catch (IOException ioe) {
        LOG.warn("Closing scanner " + e.getKey(), ioe);
      }
    }
    for (DBRegion region: regionsToClose) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("closing region " + Bytes.toString(region.getRegionName()));
      }
      try {
        region.close(abortRequested);
      } catch (Throwable e) {
        cleanup(e, "Error closing " + Bytes.toString(region.getRegionName()));
      }
    }
    return regionsToClose;
  }

  /*
   * Thread to run close of a region.
   */
  private static class RegionCloserThread extends Thread {
    private final DBRegion r;

    protected RegionCloserThread(final DBRegion r) {
      super(Thread.currentThread().getName() + ".regionCloser." + r.toString());
      this.r = r;
    }

    @Override
    public void run() {
      try {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Closing region " + r.toString());
        }
        r.close();
      } catch (Throwable e) {
        LOG.error("Error closing region " + r.toString(),
          RemoteExceptionHandler.checkThrowable(e));
      }
    }
  }

  /** Called as the first stage of cluster shutdown. */
  void closeUserRegions() {
    ArrayList<DBRegion> regionsToClose = new ArrayList<DBRegion>();
    this.lock.writeLock().lock();
    try {
      synchronized (onlineRegions) {
        for (Iterator<Map.Entry<Integer, DBRegion>> i =
            onlineRegions.entrySet().iterator(); i.hasNext();) {
          Map.Entry<Integer, DBRegion> e = i.next();
          DBRegion r = e.getValue();
          if (!r.getRegionInfo().isMetaRegion()) {
            regionsToClose.add(r);
            i.remove();
          }
        }
      }
    } finally {
      this.lock.writeLock().unlock();
    }
    // Run region closes in parallel.
    Set<Thread> threads = new HashSet<Thread>();
    try {
      for (final DBRegion r : regionsToClose) {
        RegionCloserThread t = new RegionCloserThread(r);
        t.start();
        threads.add(t);
      }
    } finally {
      for (Thread t : threads) {
        while (t.isAlive()) {
          try {
            t.join();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }
    this.quiesced.set(true);
    if (onlineRegions.size() == 0) {
      outboundMsgs.add(REPORT_EXITING);
    } else {
      outboundMsgs.add(REPORT_QUIESCED);
    }
  }

  //
  // DBRegionInterface
  //

  public DBRegionInfo getRegionInfo(final byte [] regionName)
  throws NotServingRegionException {
    requestCount.incrementAndGet();
    return getRegion(regionName).getRegionInfo();
  }


  public Result getClosestRowBefore(final byte [] regionName,
    final byte [] row, final byte [] family)
  throws IOException {
    checkOpen();
    requestCount.incrementAndGet();
    try {
      // locate the region we're operating on
      DBRegion region = getRegion(regionName);
      // ask the region for all the data

      Result r = region.getClosestRowBefore(row, family);
      return r;
    } catch (Throwable t) {
      throw convertThrowableToIOE(cleanup(t));
    }
  }

  /** {@inheritDoc} */
  public Result get(byte [] regionName, Get get) throws IOException {
    checkOpen();
    requestCount.incrementAndGet();
    try {
      DBRegion region = getRegion(regionName);
      return region.get(get, getLockFromId(get.getLockId()));
    } catch(Throwable t) {
      throw convertThrowableToIOE(cleanup(t));
    }
  }

  public boolean exists(byte [] regionName, Get get) throws IOException {
    checkOpen();
    requestCount.incrementAndGet();
    try {
      DBRegion region = getRegion(regionName);
      Result r = region.get(get, getLockFromId(get.getLockId()));
      return r != null && !r.isEmpty();
    } catch(Throwable t) {
      throw convertThrowableToIOE(cleanup(t));
    }
  }

  public void put(final byte [] regionName, final Put put)
  throws IOException {
    if (put.getRow() == null)
      throw new IllegalArgumentException("update has null row");

    checkOpen();
    this.requestCount.incrementAndGet();
    DBRegion region = getRegion(regionName);
    try {
      if (!region.getRegionInfo().isMetaTable()) {
        this.cacheFlusher.reclaimMemStoreMemory();
      }
      boolean writeToWAL = put.getWriteToWAL();
      region.put(put, getLockFromId(put.getLockId()), writeToWAL);
    } catch (Throwable t) {
      throw convertThrowableToIOE(cleanup(t));
    }
  }

  public int put(final byte[] regionName, final List<Put> puts)
  throws IOException {
    checkOpen();
    DBRegion region = null;
    try {
      region = getRegion(regionName);
      if (!region.getRegionInfo().isMetaTable()) {
        this.cacheFlusher.reclaimMemStoreMemory();
      }
      
      @SuppressWarnings("unchecked")
      Pair<Put, Integer>[] putsWithLocks = new Pair[puts.size()];
      
      int i = 0;
      for (Put p : puts) {
        Integer lock = getLockFromId(p.getLockId());
        putsWithLocks[i++] = new Pair<Put, Integer>(p, lock);
      }
      
      this.requestCount.addAndGet(puts.size());
      OperationStatusCode[] codes = region.put(putsWithLocks);
      for (i = 0; i < codes.length; i++) {
        if (codes[i] != OperationStatusCode.SUCCESS)
          return i;
      }
      return -1;
    } catch (Throwable t) {
      throw convertThrowableToIOE(cleanup(t));
    }
  }

  private boolean checkAndMutate(final byte[] regionName, final byte [] row,
      final byte [] family, final byte [] qualifier, final byte [] value,
      final Writable w, Integer lock) throws IOException {
    checkOpen();
    this.requestCount.incrementAndGet();
    DBRegion region = getRegion(regionName);
    try {
      if (!region.getRegionInfo().isMetaTable()) {
        this.cacheFlusher.reclaimMemStoreMemory();
      }
      return region.checkAndMutate(row, family, qualifier, value, w, lock,
          true);
    } catch (Throwable t) {
      throw convertThrowableToIOE(cleanup(t));
    }
  }


  /**
   *
   * @param regionName
   * @param row
   * @param family
   * @param qualifier
   * @param value the expected value
   * @param put
   * @throws IOException
   * @return true if the new put was execute, false otherwise
   */
  public boolean checkAndPut(final byte[] regionName, final byte [] row,
      final byte [] family, final byte [] qualifier, final byte [] value,
      final Put put) throws IOException{
    return checkAndMutate(regionName, row, family, qualifier, value, put,
        getLockFromId(put.getLockId()));
  }

  /**
   *
   * @param regionName
   * @param row
   * @param family
   * @param qualifier
   * @param value the expected value
   * @param delete
   * @throws IOException
   * @return true if the new put was execute, false otherwise
   */
  public boolean checkAndDelete(final byte[] regionName, final byte [] row,
      final byte [] family, final byte [] qualifier, final byte [] value,
      final Delete delete) throws IOException{
    return checkAndMutate(regionName, row, family, qualifier, value, delete,
        getLockFromId(delete.getLockId()));
  }

  //
  // remote scanner interface
  //

  public long openScanner(byte [] regionName, Scan scan)
  throws IOException {
    checkOpen();
    NullPointerException npe = null;
    if (regionName == null) {
      npe = new NullPointerException("regionName is null");
    } else if (scan == null) {
      npe = new NullPointerException("scan is null");
    }
    if (npe != null) {
      throw new IOException("Invalid arguments to openScanner", npe);
    }
    requestCount.incrementAndGet();
    try {
      DBRegion r = getRegion(regionName);
      return addScanner(r.getScanner(scan));
    } catch (Throwable t) {
      throw convertThrowableToIOE(cleanup(t, "Failed openScanner"));
    }
  }

  protected long addScanner(InternalScanner s) throws LeaseStillHeldException {
    long scannerId = -1L;
    scannerId = rand.nextLong();
    String scannerName = String.valueOf(scannerId);
    scanners.put(scannerName, s);
    this.leases.
      createLease(scannerName, new ScannerListener(scannerName));
    return scannerId;
  }

  public Result next(final long scannerId) throws IOException {
    Result [] res = next(scannerId, 1);
    if(res == null || res.length == 0) {
      return null;
    }
    return res[0];
  }

  public Result [] next(final long scannerId, int nbRows) throws IOException {
    try {
      String scannerName = String.valueOf(scannerId);
      InternalScanner s = this.scanners.get(scannerName);
      if (s == null) {
        throw new UnknownScannerException("Name: " + scannerName);
      }
      try {
        checkOpen();
      } catch (IOException e) {
        // If checkOpen failed, server not running or filesystem gone,
        // cancel this lease; filesystem is gone or we're closing or something.
        this.leases.cancelLease(scannerName);
        throw e;
      }
      this.leases.renewLease(scannerName);
      List<Result> results = new ArrayList<Result>(nbRows);
      long currentScanResultSize = 0;
      List<KeyValue> values = new ArrayList<KeyValue>();
      for (int i = 0; i < nbRows && currentScanResultSize < maxScannerResultSize; i++) {
        requestCount.incrementAndGet();
        // Collect values to be returned here
        boolean moreRows = s.next(values);
        if (!values.isEmpty()) {
          for (KeyValue kv : values) {
            currentScanResultSize += kv.heapSize();
          }
          results.add(new Result(values));
        }
        if (!moreRows) {
          break;
        }
        values.clear();
      }
      // Below is an ugly hack where we cast the InternalScanner to be a
      // DBRegion.RegionScanner.  The alternative is to change InternalScanner
      // interface but its used everywhere whereas we just need a bit of info
      // from DBRegion.RegionScanner, IF its filter if any is done with the scan
      // and wants to tell the client to stop the scan.  This is done by passing
      // a null result.
      return ((DBRegion.RegionScanner)s).isFilterDone() && results.isEmpty()?
        null: results.toArray(new Result[0]);
    } catch (Throwable t) {
      if (t instanceof NotServingRegionException) {
        String scannerName = String.valueOf(scannerId);
        this.scanners.remove(scannerName);
      }
      throw convertThrowableToIOE(cleanup(t));
    }
  }

  public void close(final long scannerId) throws IOException {
    try {
      checkOpen();
      requestCount.incrementAndGet();
      String scannerName = String.valueOf(scannerId);
      InternalScanner s = scanners.remove(scannerName);
      if (s != null) {
        s.close();
        this.leases.cancelLease(scannerName);
      }
    } catch (Throwable t) {
      throw convertThrowableToIOE(cleanup(t));
    }
  }

  /**
   * Instantiated as a scanner lease.
   * If the lease times out, the scanner is closed
   */
  private class ScannerListener implements LeaseListener {
    private final String scannerName;

    ScannerListener(final String n) {
      this.scannerName = n;
    }

    public void leaseExpired() {
      LOG.info("Scanner " + this.scannerName + " lease expired");
      InternalScanner s = scanners.remove(this.scannerName);
      if (s != null) {
        try {
          s.close();
        } catch (IOException e) {
          LOG.error("Closing scanner", e);
        }
      }
    }
  }

  //
  // Methods that do the actual work for the remote API
  //
  public void delete(final byte [] regionName, final Delete delete)
  throws IOException {
    checkOpen();
    try {
      boolean writeToWAL = true;
      this.requestCount.incrementAndGet();
      DBRegion region = getRegion(regionName);
      if (!region.getRegionInfo().isMetaTable()) {
        this.cacheFlusher.reclaimMemStoreMemory();
      }
      Integer lid = getLockFromId(delete.getLockId());
      region.delete(delete, lid, writeToWAL);
    } catch (Throwable t) {
      throw convertThrowableToIOE(cleanup(t));
    }
  }

  public int delete(final byte[] regionName, final List<Delete> deletes)
  throws IOException {
    // Count of Deletes processed.
    int i = 0;
    checkOpen();
    DBRegion region = null;
    try {
      boolean writeToWAL = true;
      region = getRegion(regionName);
      if (!region.getRegionInfo().isMetaTable()) {
        this.cacheFlusher.reclaimMemStoreMemory();
      }
      int size = deletes.size();
      Integer[] locks = new Integer[size];
      for (Delete delete: deletes) {
        this.requestCount.incrementAndGet();
        locks[i] = getLockFromId(delete.getLockId());
        region.delete(delete, locks[i], writeToWAL);
        i++;
      }
    } catch (WrongRegionException ex) {
      LOG.debug("Batch deletes: " + i, ex);
      return i;
    } catch (NotServingRegionException ex) {
      return i;
    } catch (Throwable t) {
      throw convertThrowableToIOE(cleanup(t));
    }
    return -1;
  }

  public long lockRow(byte [] regionName, byte [] row)
  throws IOException {
    checkOpen();
    NullPointerException npe = null;
    if(regionName == null) {
      npe = new NullPointerException("regionName is null");
    } else if(row == null) {
      npe = new NullPointerException("row to lock is null");
    }
    if(npe != null) {
      IOException io = new IOException("Invalid arguments to lockRow");
      io.initCause(npe);
      throw io;
    }
    requestCount.incrementAndGet();
    try {
      DBRegion region = getRegion(regionName);
      Integer r = region.obtainRowLock(row);
      long lockId = addRowLock(r,region);
      LOG.debug("Row lock " + lockId + " explicitly acquired by client");
      return lockId;
    } catch (Throwable t) {
      throw convertThrowableToIOE(cleanup(t,
        "Error obtaining row lock (fsOk: " + this.fsOk + ")"));
    }
  }

  protected long addRowLock(Integer r, DBRegion region) throws LeaseStillHeldException {
    long lockId = -1L;
    lockId = rand.nextLong();
    String lockName = String.valueOf(lockId);
    rowlocks.put(lockName, r);
    this.leases.
      createLease(lockName, new RowLockListener(lockName, region));
    return lockId;
  }

  /**
   * Method to get the Integer lock identifier used internally
   * from the long lock identifier used by the client.
   * @param lockId long row lock identifier from client
   * @return intId Integer row lock used internally in DBRegion
   * @throws IOException Thrown if this is not a valid client lock id.
   */
  Integer getLockFromId(long lockId)
  throws IOException {
    if (lockId == -1L) {
      return null;
    }
    String lockName = String.valueOf(lockId);
    Integer rl = rowlocks.get(lockName);
    if (rl == null) {
      throw new IOException("Invalid row lock");
    }
    this.leases.renewLease(lockName);
    return rl;
  }

  public void unlockRow(byte [] regionName, long lockId)
  throws IOException {
    checkOpen();
    NullPointerException npe = null;
    if(regionName == null) {
      npe = new NullPointerException("regionName is null");
    } else if(lockId == -1L) {
      npe = new NullPointerException("lockId is null");
    }
    if(npe != null) {
      IOException io = new IOException("Invalid arguments to unlockRow");
      io.initCause(npe);
      throw io;
    }
    requestCount.incrementAndGet();
    try {
      DBRegion region = getRegion(regionName);
      String lockName = String.valueOf(lockId);
      Integer r = rowlocks.remove(lockName);
      if(r == null) {
        throw new UnknownRowLockException(lockName);
      }
      region.releaseRowLock(r);
      this.leases.cancelLease(lockName);
      LOG.debug("Row lock " + lockId + " has been explicitly released by client");
    } catch (Throwable t) {
      throw convertThrowableToIOE(cleanup(t));
    }
  }

  @Override
  public void bulkLoadDBFile(
      String dbfilePath, byte[] regionName, byte[] familyName)
  throws IOException {
    DBRegion region = getRegion(regionName);
    region.bulkLoadDBFile(dbfilePath, familyName);
  }

  Map<String, Integer> rowlocks =
    new ConcurrentHashMap<String, Integer>();

  /**
   * Instantiated as a row lock lease.
   * If the lease times out, the row lock is released
   */
  private class RowLockListener implements LeaseListener {
    private final String lockName;
    private final DBRegion region;

    RowLockListener(final String lockName, final DBRegion region) {
      this.lockName = lockName;
      this.region = region;
    }

    public void leaseExpired() {
      LOG.info("Row Lock " + this.lockName + " lease expired");
      Integer r = rowlocks.remove(this.lockName);
      if(r != null) {
        region.releaseRowLock(r);
      }
    }
  }

  /** @return the info server */
  public InfoServer getInfoServer() {
    return infoServer;
  }

  /**
   * @return true if a stop has been requested.
   */
  public boolean isStopRequested() {
    return this.stopRequested.get();
  }

  /**
   *
   * @return the configuration
   */
  public Configuration getConfiguration() {
    return conf;
  }

  /** @return the write lock for the server */
  ReentrantReadWriteLock.WriteLock getWriteLock() {
    return lock.writeLock();
  }

  /**
   * @return Immutable list of this servers regions.
   */
  public Collection<DBRegion> getOnlineRegions() {
    return Collections.unmodifiableCollection(onlineRegions.values());
  }

  public DBRegion [] getOnlineRegionsAsArray() {
    return getOnlineRegions().toArray(new DBRegion[0]);
  }

  /**
   * @return The DBRegionInfos from online regions sorted
   */
  public SortedSet<DBRegionInfo> getSortedOnlineRegionInfos() {
    SortedSet<DBRegionInfo> result = new TreeSet<DBRegionInfo>();
    synchronized(this.onlineRegions) {
      for (DBRegion r: this.onlineRegions.values()) {
        result.add(r.getRegionInfo());
      }
    }
    return result;
  }

  public void addToOnlineRegions(final DBRegion r) {
    this.lock.writeLock().lock();
    try {
      this.onlineRegions.put(Bytes.mapKey(r.getRegionInfo().getRegionName()), r);
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  public DBRegion removeFromOnlineRegions(DBRegionInfo hri) {
    this.lock.writeLock().lock();
    DBRegion toReturn = null;
    try {
      toReturn = onlineRegions.remove(Bytes.mapKey(hri.getRegionName()));
    } finally {
      this.lock.writeLock().unlock();
    }
    return toReturn;
  }

  /**
   * @return A new Map of online regions sorted by region size with the first
   * entry being the biggest.
   */
  public SortedMap<Long, DBRegion> getCopyOfOnlineRegionsSortedBySize() {
    // we'll sort the regions in reverse
    SortedMap<Long, DBRegion> sortedRegions = new TreeMap<Long, DBRegion>(
        new Comparator<Long>() {
          public int compare(Long a, Long b) {
            return -1 * a.compareTo(b);
          }
        });
    // Copy over all regions. Regions are sorted by size with biggest first.
    synchronized (this.onlineRegions) {
      for (DBRegion region : this.onlineRegions.values()) {
        sortedRegions.put(Long.valueOf(region.memstoreSize.get()), region);
      }
    }
    return sortedRegions;
  }

  /**
   * @param regionName
   * @return DBRegion for the passed <code>regionName</code> or null if named
   * region is not member of the online regions.
   */
  public DBRegion getOnlineRegion(final byte [] regionName) {
    return onlineRegions.get(Bytes.mapKey(regionName));
  }

  /** @return the request count */
  public AtomicInteger getRequestCount() {
    return this.requestCount;
  }

  /** @return reference to FlushRequester */
  public FlushRequester getFlushRequester() {
    return this.cacheFlusher;
  }

  /**
   * Protected utility method for safely obtaining an DBRegion handle.
   * @param regionName Name of online {@link DBRegion} to return
   * @return {@link DBRegion} for <code>regionName</code>
   * @throws NotServingRegionException
   */
  protected DBRegion getRegion(final byte [] regionName)
  throws NotServingRegionException {
    DBRegion region = null;
    this.lock.readLock().lock();
    try {
      region = onlineRegions.get(Integer.valueOf(Bytes.hashCode(regionName)));
      if (region == null) {
        throw new NotServingRegionException(regionName);
      }
      return region;
    } finally {
      this.lock.readLock().unlock();
    }
  }

  /**
   * Get the top N most loaded regions this server is serving so we can
   * tell the master which regions it can reallocate if we're overloaded.
   * TODO: actually calculate which regions are most loaded. (Right now, we're
   * just grabbing the first N regions being served regardless of load.)
   */
  protected DBRegionInfo[] getMostLoadedRegions() {
    ArrayList<DBRegionInfo> regions = new ArrayList<DBRegionInfo>();
    synchronized (onlineRegions) {
      for (DBRegion r : onlineRegions.values()) {
        if (r.isClosed() || r.isClosing()) {
          continue;
        }
        if (regions.size() < numRegionsToReport) {
          regions.add(r.getRegionInfo());
        } else {
          break;
        }
      }
    }
    return regions.toArray(new DBRegionInfo[regions.size()]);
  }

  /**
   * Called to verify that this server is up and running.
   *
   * @throws IOException
   */
  protected void checkOpen() throws IOException {
    if (this.stopRequested.get() || this.abortRequested) {
      throw new IOException("Server not running" +
        (this.abortRequested? ", aborting": ""));
    }
    if (!fsOk) {
      throw new IOException("File system not available");
    }
  }

  /**
   * @return Returns list of non-closed regions hosted on this server.  If no
   * regions to check, returns an empty list.
   */
  protected Set<DBRegion> getRegionsToCheck() {
    HashSet<DBRegion> regionsToCheck = new HashSet<DBRegion>();
    //TODO: is this locking necessary?
    lock.readLock().lock();
    try {
      regionsToCheck.addAll(this.onlineRegions.values());
    } finally {
      lock.readLock().unlock();
    }
    // Purge closed regions.
    for (final Iterator<DBRegion> i = regionsToCheck.iterator(); i.hasNext();) {
      DBRegion r = i.next();
      if (r.isClosed()) {
        i.remove();
      }
    }
    return regionsToCheck;
  }

  public long getProtocolVersion(final String protocol,
      final long clientVersion)
  throws IOException {
    if (protocol.equals(DBRegionInterface.class.getName())) {
      return DBRPCProtocolVersion.versionID;
    }
    throw new IOException("Unknown protocol to name node: " + protocol);
  }

  /**
   * @return Queue to which you can add outbound messages.
   */
  protected LinkedBlockingQueue<DBMsg> getOutboundMsgs() {
    return this.outboundMsgs;
  }

  /**
   * Return the total size of all memstores in every region.
   * @return memstore size in bytes
   */
  public long getGlobalMemStoreSize() {
    long total = 0;
    synchronized (onlineRegions) {
      for (DBRegion region : onlineRegions.values()) {
        total += region.memstoreSize.get();
      }
    }
    return total;
  }

  /**
   * @return Return the leases.
   */
  protected Leases getLeases() {
    return leases;
  }

  /**
   * @return Return the rootDir.
   */
  protected Path getRootDir() {
    return rootDir;
  }

  /**
   * @return Return the fs.
   */
  protected FileSystem getFileSystem() {
    return fs;
  }

  /**
   * @return Info on port this server has bound to, etc.
   */
  public DBServerInfo getServerInfo() { return this.serverInfo; }

  /** {@inheritDoc} */
  public long incrementColumnValue(byte [] regionName, byte [] row,
      byte [] family, byte [] qualifier, long amount, boolean writeToWAL)
  throws IOException {
    checkOpen();

    if (regionName == null) {
      throw new IOException("Invalid arguments to incrementColumnValue " +
      "regionName is null");
    }
    requestCount.incrementAndGet();
    try {
      DBRegion region = getRegion(regionName);
      long retval = region.incrementColumnValue(row, family, qualifier, amount,
          writeToWAL);

      return retval;
    } catch (IOException e) {
      checkFileSystem();
      throw e;
    }
  }

  /** {@inheritDoc} */
  public DBRegionInfo[] getRegionsAssignment() throws IOException {
    DBRegionInfo[] regions = new DBRegionInfo[onlineRegions.size()];
    Iterator<DBRegion> ite = onlineRegions.values().iterator();
    for(int i = 0; ite.hasNext(); i++) {
      regions[i] = ite.next().getRegionInfo();
    }
    return regions;
  }

  /** {@inheritDoc} */
  public DBServerInfo getDBServerInfo() throws IOException {
    return serverInfo;
  }

  @Override
  public MultiPutResponse multiPut(MultiPut puts) throws IOException {
    MultiPutResponse resp = new MultiPutResponse();

    // do each region as it's own.
    for( Map.Entry<byte[], List<Put>> e: puts.puts.entrySet()) {
      int result = put(e.getKey(), e.getValue());
      resp.addResult(e.getKey(), result);

      e.getValue().clear(); // clear some RAM
    }

    return resp;
  }

  public String toString() {
    return this.serverInfo.toString();
  }

  /**
   * Interval at which threads should run
   * @return the interval
   */
  public int getThreadWakeFrequency() {
    return threadWakeFrequency;
  }

  //
  // Main program and support routines
  //

  /**
   * @param hrs
   * @return Thread the RegionServer is running in correctly named.
   * @throws IOException
   */
  public static Thread startRegionServer(final DBRegionServer hrs)
  throws IOException {
    return startRegionServer(hrs,
      "regionserver" + hrs.getServerInfo().getServerAddress().getPort());
  }

  /**
   * @param hrs
   * @param name
   * @return Thread the RegionServer is running in correctly named.
   * @throws IOException
   */
  public static Thread startRegionServer(final DBRegionServer hrs,
      final String name)
  throws IOException {
    Thread t = new Thread(hrs);
    t.setName(name);
    t.start();
    // Install shutdown hook that will catch signals and run an orderly shutdown
    // of the hrs.
    ShutdownHook.install(hrs.getConfiguration(),
      FSUtils.getFs(hrs.getConfiguration()), hrs, t);
    return t;
  }

  @SuppressWarnings("unused")
  private static void printUsageAndExit() {
    printUsageAndExit(null);
  }

  private static void printUsageAndExit(final String message) {
    if (message != null) {
      System.err.println(message);
    }
    System.err.println("Usage: java org.apache.bigdb.DBRegionServer start|stop [-D <conf.param=value>]");
    System.exit(0);
  }

  /**
   * Utility for constructing an instance of the passed DBRegionServer class.
   * @param regionServerClass
   * @param conf2
   * @return DBRegionServer instance.
   */
  public static DBRegionServer constructRegionServer(Class<? extends DBRegionServer> regionServerClass,
      final Configuration conf2)  {
    try {
      Constructor<? extends DBRegionServer> c =
        regionServerClass.getConstructor(Configuration.class);
      return c.newInstance(conf2);
    } catch (Exception e) {
      throw new RuntimeException("Failed construction of " +
        "Master: " + regionServerClass.toString(), e);
    }
  }

  @Override
  public void replicateLogEntries(DBLog.Entry[] entries) throws IOException {
    this.replicationHandler.replicateLogEntries(entries);
  }

  /**
   * Do class main.
   * @param args
   * @param regionServerClass DBRegionServer to instantiate.
   */
  protected static void doMain(final String [] args,
      final Class<? extends DBRegionServer> regionServerClass) {
    /*Configuration conf = ConfigurationFactory.get();

    Options opt = new Options();
    opt.addOption("D", true, "Override BigDB Configuration Settings");
    try {
      CommandLine cmd = new GnuParser().parse(opt, args);

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

      if (cmd.getArgList().contains("start")) {
        try {
          // If 'local', don't start a region server here.  Defer to
          // LocalDBCluster.  It manages 'local' clusters.
          if (LocalDBCluster.isLocal(conf)) {
            LOG.warn("Not starting a distinct region server because " +
              DBConstants.CLUSTER_DISTRIBUTED + " is false");
          } else {
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            if (runtime != null) {
              LOG.info("vmInputArguments=" + runtime.getInputArguments());
            }
            DBRegionServer hrs = constructRegionServer(regionServerClass, conf);
            startRegionServer(hrs);
          }
        } catch (Throwable t) {
          LOG.error( "Can not start region server because "+
              StringUtils.stringifyException(t) );
          System.exit(-1);
        }
      } else if (cmd.getArgList().contains("stop")) {
        throw new ParseException("To shutdown the regionserver run " +
            "bin/bigdb-daemon.sh stop regionserver or send a kill signal to" +
            "the regionserver pid");
      } else {
        throw new ParseException("Unknown argument(s): " +
            org.apache.commons.lang.StringUtils.join(cmd.getArgs(), " "));
      }
    } catch (ParseException e) {
      LOG.error("Could not parse", e);
      printUsageAndExit();
    }*/
  }

  /**
   * @param args
   */
  public static void main(String [] args) {
    Configuration conf = ConfigurationFactory.get();
    @SuppressWarnings("unchecked")
    Class<? extends DBRegionServer> regionServerClass =
      (Class<? extends DBRegionServer>) conf.getClass(DBConstants.REGION_SERVER_IMPL,
        DBRegionServer.class);
    doMain(args, regionServerClass);
  }

  public int getNumberOfOnlineRegions() {
    return onlineRegions.size();
  }
}

