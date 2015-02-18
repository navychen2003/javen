package org.javenstudio.raptor.bigdb.util;

import java.io.IOException;
import java.util.List;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.bigdb.master.DBMaster;
import org.javenstudio.raptor.bigdb.regionserver.DBRegionServer;

/**
 * Utility used running a cluster all in the one JVM.
 */
public class JVMClusterUtil {
  private static final Logger LOG = Logger.getLogger(JVMClusterUtil.class);

  /**
   * Datastructure to hold RegionServer Thread and RegionServer instance
   */
  public static class RegionServerThread extends Thread {
    private final DBRegionServer regionServer;

    public RegionServerThread(final DBRegionServer r, final int index) {
      super(r, "RegionServer:" + index);
      this.regionServer = r;
    }

    /** @return the region server */
    public DBRegionServer getRegionServer() {
      return this.regionServer;
    }

    /**
     * Block until the region server has come online, indicating it is ready
     * to be used.
     */
    public void waitForServerOnline() {
      // The server is marked online after the init method completes inside of
      // the HRS#run method.  HRS#init can fail for whatever region.  In those
      // cases, we'll jump out of the run without setting online flag.  Check
      // stopRequested so we don't wait here a flag that will never be flipped.
      while (!this.regionServer.isOnline() &&
          !this.regionServer.isStopRequested()) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          // continue waiting
        }
      }
    }
  }

  /**
   * Creates a {@link RegionServerThread}.
   * Call 'start' on the returned thread to make it run.
   * @param c Configuration to use.
   * @param hrsc Class to create.
   * @param index Used distingushing the object returned.
   * @throws IOException
   * @return Region server added.
   */
  public static JVMClusterUtil.RegionServerThread createRegionServerThread(final Configuration c,
    final Class<? extends DBRegionServer> hrsc, final int index)
  throws IOException {
      DBRegionServer server;
      try {
        server = hrsc.getConstructor(Configuration.class).newInstance(c);
      } catch (Exception e) {
        IOException ioe = new IOException();
        ioe.initCause(e);
        throw ioe;
      }
      return new JVMClusterUtil.RegionServerThread(server, index);
  }

  /**
   * Start the cluster.
   * @param m
   * @param regionServers
   * @return Address to use contacting master.
   */
  public static String startup(final DBMaster m,
      final List<JVMClusterUtil.RegionServerThread> regionservers) {
    if (m != null) m.start();
    if (regionservers != null) {
      for (JVMClusterUtil.RegionServerThread t: regionservers) {
        t.start();
      }
    }
    return m == null? null: m.getMasterAddress().toString();
  }

  /**
   * @param master
   * @param regionservers
   */
  public static void shutdown(final DBMaster master,
      final List<RegionServerThread> regionservers) {
    LOG.debug("Shutting down BigDB Cluster");
    if (master != null) {
      master.shutdown();
    }
    // regionServerThreads can never be null because they are initialized when
    // the class is constructed.
    if (regionservers != null) {
      for (Thread t: regionservers) {
        if (t.isAlive()) {
          try {
            t.join();
          } catch (InterruptedException e) {
            // continue
          }
        }
      }
    }
    if (master != null) {
      while (master.isAlive()) {
        try {
          // The below has been replaced to debug sometime hangs on end of
          // tests.
          master.join();
          //Threads.threadDumpingIsAlive(master);
        } catch (InterruptedException e) {
          // continue
        }
      }
    }
    if (LOG.isInfoEnabled()) {
      LOG.info("Shutdown " +
        ((regionservers != null)? master.getName(): "0 masters") +
        " " + regionservers.size() + " region server(s)");
    }
  }
}
