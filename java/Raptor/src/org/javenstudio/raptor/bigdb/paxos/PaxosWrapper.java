package org.javenstudio.raptor.bigdb.paxos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBServerAddress;
import org.javenstudio.raptor.bigdb.DBServerInfo;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.paxos.PaxosException;
import org.javenstudio.raptor.paxos.WatchedEvent;
import org.javenstudio.raptor.paxos.Watcher;
import org.javenstudio.raptor.paxos.Paxos;
import org.javenstudio.raptor.paxos.PaxosDefs.Ids;
import org.javenstudio.raptor.paxos.Paxos.States;
import org.javenstudio.raptor.paxos.data.Stat;
import org.javenstudio.raptor.paxos.server.CreateMode;

/**
 * Wraps a Paxos instance and adds BigDB specific functionality.
 *
 * This class provides methods to:
 * - read/write/delete the root region location in Paxos.
 * - set/check out of safe mode flag.
 *
 * ------------------------------------------
 * The following STATIC ZNodes are created:
 * ------------------------------------------
 * - parentZNode     : All the BigDB directories are hosted under this parent
 *                     node, default = "/bigdb"
 * - rsZNode         : This is the directory where the RS's create ephemeral
 *                     nodes. The master watches these nodes, and their expiry
 *                     indicates RS death. The default location is "/bigdb/rs"
 *
 * ------------------------------------------
 * The following DYNAMIC ZNodes are created:
 * ------------------------------------------
 * - rootRegionZNode     : Specifies the RS hosting root.
 * - masterElectionZNode : ZNode used for election of the primary master when
 *                         there are secondaries. All the masters race to write
 *                         their addresses into this location, the one that
 *                         succeeds is the primary. Others block.
 * - clusterStateZNode   : Determines if the cluster is running. Its default
 *                         location is "/bigdb/shutdown". It always has a value
 *                         of "up". If present with the valus, cluster is up
 *                         and running. If deleted, the cluster is shutting
 *                         down.
 * - rgnsInTransitZNode  : All the nodes under this node are names of regions
 *                         in transition. The first byte of the data for each
 *                         of these nodes is the event type. This is used to
 *                         deserialize the rest of the data.
 */
public class PaxosWrapper implements Watcher {
  protected static final Logger LOG = Logger.getLogger(PaxosWrapper.class);

  // instances of the watcher
  private static Map<String,PaxosWrapper> INSTANCES =
    new HashMap<String,PaxosWrapper>();
  // lock for ensuring a singleton per instance type
  private static Lock createLock = new ReentrantLock();
  // name of this instance
  private String instanceName;

  // TODO: Replace this with Paxos constant when PAXOS-277 is resolved.
  private static final char ZNODE_PATH_SEPARATOR = '/';

  private String quorumServers = null;
  private final int sessionTimeout;
  private Paxos paxos;

  /*
   * All the BigDB directories are hosted under this parent
   */
  private final String parentZNode;
  /*
   * Specifies the RS hosting root
   */
  private final String rootRegionZNode;
  /*
   * This is the directory where the RS's create ephemeral nodes. The master
   * watches these nodes, and their expiry indicates RS death.
   */
  private final String rsZNode;
  /*
   * ZNode used for election of the primary master when there are secondaries.
   */
  private final String masterElectionZNode;
  /*
   * State of the cluster - if up and running or shutting down
   */
  private final String clusterStateZNode;

  private List<Watcher> listeners = new ArrayList<Watcher>();

  // return the singleton given the name of the instance
  public static PaxosWrapper getInstance(Configuration conf, String name) {
    name = getPaxosClusterKey(conf, name);
    return INSTANCES.get(name);
  }
  
  // creates only one instance
  public static PaxosWrapper createInstance(Configuration conf, String name) {
    if (getInstance(conf, name) != null) 
      return getInstance(conf, name);
    
    PaxosWrapper.createLock.lock();
    try {
      if (getInstance(conf, name) == null) {
        try {
          String fullname = getPaxosClusterKey(conf, name);
          PaxosWrapper instance = new PaxosWrapper(conf, fullname);
          INSTANCES.put(fullname, instance);
        }
        catch (Exception e) {
          if (LOG.isErrorEnabled())
            LOG.error("<" + name + ">" + "Error creating a PaxosWrapper: " + e, e);
        }
      }
    }
    finally {
      createLock.unlock();
    }
    return getInstance(conf, name);
  }

