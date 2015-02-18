package org.javenstudio.raptor.bigdb.master;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.bigdb.Chore;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBMsg;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.DBRegionLocation;
import org.javenstudio.raptor.bigdb.DBServerAddress;
import org.javenstudio.raptor.bigdb.DBServerInfo;
import org.javenstudio.raptor.bigdb.DBServerLoad;
import org.javenstudio.raptor.bigdb.PleaseHoldException;
import org.javenstudio.raptor.bigdb.YouAreDeadException;
import org.javenstudio.raptor.bigdb.client.Get;
import org.javenstudio.raptor.bigdb.client.Result;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.master.RegionManager.RegionState;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.bigdb.util.Threads;
import org.javenstudio.raptor.paxos.WatchedEvent;
import org.javenstudio.raptor.paxos.Watcher;
import org.javenstudio.raptor.paxos.Watcher.Event.EventType;

/**
 * The ServerManager class manages info about region servers - DBServerInfo,
 * load numbers, dying servers, etc.
 */
public class ServerManager {
  private static final Logger LOG =
		  Logger.getLogger(ServerManager.class);

  private final AtomicInteger quiescedServers = new AtomicInteger(0);

  // The map of known server names to server info
  private final Map<String, DBServerInfo> serversToServerInfo =
    new ConcurrentHashMap<String, DBServerInfo>();

  /**
   * Set of known dead servers.  On znode expiration, servers are added here.
   * This is needed in case of a network partitioning where the server's lease
   * expires, but the server is still running. After the network is healed,
   * and it's server logs are recovered, it will be told to call server startup
   * because by then, its regions have probably been reassigned.
   */
  private final Set<String> deadServers =
    Collections.synchronizedSet(new HashSet<String>());

  // SortedMap server load -> Set of server names
  private final SortedMap<DBServerLoad, Set<String>> loadToServers =
    Collections.synchronizedSortedMap(new TreeMap<DBServerLoad, Set<String>>());
  // Map of server names -> server load
  private final Map<String, DBServerLoad> serversToLoad =
    new ConcurrentHashMap<String, DBServerLoad>();

  private DBMaster master;

  /**
   * The regionserver will not be assigned or asked close regions if it
   * is currently opening >= this many regions.
   */
  private final int nobalancingCount;

  private final ServerMonitor serverMonitorThread;

  private int minimumServerCount;

  private final LogsCleaner logCleaner;

  /**
   * Dumps into log current stats on dead servers and number of servers
   * TODO: Make this a metric; dump metrics into log.
   */
  class ServerMonitor extends Chore {
    ServerMonitor(final int period, final AtomicBoolean stop) {
      super("ServerMonitor", period, stop);
    }

    @Override
    protected void chore() {
      int numServers = serversToServerInfo.size();
      int numDeadServers = deadServers.size();
      double averageLoad = getAverageLoad();
      String deadServersList = null;
      if (numDeadServers > 0) {
        StringBuilder sb = new StringBuilder("Dead Server [");
        boolean first = true;
        synchronized (deadServers) {
          for (String server: deadServers) {
            if (!first) {
              sb.append(",  ");
              first = false;
            }
            sb.append(server);
          }
        }
        sb.append("]");
        deadServersList = sb.toString();
      }
      LOG.info(numServers + " region servers, " + numDeadServers +
        " dead, average load " + averageLoad +
        (deadServersList != null? deadServers: ""));
    }
  }

  /**
   * Constructor.
   * @param master
   */
  public ServerManager(DBMaster master) {
    this.master = master;
    Configuration c = master.getConfiguration();
    this.nobalancingCount = c.getInt("bigdb.regions.nobalancing.count", 4);
    int metaRescanInterval = c.getInt("bigdb.master.meta.thread.rescanfrequency",
      60 * 1000);
    this.minimumServerCount = c.getInt("bigdb.regions.server.count.min", 0);
    this.serverMonitorThread = new ServerMonitor(metaRescanInterval,
      this.master.getShutdownRequested());
    String n = Thread.currentThread().getName();
    Threads.setDaemonThreadRunning(this.serverMonitorThread,
      n + ".serverMonitor");
    this.logCleaner = new LogsCleaner(
      c.getInt("bigdb.master.meta.thread.rescanfrequency",60 * 1000),
        this.master.getShutdownRequested(), c,
        master.getFileSystem(), master.getOldLogDir());
    Threads.setDaemonThreadRunning(logCleaner,
      n + ".oldLogCleaner");

  }

