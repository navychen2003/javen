package org.javenstudio.raptor.bigdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.bigdb.client.DBAdmin;
import org.javenstudio.raptor.bigdb.regionserver.DBRegionServer;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.bigdb.master.DBMaster;
import org.javenstudio.raptor.bigdb.util.JVMClusterUtil;

/**
 * This class creates a single process HBase cluster. One thread is created for
 * a master and one per region server.
 *
 * Call {@link #startup()} to start the cluster running and {@link #shutdown()}
 * to close it all down. {@link #join} the cluster is you want to wait on
 * shutdown completion.
 *
 * <p>Runs master on port 60000 by default.  Because we can't just kill the
 * process -- not till HADOOP-1700 gets fixed and even then.... -- we need to
 * be able to find the master with a remote client to run shutdown.  To use a
 * port other than 60000, set the bigdb.master to a value of 'local:PORT':
 * that is 'local', not 'localhost', and the port number the master should use
 * instead of 60000.
 *
 * <p>To make 'local' mode more responsive, make values such as
 * <code>bigdb.regionserver.msginterval</code>,
 * <code>bigdb.master.meta.thread.rescanfrequency</code>, and
 * <code>bigdb.server.thread.wakefrequency</code> a second or less.
 */
public class LocalDBCluster {
  static final Logger LOG = Logger.getLogger(LocalDBCluster.class);
  private final DBMaster master;
  private final List<JVMClusterUtil.RegionServerThread> regionThreads;
  private final static int DEFAULT_NO = 1;
  /** local mode */
  public static final String LOCAL = "local";
  /** 'local:' */
  public static final String LOCAL_COLON = LOCAL + ":";
  private final Configuration conf;
  private final Class<? extends DBRegionServer> regionServerClass;

  /**
   * Constructor.
   * @param conf
   * @throws IOException
   */
  public LocalDBCluster(final Configuration conf)
  throws IOException {
    this(conf, DEFAULT_NO);
  }

  /**
   * Constructor.
   * @param conf Configuration to use.  Post construction has the master's
   * address.
   * @param noRegionServers Count of regionservers to start.
   * @throws IOException
   */
  public LocalDBCluster(final Configuration conf, final int noRegionServers)
  throws IOException {
    this(conf, noRegionServers, DBMaster.class, getRegionServerImplementation(conf));
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends DBRegionServer> getRegionServerImplementation(final Configuration conf) {
    return (Class<? extends DBRegionServer>)conf.getClass(DBConstants.REGION_SERVER_IMPL,
       DBRegionServer.class);
  }

  /**
   * Constructor.
   * @param conf Configuration to use.  Post construction has the master's
   * address.
   * @param noRegionServers Count of regionservers to start.
   * @param masterClass
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public LocalDBCluster(final Configuration conf,
    final int noRegionServers, final Class<? extends DBMaster> masterClass,
    final Class<? extends DBRegionServer> regionServerClass)
  throws IOException {
    this.conf = conf;
    // Create the master
    this.master = DBMaster.constructMaster(masterClass, conf);
    // Start the DBRegionServers.  Always have region servers come up on
    // port '0' so there won't be clashes over default port as unit tests
    // start/stop ports at different times during the life of the test.
    conf.set(DBConstants.REGIONSERVER_PORT, "0");
    this.regionThreads =
      new CopyOnWriteArrayList<JVMClusterUtil.RegionServerThread>();
    this.regionServerClass =
      (Class<? extends DBRegionServer>)conf.getClass(DBConstants.REGION_SERVER_IMPL,
       regionServerClass);
    for (int i = 0; i < noRegionServers; i++) {
      addRegionServer(i);
    }
  }

  public JVMClusterUtil.RegionServerThread addRegionServer() throws IOException {
    return addRegionServer(this.regionThreads.size());
  }

  public JVMClusterUtil.RegionServerThread addRegionServer(final int index)
  throws IOException {
    JVMClusterUtil.RegionServerThread rst = JVMClusterUtil.createRegionServerThread(this.conf,
        this.regionServerClass, index);
    this.regionThreads.add(rst);
    return rst;
  }

  /**
   * @param serverNumber
   * @return region server
   */
  public DBRegionServer getRegionServer(int serverNumber) {
    return regionThreads.get(serverNumber).getRegionServer();
  }

  /**
   * @return the DBMaster thread
   */
  public DBMaster getMaster() {
    return this.master;
  }

  /**
   * @return Read-only list of region server threads.
   */
  public List<JVMClusterUtil.RegionServerThread> getRegionServers() {
    return Collections.unmodifiableList(this.regionThreads);
  }

  /**
   * @return List of running servers (Some servers may have been killed or
   * aborted during lifetime of cluster; these servers are not included in this
   * list).
   */
  public List<JVMClusterUtil.RegionServerThread> getLiveRegionServers() {
    List<JVMClusterUtil.RegionServerThread> liveServers =
      new ArrayList<JVMClusterUtil.RegionServerThread>();
    for (JVMClusterUtil.RegionServerThread rst: getRegionServers()) {
      if (rst.isAlive()) liveServers.add(rst);
    }
    return liveServers;
  }

  /**
   * Wait for the specified region server to stop
   * Removes this thread from list of running threads.
   * @param serverNumber
   * @return Name of region server that just went down.
   */
  public String waitOnRegionServer(int serverNumber) {
    JVMClusterUtil.RegionServerThread regionServerThread =
      this.regionThreads.remove(serverNumber);
    while (regionServerThread.isAlive()) {
      try {
        LOG.info("Waiting on " +
          regionServerThread.getRegionServer().getDBServerInfo().toString());
        regionServerThread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return regionServerThread.getName();
  }

  /**
   * Wait for Mini HBase Cluster to shut down.
   * Presumes you've already called {@link #shutdown()}.
   */
  public void join() {
    if (this.regionThreads != null) {
        for (Thread t: this.regionThreads) {
          if (t.isAlive()) {
            try {
              t.join();
          } catch (InterruptedException e) {
            // continue
          }
        }
      }
    }
    if (this.master != null && this.master.isAlive()) {
      try {
        this.master.join();
      } catch(InterruptedException e) {
        // continue
      }
    }
  }

  /**
   * Start the cluster.
   */
  public void startup() {
    JVMClusterUtil.startup(this.master, this.regionThreads);
  }

  /**
   * Shut down the mini HBase cluster
   */
  public void shutdown() {
    JVMClusterUtil.shutdown(this.master, this.regionThreads);
  }

  /**
   * @param c Configuration to check.
   * @return True if a 'local' address in bigdb.master value.
   */
  public static boolean isLocal(final Configuration c) {
    final String mode = c.get(DBConstants.CLUSTER_DISTRIBUTED);
    return mode == null || mode.equals(DBConstants.CLUSTER_IS_LOCAL);
  }

  /**
   * Test things basically work.
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    Configuration conf = ConfigurationFactory.get();
    LocalDBCluster cluster = new LocalDBCluster(conf);
    cluster.startup();
    DBAdmin admin = new DBAdmin(conf);
    DBTableDescriptor htd =
      new DBTableDescriptor(Bytes.toBytes(cluster.getClass().getName()));
    admin.createTable(htd);
    cluster.shutdown();
  }
}