  /**
   * Create a PaxosWrapper. The Paxos wrapper listens to all messages
   * from Paxos, and notifies all the listeners about all the messages. Any
   * component can subscribe to these messages by adding itself as a listener,
   * and remove itself from being a listener.
   *
   * @param conf BigDBConfiguration to read settings from.
   * @throws IOException If a connection error occurs.
   */
  private PaxosWrapper(Configuration conf, String instanceName)
      throws IOException {
    this.instanceName = instanceName;
    Properties properties = DBQuorumPeer.makePaxosProps(conf);
    quorumServers = DBQuorumPeer.getPaxosQuorumServersString(properties);
    if (quorumServers == null) {
      throw new IOException("Could not read quorum servers from " +
                            DBConstants.PAXOS_CONFIG_NAME);
    }
    sessionTimeout = conf.getInt("paxos.session.timeout", 60 * 1000);
    reconnectToPaxos();

    parentZNode = conf.get(DBConstants.PAXOS_ZNODE_PARENT, DBConstants.DEFAULT_PAXOS_ZNODE_PARENT);

    String rootServerZNodeName = conf.get("paxos.znode.rootserver", "root-region-server");
    String rsZNodeName         = conf.get("paxos.znode.rs", "rs");
    String masterAddressZNodeName = conf.get("paxos.znode.master", "master");
    String stateZNodeName      = conf.get("paxos.znode.state", "shutdown");
    @SuppressWarnings("unused")
	String regionsInTransitZNodeName = conf.get("paxos.znode.regionInTransition", "UNASSIGNED");

    rootRegionZNode     = getZNode(parentZNode, rootServerZNodeName);
    rsZNode             = getZNode(parentZNode, rsZNodeName);
    masterElectionZNode = getZNode(parentZNode, masterAddressZNodeName);
    clusterStateZNode   = getZNode(parentZNode, stateZNodeName);
  }

  public void reconnectToPaxos() throws IOException {
    try {
      if (LOG.isInfoEnabled())
    	  LOG.info("Reconnecting to paxos");
      if (paxos != null) {
        paxos.close();
        if (LOG.isDebugEnabled())
        	LOG.debug("<" + instanceName + ">" + "Closed existing paxos client");
      }
      paxos = new Paxos(quorumServers, sessionTimeout, this);
      if (LOG.isDebugEnabled())
    	  LOG.debug("<" + instanceName + ">" + "Connected to paxos again");
    } catch (IOException e) {
      if (LOG.isErrorEnabled())
    	  LOG.error("<" + instanceName + ">" + "Failed to create Paxos object: " + e);
      throw new IOException(e);
    } catch (InterruptedException e) {
      if (LOG.isErrorEnabled())
    	  LOG.error("<" + instanceName + ">" + "Error closing Paxos connection: " + e);
      throw new IOException(e);
    }
  }

  public String getClusterStateZNode() { return clusterStateZNode; }
  
  public synchronized void registerListener(Watcher watcher) {
    listeners.add(watcher);
  }

  public synchronized void unregisterListener(Watcher watcher) {
    listeners.remove(watcher);
  }

  /**
   * This is the primary Paxos watcher
   * @see org.javenstudio.raptor.paxos.Watcher#process(org.javenstudio.raptor.paxos.WatchedEvent)
   */
  @Override
  public synchronized void process(WatchedEvent event) {
    for(Watcher w : listeners) {
      try {
        w.process(event);
      } catch (Throwable t) {
    	if (LOG.isErrorEnabled())
          LOG.error("<"+instanceName+">" + "Paxos updates listener threw an exception in process()", t);
      }
    }
  }

  /** @return String dump of everything in Paxos. */
  public String dump() {
    StringBuilder sb = new StringBuilder();
    sb.append("\nBigDB tree in Paxos is rooted at ").append(parentZNode);
    sb.append("\n  Cluster up? ").append(exists(clusterStateZNode, true));
    sb.append("\n  Master address: ").append(readMasterAddress(null));
    sb.append("\n  Region server holding ROOT: ").append(readRootRegionLocation());
    sb.append("\n  Region servers:");
    for (DBServerAddress address : scanRSDirectory()) {
      sb.append("\n    - ").append(address);
    }
    sb.append("\n  Quorum Server Statistics:");
    String[] servers = quorumServers.split(",");
    for (String server : servers) {
      sb.append("\n    - ").append(server);
      try {
        String[] stat = getServerStats(server);
        for (String s : stat) {
          sb.append("\n        ").append(s);
        }
      } catch (Exception e) {
        sb.append("\n        ERROR: ").append(e.getMessage());
      }
    }
    return sb.toString();
  }