  /**
   * Let the server manager know a new regionserver has come online
   * @param serverInfo
   * @throws IOException
   */
  void regionServerStartup(final DBServerInfo serverInfo)
  throws IOException {
    // Test for case where we get a region startup message from a regionserver
    // that has been quickly restarted but whose znode expiration handler has
    // not yet run, or from a server whose fail we are currently processing.
    // Test its host+port combo is present in serverAddresstoServerInfo.  If it
    // is, reject the server and trigger its expiration. The next time it comes
    // in, it should have been removed from serverAddressToServerInfo and queued
    // for processing by ProcessServerShutdown.
    DBServerInfo info = new DBServerInfo(serverInfo);
    String hostAndPort = info.getServerAddress().toString();
    DBServerInfo existingServer = haveServerWithSameHostAndPortAlready(info.getHostnamePort());
    if (existingServer != null) {
      String message = "Server start rejected; we already have " + hostAndPort +
        " registered; existingServer=" + existingServer + ", newServer=" + info;
      LOG.info(message);
      if (existingServer.getStartCode() < info.getStartCode()) {
        LOG.info("Triggering server recovery; existingServer looks stale");
        expireServer(existingServer);
      }
      throw new PleaseHoldException(message);
    }
    checkIsDead(info.getServerName(), "STARTUP");
    LOG.info("Received start message from: " + info.getServerName());
    recordNewServer(info);
  }

  private DBServerInfo haveServerWithSameHostAndPortAlready(final String hostnamePort) {
    synchronized (this.serversToServerInfo) {
      for (Map.Entry<String, DBServerInfo> e: this.serversToServerInfo.entrySet()) {
        if (e.getValue().getHostnamePort().equals(hostnamePort)) {
          return e.getValue();
        }
      }
    }
    return null;
  }

  /**
   * If this server is on the dead list, reject it with a LeaseStillHeldException
   * @param serverName Server name formatted as host_port_startcode.
   * @param what START or REPORT
   * @throws LeaseStillHeldException
   */
  private void checkIsDead(final String serverName, final String what)
  throws YouAreDeadException {
    if (!isDead(serverName)) return;
    String message = "Server " + what + " rejected; currently processing " +
      serverName + " as dead server";
    LOG.debug(message);
    throw new YouAreDeadException(message);
  }

  /**
   * Adds the HSI to the RS list and creates an empty load
   * @param info The region server informations
   */
  public void recordNewServer(DBServerInfo info) {
    recordNewServer(info, false);
  }

  /**
   * Adds the HSI to the RS list
   * @param info The region server informations
   * @param useInfoLoad True if the load from the info should be used
   *                    like under a master failover
   */
  void recordNewServer(DBServerInfo info, boolean useInfoLoad) {
    DBServerLoad load = useInfoLoad ? info.getLoad() : new DBServerLoad();
    String serverName = info.getServerName();
    info.setLoad(load);
    // We must set this watcher here because it can be set on a fresh start
    // or on a failover
    Watcher watcher = new ServerExpirer(new DBServerInfo(info));
    this.master.getPaxosWrapper().updateRSLocationGetWatch(info, watcher);
    this.serversToServerInfo.put(serverName, info);
    this.serversToLoad.put(serverName, load);
    synchronized (this.loadToServers) {
      Set<String> servers = this.loadToServers.get(load);
      if (servers == null) {
        servers = new HashSet<String>();
      }
      servers.add(serverName);
      this.loadToServers.put(load, servers);
    }
  }