  /**
   * Gets the statistics from the given server. Uses a 1 minute timeout.
   *
   * @param server  The server to get the statistics from.
   * @return The array of response strings.
   * @throws IOException When the socket communication fails.
   */
  public String[] getServerStats(String server)
      throws IOException {
    return getServerStats(server, 60 * 1000);
  }

  /**
   * Gets the statistics from the given server.
   *
   * @param server  The server to get the statistics from.
   * @param timeout  The socket timeout to use.
   * @return The array of response strings.
   * @throws IOException When the socket communication fails.
   */
  public String[] getServerStats(String server, int timeout)
      throws IOException {
    String[] sp = server.split(":");
    Socket socket = new Socket(sp[0],
      sp.length > 1 ? Integer.parseInt(sp[1]) : 2181);
    socket.setSoTimeout(timeout);
    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    BufferedReader in = new BufferedReader(new InputStreamReader(
      socket.getInputStream()));
    out.println("stat");
    out.flush();
    ArrayList<String> res = new ArrayList<String>();
    while (true) {
      String line = in.readLine();
      if (line != null) res.add(line);
      else break;
    }
    socket.close();
    return res.toArray(new String[res.size()]);
  }

  public boolean exists(String znode, boolean watch) {
    try {
      return paxos.exists(getZNode(parentZNode, znode), watch?this:null) != null;
    } catch (PaxosException.SessionExpiredException e) {
      // if the session has expired try to reconnect to Paxos, then perform query
      try {
        // TODO: Paxos-REFACTOR: We should not reconnect - we should just quit and restart.
        reconnectToPaxos();
        return paxos.exists(getZNode(parentZNode, znode), watch?this:null) != null;
      } catch (IOException e1) {
    	if (LOG.isErrorEnabled())
          LOG.error("Error reconnecting to paxos", e1);
        throw new RuntimeException("Error reconnecting to paxos", e1);
      } catch (PaxosException e1) {
    	if (LOG.isErrorEnabled())
          LOG.error("Error reading after reconnecting to paxos", e1);
        throw new RuntimeException("Error reading after reconnecting to paxos", e1);
      } catch (InterruptedException e1) {
    	if (LOG.isErrorEnabled())
          LOG.error("Error reading after reconnecting to paxos", e1);
        throw new RuntimeException("Error reading after reconnecting to paxos", e1);
      }
    } catch (PaxosException e) {
      return false;
    } catch (InterruptedException e) {
      return false;
    }
  }

  /** @return Paxos used by this wrapper. */
  public Paxos getPaxos() {
    return paxos;
  }

  /**
   * This is for testing PaxosException.SessionExpiredException.
   * See HBASE-1232.
   * @return long session ID of this Paxos session.
   */
  public long getSessionID() {
    return paxos.getSessionId();
  }

  /**
   * This is for testing PaxosException.SessionExpiredException.
   * See HBASE-1232.
   * @return byte[] password of this Paxos session.
   */
  public byte[] getSessionPassword() {
    return paxos.getSessionPasswd();
  }

  /** @return host:port list of quorum servers. */
  public String getQuorumServers() {
    return quorumServers;
  }

  /** @return true if currently connected to Paxos, false otherwise. */
  public boolean isConnected() {
    return paxos.getState() == States.CONNECTED;
  }

  /**
   * Read location of server storing root region.
   * @return DBServerAddress pointing to server serving root region or null if
   *         there was a problem reading the ZNode.
   */
  public DBServerAddress readRootRegionLocation() {
    return readAddress(rootRegionZNode, null);
  }

  /**
   * Read address of master server.
   * @return DBServerAddress of master server.
   * @throws IOException if there's a problem reading the ZNode.
   */
  public DBServerAddress readMasterAddressOrThrow() throws IOException {
    return readAddressOrThrow(masterElectionZNode, null);
  }

  /**
   * Read master address and set a watch on it.
   * @param watcher Watcher to set on master address ZNode if not null.
   * @return DBServerAddress of master or null if there was a problem reading the
   *         ZNode. The watcher is set only if the result is not null.
   */
  public DBServerAddress readMasterAddress(Watcher watcher) {
    return readAddress(masterElectionZNode, watcher);
  }

  /**
   * Watch the state of the cluster, up or down
   * @param watcher Watcher to set on cluster state node
   */
  public void setClusterStateWatch(Watcher watcher) {
    try {
      paxos.exists(clusterStateZNode, watcher == null ? this : watcher);
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to check on ZNode " + clusterStateZNode, e);
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to check on ZNode " + clusterStateZNode, e);
    }
  }

  /**
   * Set the cluster state, up or down
   * @param up True to write the node, false to delete it
   * @return true if it worked, else it's false
   */
  public boolean setClusterState(boolean up) {
    if (!ensureParentExists(clusterStateZNode)) {
      return false;
    }
    try {
      if(up) {
        byte[] data = Bytes.toBytes("up");
        paxos.create(clusterStateZNode, data,
            Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        if (LOG.isDebugEnabled())
          LOG.debug("<" + instanceName + ">" + "State node wrote in Paxos");
      } else {
        paxos.delete(clusterStateZNode, -1);
        if (LOG.isDebugEnabled())
          LOG.debug("<" + instanceName + ">" + "State node deleted in Paxos");
      }
      return true;
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to set state node in Paxos", e);
    } catch (PaxosException e) {
      if (e.code() == PaxosException.Code.NODEEXISTS) {
    	if (LOG.isDebugEnabled())
          LOG.debug("<" + instanceName + ">" + "State node exists.");
      } else {
    	if (LOG.isWarnEnabled())
          LOG.warn("<" + instanceName + ">" + "Failed to set state node in Paxos", e);
      }
    }

    return false;
  }

  /**
   * Set a watcher on the master address ZNode. The watcher will be set unless
   * an exception occurs with Paxos.
   * @param watcher Watcher to set on master address ZNode.
   * @return true if watcher was set, false otherwise.
   */
  public boolean watchMasterAddress(Watcher watcher) {
    try {
      paxos.exists(masterElectionZNode, watcher);
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to set watcher on ZNode " + masterElectionZNode, e);
      return false;
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to set watcher on ZNode " + masterElectionZNode, e);
      return false;
    }
    if (LOG.isDebugEnabled())
      LOG.debug("<" + instanceName + ">" + "Set watcher on master address ZNode " + masterElectionZNode);
    return true;
  }
  
  /**
   * @return true if paxos has a master address.
   */
  public boolean masterAddressExists() {
    return checkExistenceOf(masterElectionZNode);
  }

  private DBServerAddress readAddress(String znode, Watcher watcher) {
    try {
      if (LOG.isDebugEnabled())
        LOG.debug("<" + instanceName + ">" + "Trying to read " + znode);
      return readAddressOrThrow(znode, watcher);
    } catch (IOException e) {
      if (LOG.isDebugEnabled())
        LOG.debug("<" + instanceName + ">" + "Failed to read " + e.getMessage());
      return null;
    }
  }

  private DBServerAddress readAddressOrThrow(String znode, Watcher watcher) throws IOException {
    byte[] data;
    try {
      data = paxos.getData(znode, watcher, null);
    } catch (InterruptedException e) {
      throw new IOException(e);
    } catch (PaxosException e) {
      throw new IOException(e);
    }

    String addressString = Bytes.toString(data);
    if (LOG.isDebugEnabled())
      LOG.debug("<" + instanceName + ">" + "Read ZNode " + znode + " got " + addressString);
    return new DBServerAddress(addressString);
  }

  /**
   * Make sure this znode exists by creating it if it's missing
   * @param znode full path to znode
   * @return true if it works
   */
  public boolean ensureExists(final String znode) {
    try {
      Stat stat = paxos.exists(znode, false);
      if (stat != null) {
        return true;
      }
      paxos.create(znode, new byte[0],
                       Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      if (LOG.isDebugEnabled())
        LOG.debug("<" + instanceName + ">" + "Created ZNode " + znode);
      return true;
    } catch (PaxosException.NodeExistsException e) {
      return true;      // ok, move on.
    } catch (PaxosException.NoNodeException e) {
      return ensureParentExists(znode) && ensureExists(znode);
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("<" + instanceName + ">" + "Failed to create " + znode +
          " -- check quorum servers, currently=" + this.quorumServers, e);
      }
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("<" + instanceName + ">" + "Failed to create " + znode +
          " -- check quorum servers, currently=" + this.quorumServers, e);
      }
    }
    return false;
  }