  /**
   * Called to process the messages sent from the region server to the master
   * along with the heart beat.
   *
   * @param serverInfo
   * @param msgs
   * @param mostLoadedRegions Array of regions the region server is submitting
   * as candidates to be rebalanced, should it be overloaded
   * @return messages from master to region server indicating what region
   * server should do.
   *
   * @throws IOException
   */
  DBMsg [] regionServerReport(final DBServerInfo serverInfo,
    final DBMsg msgs[], final DBRegionInfo[] mostLoadedRegions)
  throws IOException {
    DBServerInfo info = new DBServerInfo(serverInfo);
    checkIsDead(info.getServerName(), "REPORT");
    if (msgs.length > 0) {
      if (msgs[0].isType(DBMsg.Type.MSG_REPORT_EXITING)) {
        processRegionServerExit(info, msgs);
        return DBMsg.EMPTY_HMSG_ARRAY;
      } else if (msgs[0].isType(DBMsg.Type.MSG_REPORT_QUIESCED)) {
        LOG.info("Region server " + info.getServerName() + " quiesced");
        this.quiescedServers.incrementAndGet();
      }
    }
    if (this.master.getShutdownRequested().get()) {
      if (quiescedServers.get() >= serversToServerInfo.size()) {
        // If the only servers we know about are meta servers, then we can
        // proceed with shutdown
        LOG.info("All user tables quiesced. Proceeding with shutdown");
        this.master.startShutdown();
      }
      if (!this.master.isClosed()) {
        if (msgs.length > 0 &&
            msgs[0].isType(DBMsg.Type.MSG_REPORT_QUIESCED)) {
          // Server is already quiesced, but we aren't ready to shut down
          // return empty response
          return DBMsg.EMPTY_HMSG_ARRAY;
        }
        // Tell the server to stop serving any user regions
        return new DBMsg [] {DBMsg.REGIONSERVER_QUIESCE};
      }
    }
    if (this.master.isClosed()) {
      // Tell server to shut down if we are shutting down.  This should
      // happen after check of MSG_REPORT_EXITING above, since region server
      // will send us one of these messages after it gets MSG_REGIONSERVER_STOP
      return new DBMsg [] {DBMsg.REGIONSERVER_STOP};
    }

    DBServerInfo storedInfo = this.serversToServerInfo.get(info.getServerName());
    if (storedInfo == null) {
      LOG.warn("Received report from unknown server -- telling it " +
        "to " + DBMsg.REGIONSERVER_STOP + ": " + info.getServerName());
      // The HBaseMaster may have been restarted.
      // Tell the RegionServer to abort!
      return new DBMsg[] {DBMsg.REGIONSERVER_STOP};
    } else if (storedInfo.getStartCode() != info.getStartCode()) {
      // This state is reachable if:
      //
      // 1) RegionServer A started
      // 2) RegionServer B started on the same machine, then
      //    clobbered A in regionServerStartup.
      // 3) RegionServer A returns, expecting to work as usual.
      //
      // The answer is to ask A to shut down for good.

      if (LOG.isDebugEnabled()) {
        LOG.debug("region server race condition detected: " +
            info.getServerName());
      }

      synchronized (this.serversToServerInfo) {
        removeServerInfo(info.getServerName());
        notifyServers();
      }

      return new DBMsg[] {DBMsg.REGIONSERVER_STOP};
    } else {
      return processRegionServerAllsWell(info, mostLoadedRegions, msgs);
    }
  }