  private boolean ensureParentExists(final String znode) {
    int index = znode.lastIndexOf(ZNODE_PATH_SEPARATOR);
    if (index <= 0)   // Parent is root, which always exists.
      return true;
    
    return ensureExists(znode.substring(0, index));
  }

  /**
   * Delete ZNode containing root region location.
   * @return true if operation succeeded, false otherwise.
   */
  public boolean deleteRootRegionLocation()  {
    if (!ensureParentExists(rootRegionZNode)) {
      return false;
    }

    try {
      deleteZNode(rootRegionZNode);
      return true;
    } catch (PaxosException.NoNodeException e) {
      return true;    // ok, move on.
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to delete " + rootRegionZNode + ": " + e);
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to delete " + rootRegionZNode + ": " + e);
    }

    return false;
  }

  /**
   * Unrecursive deletion of specified znode
   * @param znode
   * @throws PaxosException
   * @throws InterruptedException
   */
  public void deleteZNode(String znode)
      throws PaxosException, InterruptedException {
    deleteZNode(znode, false);
  }

  /**
   * Optionnally recursive deletion of specified znode
   * @param znode
   * @param recursive
   * @throws PaxosException
   * @throws InterruptedException
   */
  public void deleteZNode(String znode, boolean recursive)
    throws PaxosException, InterruptedException {
    if (recursive) {
      if (LOG.isInfoEnabled())
        LOG.info("<" + instanceName + ">" + "deleteZNode get children for " + znode);
      List<String> znodes = this.paxos.getChildren(znode, false);
      if (znodes != null && znodes.size() > 0) {
        for (String child : znodes) {
          String childFullPath = getZNode(znode, child);
          if (LOG.isInfoEnabled())
            LOG.info("<" + instanceName + ">" + "deleteZNode recursive call " + childFullPath);
          this.deleteZNode(childFullPath, true);
        }
      }
    }
    this.paxos.delete(znode, -1);
    if (LOG.isDebugEnabled())
      LOG.debug("<" + instanceName + ">" + "Deleted ZNode " + znode);
  }

  private boolean createRootRegionLocation(String address) {
    byte[] data = Bytes.toBytes(address);
    try {
      paxos.create(rootRegionZNode, data, Ids.OPEN_ACL_UNSAFE,
                       CreateMode.PERSISTENT);
      if (LOG.isDebugEnabled())
        LOG.debug("<" + instanceName + ">" + "Created ZNode " + rootRegionZNode + " with data " + address);
      return true;
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to create root region in Paxos: " + e);
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to create root region in Paxos: " + e);
    }

    return false;
  }

  private boolean updateRootRegionLocation(String address) {
    byte[] data = Bytes.toBytes(address);
    try {
      paxos.setData(rootRegionZNode, data, -1);
      if (LOG.isDebugEnabled())
        LOG.debug("<" + instanceName + ">" + "SetData of ZNode " + rootRegionZNode + " with " + address);
      return true;
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to set root region location in Paxos: " + e);
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to set root region location in Paxos: " + e);
    }

    return false;
  }

  /**
   * Write root region location to Paxos. If address is null, delete ZNode.
   * containing root region location.
   * @param address DBServerAddress to write to Paxos.
   * @return true if operation succeeded, false otherwise.
   */
  public boolean writeRootRegionLocation(DBServerAddress address) {
    if (address == null) 
      return deleteRootRegionLocation();

    if (!ensureParentExists(rootRegionZNode)) 
      return false;

    String addressString = address.toString();

    if (checkExistenceOf(rootRegionZNode)) 
      return updateRootRegionLocation(addressString);

    return createRootRegionLocation(addressString);
  }