  /*
   * Region server is exiting with a clean shutdown.
   *
   * In this case, the server sends MSG_REPORT_EXITING in msgs[0] followed by
   * a MSG_REPORT_CLOSE for each region it was serving.
   * @param serverInfo
   * @param msgs
   */
  private void processRegionServerExit(DBServerInfo serverInfo, DBMsg[] msgs) {
    synchronized (this.serversToServerInfo) {
      // This method removes ROOT/META from the list and marks them to be
      // reassigned in addition to other housework.
      if (removeServerInfo(serverInfo.getServerName())) {
        // Only process the exit message if the server still has registered info.
        // Otherwise we could end up processing the server exit twice.
        LOG.info("Region server " + serverInfo.getServerName() +
          ": MSG_REPORT_EXITING");
        // Get all the regions the server was serving reassigned
        // (if we are not shutting down).
        if (!master.closed.get()) {
          for (int i = 1; i < msgs.length; i++) {
            LOG.info("Processing " + msgs[i] + " from " +
              serverInfo.getServerName());
            assert msgs[i].getType() == DBMsg.Type.MSG_REGION_CLOSE;
            DBRegionInfo info = msgs[i].getRegionInfo();
            // Meta/root region offlining is handed in removeServerInfo above.
            if (!info.isMetaRegion()) {
              synchronized (master.getRegionManager()) {
                if (!master.getRegionManager().isOfflined(info.getRegionNameAsString())) {
                  master.getRegionManager().setUnassigned(info, true);
                } else {
                  master.getRegionManager().removeRegion(info);
                }
              }
            }
          }
        }
        // There should not be any regions in transition for this server - the
        // server should finish transitions itself before closing
        Map<String, RegionState> inTransition = master.getRegionManager()
            .getRegionsInTransitionOnServer(serverInfo.getServerName());
        for (Map.Entry<String, RegionState> entry : inTransition.entrySet()) {
          LOG.warn("Region server " + serverInfo.getServerName()
              + " shut down with region " + entry.getKey() + " in transition "
              + "state " + entry.getValue());
          master.getRegionManager().setUnassigned(entry.getValue().getRegionInfo(),
              true);
        }
      }
    }
  }

  /*
   *  RegionServer is checking in, no exceptional circumstances
   * @param serverInfo
   * @param mostLoadedRegions
   * @param msgs
   * @return
   * @throws IOException
   */
  private DBMsg[] processRegionServerAllsWell(DBServerInfo serverInfo,
      final DBRegionInfo[] mostLoadedRegions, DBMsg[] msgs)
  throws IOException {
    // Refresh the info object and the load information
    this.serversToServerInfo.put(serverInfo.getServerName(), serverInfo);
    DBServerLoad load = this.serversToLoad.get(serverInfo.getServerName());
    if (load != null) {
      this.master.getMetrics().incrementRequests(load.getNumberOfRequests());
      if (!load.equals(serverInfo.getLoad())) {
        updateLoadToServers(serverInfo.getServerName(), load);
      }
    }

    // Set the current load information
    load = serverInfo.getLoad();
    this.serversToLoad.put(serverInfo.getServerName(), load);
    synchronized (loadToServers) {
      Set<String> servers = this.loadToServers.get(load);
      if (servers == null) {
        servers = new HashSet<String>();
      }
      servers.add(serverInfo.getServerName());
      this.loadToServers.put(load, servers);
    }

    // Next, process messages for this server
    return processMsgs(serverInfo, mostLoadedRegions, msgs);
  }