  /**
   * Write address of master to Paxos.
   * @param address DBServerAddress of master.
   * @return true if operation succeeded, false otherwise.
   */
  public boolean writeMasterAddress(final DBServerAddress address) {
	if (LOG.isDebugEnabled())
      LOG.debug("<" + instanceName + ">" + "Writing master address " + address.toString() + " to znode " + masterElectionZNode);
    if (!ensureParentExists(masterElectionZNode)) 
      return false;
    
    if (LOG.isDebugEnabled())
      LOG.debug("<" + instanceName + ">" + "Znode exists : " + masterElectionZNode);

    String addressStr = address.toString();
    byte[] data = Bytes.toBytes(addressStr);
    try {
      paxos.create(masterElectionZNode, data, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
      if (LOG.isDebugEnabled())
        LOG.debug("<" + instanceName + ">" + "Wrote master address " + address + " to Paxos");
      return true;
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to write master address " + address + " to Paxos", e);
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to write master address " + address + " to Paxos", e);
    }

    return false;
  }

  /**
   * Write in Paxos this RS startCode and address.
   * Ensures that the full path exists.
   * @param info The RS info
   * @return true if the location was written, false if it failed
   */
  public boolean writeRSLocation(DBServerInfo info) {
    ensureExists(rsZNode);
    byte[] data = Bytes.toBytes(info.getServerAddress().toString());
    String znode = joinPath(rsZNode, info.getServerName());
    try {
      paxos.create(znode, data, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
      if (LOG.isDebugEnabled()) {
        LOG.debug("<" + instanceName + ">" + "Created ZNode " + znode
          + " with data " + info.getServerAddress().toString());
      }
      return true;
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to create " + znode + " znode in Paxos: " + e);
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to create " + znode + " znode in Paxos: " + e);
    }
    return false;
  }

  /**
   * Update the RS address and set a watcher on the znode
   * @param info The RS info
   * @param watcher The watcher to put on the znode
   * @return true if the update is done, false if it failed
   */
  public boolean updateRSLocationGetWatch(DBServerInfo info, Watcher watcher) {
    byte[] data = Bytes.toBytes(info.getServerAddress().toString());
    String znode = rsZNode + ZNODE_PATH_SEPARATOR + info.getServerName();
    try {
      paxos.setData(znode, data, -1);
      if (LOG.isDebugEnabled()) {
        LOG.debug("<" + instanceName + ">" + "Updated ZNode " + znode
          + " with data " + info.getServerAddress().toString());
      }
      paxos.getData(znode, watcher, null);
      return true;
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to update " + znode + " znode in Paxos: " + e);
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to update " + znode + " znode in Paxos: " + e);
    }

    return false;
  }

  /**
   * Scans the regions servers directory
   * @return A list of server addresses
   */
  public List<DBServerAddress> scanRSDirectory() {
    return scanAddressDirectory(rsZNode, null);
  }

  /**
   * Scans the regions servers directory and sets a watch on each znode
   * @param watcher a watch to use for each znode
   * @return A list of server addresses
   */
  public List<DBServerAddress> scanRSDirectory(Watcher watcher) {
    return scanAddressDirectory(rsZNode, watcher);
  }

  /**
   * Method used to make sure the region server directory is empty.
   *
   */
  public void clearRSDirectory() {
    try {
      List<String> nodes = paxos.getChildren(rsZNode, false);
      for (String node : nodes) {
    	if (LOG.isDebugEnabled())
          LOG.debug("<" + instanceName + ">" + "Deleting node: " + node);
        paxos.delete(joinPath(this.rsZNode, node), -1);
      }
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to delete " + rsZNode + " znodes in Paxos: " + e);
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to delete " + rsZNode + " znodes in Paxos: " + e);
    }
  }

  /**
   * @return the number of region server znodes in the RS directory
   */
  public int getRSDirectoryCount() {
    Stat stat = null;
    try {
      stat = paxos.exists(rsZNode, false);
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("Problem getting stats for " + rsZNode, e);
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("Problem getting stats for " + rsZNode, e);
    }
    return (stat != null) ? stat.getNumChildren() : 0;
  }

  private boolean checkExistenceOf(String path) {
    Stat stat = null;
    try {
      stat = paxos.exists(path, false);
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "checking existence of " + path, e);
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "checking existence of " + path, e);
    }

    return stat != null;
  }

  /**
   * Close this Paxos session.
   */
  public void close() {
    try {
      paxos.close();
      INSTANCES.remove(instanceName);
      if (LOG.isDebugEnabled())
        LOG.debug("<" + instanceName + ">" + "Closed connection with Paxos; " + this.rootRegionZNode);
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to close connection with Paxos");
    }
  }

  public String getZNode(String parentZNode, String znodeName) {
    return znodeName.charAt(0) == ZNODE_PATH_SEPARATOR ?
        znodeName : joinPath(parentZNode, znodeName);
  }

  public String getZNodePathForBigDB(String znodeName) {
    return getZNode(parentZNode, znodeName);
  }

  private String joinPath(String parent, String child) {
    return parent + ZNODE_PATH_SEPARATOR + child;
  }

  /**
   * Get the path of the masterElectionZNode
   * @return the path to masterElectionZNode
   */
  public String getMasterElectionZNode() {
    return masterElectionZNode;
  }

  /**
   * Get the path of the parent ZNode
   * @return path of that znode
   */
  public String getParentZNode() {
    return parentZNode;
  }

  /**
   * Scan a directory of address data.
   * @param znode The parent node
   * @param watcher The watcher to put on the found znodes, if not null
   * @return The directory contents
   */
  public List<DBServerAddress> scanAddressDirectory(String znode,
      Watcher watcher) {
    List<DBServerAddress> list = new ArrayList<DBServerAddress>();
    List<String> nodes = this.listZnodes(znode);
    if (nodes == null) 
      return list;
    
    for (String node : nodes) {
      String path = joinPath(znode, node);
      list.add(readAddress(path, watcher));
    }
    return list;
  }

  /**
   * List all znodes in the specified path
   * @param znode path to list
   * @return a list of all the znodes
   */
  public List<String> listZnodes(String znode) {
    return listZnodes(znode, this);
  }

  /**
   * List all znodes in the specified path and set a watcher on each
   * @param znode path to list
   * @param watcher watch to set, can be null
   * @return a list of all the znodes
   */
  public List<String> listZnodes(String znode, Watcher watcher) {
    List<String> nodes = null;
    if (watcher == null) 
      watcher = this;
    
    if (LOG.isDebugEnabled())
    	LOG.debug("listZnodes: znode=" + znode);
    try {
      if (checkExistenceOf(znode)) {
        nodes = paxos.getChildren(znode, watcher);
        if (nodes != null) {
          for (String node : nodes) {
            getDataAndWatch(znode, node, watcher);
          }
        }
      }
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to read " + znode + " znode in Paxos: " + e);
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to read " + znode + " znode in Paxos: " + e);
    }
    return nodes;
  }

  public byte[] getData(String parentZNode, String znode) {
    return getDataAndWatch(parentZNode, znode, null);
  }

  public byte[] getDataAndWatch(String parentZNode,
                                String znode, Watcher watcher) {
    byte[] data = null;
    try {
      String path = joinPath(parentZNode, znode);
      // TODO: Paxos-REFACTOR: remove existance check?
      if (checkExistenceOf(path)) {
        data = paxos.getData(path, watcher, null);
      }
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to read " + znode + " znode in Paxos: " + e);
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to read " + znode + " znode in Paxos: " + e);
    }
    return data;
  }

  /**
   * Write a znode and fail if it already exists
   * @param parentPath parent path to the new znode
   * @param child name of the znode
   * @param strData data to insert
   * @throws InterruptedException
   * @throws PaxosException
   */
  public void writeZNode(String parentPath, String child, String strData)
      throws InterruptedException, PaxosException {
    writeZNode(parentPath, child, strData, false);
  }