  /**
   * Process all the incoming messages from a server that's contacted us.
   * Note that we never need to update the server's load information because
   * that has already been done in regionServerReport.
   * @param serverInfo
   * @param mostLoadedRegions
   * @param incomingMsgs
   * @return
   */
  @SuppressWarnings("deprecation")
  private DBMsg[] processMsgs(DBServerInfo serverInfo,
      DBRegionInfo[] mostLoadedRegions, DBMsg incomingMsgs[]) {
    ArrayList<DBMsg> returnMsgs = new ArrayList<DBMsg>();
    if (serverInfo.getServerAddress() == null) {
      throw new NullPointerException("Server address cannot be null; " +
        "bigdb-958 debugging");
    }
    // Get reports on what the RegionServer did.
    // Be careful that in message processors we don't throw exceptions that
    // break the switch below because then we might drop messages on the floor.
    int openingCount = 0;
    for (int i = 0; i < incomingMsgs.length; i++) {
      DBRegionInfo region = incomingMsgs[i].getRegionInfo();
      LOG.info("Processing " + incomingMsgs[i] + " from " +
        serverInfo.getServerName() + "; " + (i + 1) + " of " +
        incomingMsgs.length);
      if (!this.master.getRegionServerOperationQueue().
          process(serverInfo, incomingMsgs[i])) {
        continue;
      }
      switch (incomingMsgs[i].getType()) {
        case MSG_REPORT_PROCESS_OPEN:
          openingCount++;
          break;

        case MSG_REPORT_OPEN:
          processRegionOpen(serverInfo, region, returnMsgs);
          break;

        case MSG_REPORT_CLOSE:
          processRegionClose(region);
          break;

        case MSG_REPORT_SPLIT:
          processSplitRegion(region, incomingMsgs[++i].getRegionInfo(),
            incomingMsgs[++i].getRegionInfo());
          break;

        case MSG_REPORT_SPLIT_INCLUDES_DAUGHTERS:
          processSplitRegion(region, incomingMsgs[i].getDaughterA(),
            incomingMsgs[i].getDaughterB());
          break;

        default:
          LOG.warn("Impossible state during message processing. Instruction: " +
            incomingMsgs[i].getType());
      }
    }

    synchronized (this.master.getRegionManager()) {
      // Tell the region server to close regions that we have marked for closing.
      for (DBRegionInfo i:
        this.master.getRegionManager().getMarkedToClose(serverInfo.getServerName())) {
        returnMsgs.add(new DBMsg(DBMsg.Type.MSG_REGION_CLOSE, i));
        // Transition the region from toClose to closing state
        this.master.getRegionManager().setPendingClose(i.getRegionNameAsString());
      }

      // Figure out what the RegionServer ought to do, and write back.

      // Should we tell it close regions because its overloaded?  If its
      // currently opening regions, leave it alone till all are open.
      if (openingCount < this.nobalancingCount) {
        this.master.getRegionManager().assignRegions(serverInfo, mostLoadedRegions,
          returnMsgs);
      }

      // Send any pending table actions.
      this.master.getRegionManager().applyActions(serverInfo, returnMsgs);
    }
    return returnMsgs.toArray(new DBMsg[returnMsgs.size()]);
  }

  /**
   * A region has split.
   *
   * @param region
   * @param splitA
   * @param splitB
   * @param returnMsgs
   */
  private void processSplitRegion(DBRegionInfo region, DBRegionInfo a, DBRegionInfo b) {
    synchronized (master.getRegionManager()) {
      // Cancel any actions pending for the affected region.
      // This prevents the master from sending a SPLIT message if the table
      // has already split by the region server.
      this.master.getRegionManager().endActions(region.getRegionName());
      assignSplitDaughter(a);
      assignSplitDaughter(b);
      if (region.isMetaTable()) {
        // A meta region has split.
        this. master.getRegionManager().offlineMetaRegionWithStartKey(region.getStartKey());
        this.master.getRegionManager().incrementNumMetaRegions();
      }
    }
  }

  /**
   * Assign new daughter-of-a-split UNLESS its already been assigned.
   * It could have been assigned already in rare case where there was a large
   * gap between insertion of the daughter region into .META. by the
   * splitting regionserver and receipt of the split message in master (See
   * HBASE-1784).
   * @param hri Region to assign.
   */
  private void assignSplitDaughter(final DBRegionInfo hri) {
    MetaRegion mr =
      this.master.getRegionManager().getFirstMetaRegionForRegion(hri);
    Get g = new Get(hri.getRegionName());
    g.addFamily(DBConstants.CATALOG_FAMILY);
    try {
      DBRegionInterface server =
        this.master.getServerConnection().getDBRegionConnection(mr.getServer());
      Result r = server.get(mr.getRegionName(), g);
      // If size > 3 -- presume regioninfo, startcode and server -- then presume
      // that this daughter already assigned and return.
      if (r.size() >= 3) return;
    } catch (IOException e) {
      LOG.warn("Failed get on " + DBConstants.CATALOG_FAMILY_STR +
        "; possible double-assignment?", e);
    }
    this.master.getRegionManager().setUnassigned(hri, false);
  }

  /**
   * Region server is reporting that a region is now opened
   * @param serverInfo
   * @param region
   * @param returnMsgs
   */
  public void processRegionOpen(DBServerInfo serverInfo,
      DBRegionInfo region, ArrayList<DBMsg> returnMsgs) {
    boolean duplicateAssignment = false;
    synchronized (master.getRegionManager()) {
      if (!this.master.getRegionManager().isUnassigned(region) &&
          !this.master.getRegionManager().isPendingOpen(region.getRegionNameAsString())) {
        if (region.isRootRegion()) {
          // Root region
          DBServerAddress rootServer =
            this.master.getRegionManager().getRootRegionLocation();
          if (rootServer != null) {
            if (rootServer.compareTo(serverInfo.getServerAddress()) == 0) {
              // A duplicate open report from the correct server
              return;
            }
            // We received an open report on the root region, but it is
            // assigned to a different server
            duplicateAssignment = true;
          }
        } else {
          // Not root region. If it is not a pending region, then we are
          // going to treat it as a duplicate assignment, although we can't
          // tell for certain that's the case.
          if (this.master.getRegionManager().isPendingOpen(
              region.getRegionNameAsString())) {
            // A duplicate report from the correct server
            return;
          }
          duplicateAssignment = true;
        }
      }

      if (duplicateAssignment) {
        LOG.warn("region server " + serverInfo.getServerAddress().toString() +
          " should not have opened region " + Bytes.toString(region.getRegionName()));

        // This Region should not have been opened.
        // Ask the server to shut it down, but don't report it as closed.
        // Otherwise the DBMaster will think the Region was closed on purpose,
        // and then try to reopen it elsewhere; that's not what we want.
        returnMsgs.add(new DBMsg(DBMsg.Type.MSG_REGION_CLOSE_WITHOUT_REPORT,
          region, "Duplicate assignment".getBytes()));
      } else {
        if (region.isRootRegion()) {
          // it was assigned, and it's not a duplicate assignment, so take it out
          // of the unassigned list.
          this.master.getRegionManager().removeRegion(region);

          // Store the Root Region location (in memory)
          DBServerAddress rootServer = serverInfo.getServerAddress();
          this.master.getServerConnection().setRootRegionLocation(
            new DBRegionLocation(region, rootServer));
          this.master.getRegionManager().setRootRegionLocation(rootServer);
        } else {
          // Note that the table has been assigned and is waiting for the
          // meta table to be updated.
          this.master.getRegionManager().setOpen(region.getRegionNameAsString());
          RegionServerOperation op =
            new ProcessRegionOpen(master, serverInfo, region);
          this.master.getRegionServerOperationQueue().put(op);
        }
      }
    }
  }

  /**
   * @param region
   * @throws Exception
   */
  public void processRegionClose(DBRegionInfo region) {
    synchronized (this.master.getRegionManager()) {
      if (region.isRootRegion()) {
        // Root region
        this.master.getRegionManager().unsetRootRegion();
        if (region.isOffline()) {
          // Can't proceed without root region. Shutdown.
          LOG.fatal("root region is marked offline");
          this.master.shutdown();
          return;
        }

      } else if (region.isMetaTable()) {
        // Region is part of the meta table. Remove it from onlineMetaRegions
        this.master.getRegionManager().offlineMetaRegionWithStartKey(region.getStartKey());
      }

      boolean offlineRegion =
        this.master.getRegionManager().isOfflined(region.getRegionNameAsString());
      boolean reassignRegion = !region.isOffline() && !offlineRegion;

      // NOTE: If the region was just being closed and not offlined, we cannot
      //       mark the region unassignedRegions as that changes the ordering of
      //       the messages we've received. In this case, a close could be
      //       processed before an open resulting in the master not agreeing on
      //       the region's state.
      this.master.getRegionManager().setClosed(region.getRegionNameAsString());
      RegionServerOperation op =
        new ProcessRegionClose(master, region, offlineRegion, reassignRegion);
      this.master.getRegionServerOperationQueue().put(op);
    }
  }