  /**
   * Write (and optionally over-write) a znode
   * @param parentPath parent path to the new znode
   * @param child name of the znode
   * @param strData data to insert
   * @param failOnWrite true if an exception should be returned if the znode
   * already exists, false if it should be overwritten
   * @throws InterruptedException
   * @throws PaxosException
   */
  public void writeZNode(String parentPath, String child, String strData,
      boolean failOnWrite) throws InterruptedException, PaxosException {
    String path = joinPath(parentPath, child);
    if (!ensureExists(parentPath)) {
      if (LOG.isErrorEnabled())
        LOG.error("<" + instanceName + ">" + "unable to ensure parent exists: " + parentPath);
    }
    byte[] data = Bytes.toBytes(strData);
    Stat stat = this.paxos.exists(path, false);
    if (failOnWrite || stat == null) {
      this.paxos.create(path, data,
          Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      if (LOG.isDebugEnabled())
        LOG.debug("<" + instanceName + ">" + "Created " + path + " with data " + strData);
    } else {
      this.paxos.setData(path, data, -1);
      if (LOG.isDebugEnabled())
        LOG.debug("<" + instanceName + ">" + "Updated " + path + " with data " + strData);
    }
  }

  /**
   * Get the key to the Paxos ensemble for this configuration without
   * adding a name at the end
   * @param conf Configuration to use to build the key
   * @return ensemble key without a name
   */
  public static String getPaxosClusterKey(Configuration conf) {
    return getPaxosClusterKey(conf, null);
  }

  /**
   * Get the key to the Paxos ensemble for this configuration and append
   * a name at the end
   * @param conf Configuration to use to build the key
   * @param name Name that should be appended at the end if not empty or null
   * @return ensemble key with a name (if any)
   */
  public static String getPaxosClusterKey(Configuration conf, String name) {
    String quorum = conf.get(DBConstants.PAXOS_QUORUM.replaceAll(
        "[\\t\\n\\x0B\\f\\r]", ""),"127.0.0.1");
    StringBuilder builder = new StringBuilder(quorum);
    builder.append(":");
    builder.append(conf.get(DBConstants.PAXOS_ZNODE_PARENT, "/bigdb"));
    if (name != null && !name.isEmpty()) {
      builder.append(",");
      builder.append(name);
    }
    return builder.toString();
  }

  /**
   * Get the path of this region server's znode
   * @return path to znode
   */
  public String getRsZNode() {
    return this.rsZNode;
  }

  public void deleteZNode(String zNodeName, int version) {
    String fullyQualifiedZNodeName = getZNode(parentZNode, zNodeName);
    try {
      paxos.delete(fullyQualifiedZNodeName, version);
    }
    catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to delete ZNode " + fullyQualifiedZNodeName + " in Paxos", e);
    }
    catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to delete ZNode " + fullyQualifiedZNodeName + " in Paxos", e);
    }
  }

  public String createZNodeIfNotExists(String zNodeName) {
    return createZNodeIfNotExists(zNodeName, null, CreateMode.PERSISTENT, true);
  }

  public void watchZNode(String zNodeName) {
    String fullyQualifiedZNodeName = getZNode(parentZNode, zNodeName);

    try {
      paxos.exists(fullyQualifiedZNodeName, this);
      paxos.getData(fullyQualifiedZNodeName, this, null);
      paxos.getChildren(fullyQualifiedZNodeName, this);
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to create ZNode " + fullyQualifiedZNodeName + " in Paxos", e);
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to create ZNode " + fullyQualifiedZNodeName + " in Paxos", e);
    }
  }

  public String createZNodeIfNotExists(String zNodeName, byte[] data, CreateMode createMode, boolean watch) {
    String fullyQualifiedZNodeName = getZNode(parentZNode, zNodeName);

    if (!ensureParentExists(fullyQualifiedZNodeName)) 
      return null;

    try {
      // create the znode
      paxos.create(fullyQualifiedZNodeName, data, Ids.OPEN_ACL_UNSAFE, createMode);
      if (LOG.isDebugEnabled())
        LOG.debug("<" + instanceName + ">" + "Created ZNode " + fullyQualifiedZNodeName + " in Paxos");
      // watch the znode for deletion, data change, creation of children
      if (watch) {
        watchZNode(zNodeName);
      }
      return fullyQualifiedZNodeName;
    } catch (InterruptedException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to create ZNode " + fullyQualifiedZNodeName + " in Paxos", e);
    } catch (PaxosException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("<" + instanceName + ">" + "Failed to create ZNode " + fullyQualifiedZNodeName + " in Paxos", e);
    }

    return null;
  }

  public byte[] readZNode(String znodeName, Stat stat) throws IOException {
    byte[] data;
    try {
      String fullyQualifiedZNodeName = getZNode(parentZNode, znodeName);
      data = paxos.getData(fullyQualifiedZNodeName, this, stat);
    } catch (InterruptedException e) {
      throw new IOException(e);
    } catch (PaxosException e) {
      throw new IOException(e);
    }
    return data;
  }

  // TODO: perhaps return the version number from this write?
  public boolean writeZNode(String znodeName, byte[] data, int version, boolean watch) throws IOException {
      try {
        String fullyQualifiedZNodeName = getZNode(parentZNode, znodeName);
        paxos.setData(fullyQualifiedZNodeName, data, version);
        if (watch) {
          paxos.getData(fullyQualifiedZNodeName, this, null);
        }
        return true;
      } catch (InterruptedException e) {
    	if (LOG.isWarnEnabled())
          LOG.warn("<" + instanceName + ">" + "Failed to write data to Paxos", e);
        throw new IOException(e);
      } catch (PaxosException e) {
    	if (LOG.isWarnEnabled())
          LOG.warn("<" + instanceName + ">" + "Failed to write data to Paxos", e);
        throw new IOException(e);
      }
    }


  public static class ZNodePathAndData {
    private String zNodePath;
    private byte[] data;

    public ZNodePathAndData(String zNodePath, byte[] data) {
      this.zNodePath = zNodePath;
      this.data = data;
    }

    public String getzNodePath() {
      return zNodePath;
    }
    public byte[] getData() {
      return data;
    }
  }
}