  /** Update a server load information because it's shutting down*/
  private boolean removeServerInfo(final String serverName) {
    boolean infoUpdated = false;
    DBServerInfo info = this.serversToServerInfo.remove(serverName);
    // Only update load information once.
    // This method can be called a couple of times during shutdown.
    if (info != null) {
      LOG.info("Removing server's info " + serverName);
      this.master.getRegionManager().offlineMetaServer(info.getServerAddress());

      //HBASE-1928: Check whether this server has been transitioning the ROOT table
      if (this.master.getRegionManager().isRootInTransitionOnThisServer(serverName)) {
         this.master.getRegionManager().unsetRootRegion();
         this.master.getRegionManager().reassignRootRegion();
      }

      //HBASE-1928: Check whether this server has been transitioning the META table
      DBRegionInfo metaServerRegionInfo = this.master.getRegionManager().getMetaServerRegionInfo (serverName);
      if (metaServerRegionInfo != null) {
         this.master.getRegionManager().setUnassigned(metaServerRegionInfo, true);
      }

      infoUpdated = true;
      // update load information
      updateLoadToServers(serverName, this.serversToLoad.remove(serverName));
    }
    return infoUpdated;
  }

  private void updateLoadToServers(final String serverName,
      final DBServerLoad load) {
    if (load == null) return;
    synchronized (this.loadToServers) {
      Set<String> servers = this.loadToServers.get(load);
      if (servers != null) {
        servers.remove(serverName);
        if (servers.size() > 0)
          this.loadToServers.put(load, servers);
        else
          this.loadToServers.remove(load);
      }
    }
  }

  /**
   * Compute the average load across all region servers.
   * Currently, this uses a very naive computation - just uses the number of
   * regions being served, ignoring stats about number of requests.
   * @return the average load
   */
  public double getAverageLoad() {
    int totalLoad = 0;
    int numServers = 0;
    double averageLoad = 0.0;
    synchronized (serversToLoad) {
      numServers = serversToLoad.size();
      for (DBServerLoad load : serversToLoad.values()) {
        totalLoad += load.getNumberOfRegions();
      }
      averageLoad = (double)totalLoad / (double)numServers;
    }
    return averageLoad;
  }

  /** @return the number of active servers */
  public int numServers() {
    return this.serversToServerInfo.size();
  }

  /**
   * @param name server name
   * @return DBServerInfo for the given server address
   */
  public DBServerInfo getServerInfo(String name) {
    return this.serversToServerInfo.get(name);
  }

  /**
   * @return Read-only map of servers to serverinfo.
   */
  public Map<String, DBServerInfo> getServersToServerInfo() {
    synchronized (this.serversToServerInfo) {
      return Collections.unmodifiableMap(this.serversToServerInfo);
    }
  }

  /**
   * @param hsa
   * @return The DBServerInfo whose DBServerAddress is <code>hsa</code> or null
   * if nothing found.
   */
  public DBServerInfo getDBServerInfo(final DBServerAddress hsa) {
    synchronized(this.serversToServerInfo) {
      // TODO: This is primitive.  Do a better search.
      for (Map.Entry<String, DBServerInfo> e: this.serversToServerInfo.entrySet()) {
        if (e.getValue().getServerAddress().equals(hsa)) return e.getValue();
      }
    }
    return null;
  }

  /**
   * @return Read-only map of servers to load.
   */
  public Map<String, DBServerLoad> getServersToLoad() {
    synchronized (this.serversToLoad) {
      return Collections.unmodifiableMap(serversToLoad);
    }
  }

  /**
   * @return Read-only map of load to servers.
   */
  public SortedMap<DBServerLoad, Set<String>> getLoadToServers() {
    synchronized (this.loadToServers) {
      return Collections.unmodifiableSortedMap(this.loadToServers);
    }
  }

  /**
   * Wakes up threads waiting on serversToServerInfo
   */
  public void notifyServers() {
    synchronized (this.serversToServerInfo) {
      this.serversToServerInfo.notifyAll();
    }
  }

  /*
   * Wait on regionservers to report in
   * with {@link #regionServerReport(DBServerInfo, DBMsg[])} so they get notice
   * the master is going down.  Waits until all region servers come back with
   * a MSG_REGIONSERVER_STOP.
   */
  void letRegionServersShutdown() {
    if (!master.checkFileSystem()) {
      // Forget waiting for the region servers if the file system has gone
      // away. Just exit as quickly as possible.
      return;
    }
    synchronized (serversToServerInfo) {
      while (serversToServerInfo.size() > 0) {
        LOG.info("Waiting on following regionserver(s) to go down " +
          this.serversToServerInfo.values());
        try {
          this.serversToServerInfo.wait(500);
        } catch (InterruptedException e) {
          // continue
        }
      }
    }
  }

  /** Watcher triggered when a RS znode is deleted */
  private class ServerExpirer implements Watcher {
    private DBServerInfo server;

    ServerExpirer(final DBServerInfo hsi) {
      this.server = hsi;
    }

    public void process(WatchedEvent event) {
      if (!event.getType().equals(EventType.NodeDeleted)) {
        LOG.warn("Unexpected event=" + event);
        return;
      }
      LOG.info(this.server.getServerName() + " znode expired");
      expireServer(this.server);
    }
  }

  /**
   * Expire the passed server.  Add it to list of deadservers and queue a
   * shutdown processing.
   */
  private synchronized void expireServer(final DBServerInfo hsi) {
    // First check a server to expire.  ServerName is of the form:
    // <hostname> , <port> , <startcode>
    String serverName = hsi.getServerName();
    DBServerInfo info = this.serversToServerInfo.get(serverName);
    if (info == null) {
      LOG.warn("No DBServerInfo for " + serverName);
      return;
    }
    if (this.deadServers.contains(serverName)) {
      LOG.warn("Already processing shutdown of " + serverName);
      return;
    }
    // Remove the server from the known servers lists and update load info
    this.serversToServerInfo.remove(serverName);
    DBServerLoad load = this.serversToLoad.remove(serverName);
    if (load != null) {
      synchronized (this.loadToServers) {
        Set<String> servers = this.loadToServers.get(load);
        if (servers != null) {
          servers.remove(serverName);
          if (servers.isEmpty()) this.loadToServers.remove(load);
        }
      }
    }
    // Add to dead servers and queue a shutdown processing.
    LOG.debug("Added=" + serverName +
      " to dead servers, added shutdown processing operation");
    this.deadServers.add(serverName);
    this.master.getRegionServerOperationQueue().
      put(new ProcessServerShutdown(master, info));
  }

  /**
   * @param serverName
   */
  void removeDeadServer(String serverName) {
    this.deadServers.remove(serverName);
  }

  /**
   * @param serverName
   * @return true if server is dead
   */
  public boolean isDead(final String serverName) {
    return isDead(serverName, false);
  }

  /**
   * @param serverName Servername as either <code>host:port</code> or
   * <code>host,port,startcode</code>.
   * @param hostAndPortOnly True if <code>serverName</code> is host and
   * port only (<code>host:port</code>) and if so, then we do a prefix compare
   * (ignoring start codes) looking for dead server.
   * @return true if server is dead
   */
  boolean isDead(final String serverName, final boolean hostAndPortOnly) {
    return isDead(this.deadServers, serverName, hostAndPortOnly);
  }

  static boolean isDead(final Set<String> deadServers,
      final String serverName, final boolean hostAndPortOnly) {
    return DBServerInfo.isServer(deadServers, serverName, hostAndPortOnly);
  }

  Set<String> getDeadServers() {
    return this.deadServers;
  }

  /**
   * Add to the passed <code>m</code> servers that are loaded less than
   * <code>l</code>.
   * @param l
   * @param m
   */
  void getLightServers(final DBServerLoad l,
      SortedMap<DBServerLoad, Set<String>> m) {
    synchronized (this.loadToServers) {
      m.putAll(this.loadToServers.headMap(l));
    }
  }

  public boolean canAssignUserRegions() {
    if (minimumServerCount == 0) {
      return true;
    }
    return (numServers() >= minimumServerCount);
  }

  public void setMinimumServerCount(int minimumServerCount) {
    this.minimumServerCount = minimumServerCount;
  }
}

