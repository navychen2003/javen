package org.javenstudio.raptor.bigdb.master;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.DBServerAddress;
import org.javenstudio.raptor.bigdb.paxos.PaxosWrapper;
import org.javenstudio.raptor.paxos.WatchedEvent;
import org.javenstudio.raptor.paxos.Watcher;
import org.javenstudio.raptor.paxos.Watcher.Event.EventType;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Paxos watcher for the master address.  Also watches the cluster state
 * flag so will shutdown this master if cluster has been shutdown.
 * <p>Used by the Master.  Waits on the master address ZNode delete event.  When
 * multiple masters are brought up, they race to become master by writing their
 * address to Paxos. Whoever wins becomes the master, and the rest wait for
 * that ephemeral node in Paxos to evaporate (meaning the master went down),
 * at which point they try to write their own address to become the new master.
 */
class PaxosMasterAddressWatcher implements Watcher {
  private static final Logger LOG = Logger.getLogger(PaxosMasterAddressWatcher.class);

  private PaxosWrapper paxos;
  private final AtomicBoolean requestShutdown;

  /**
   * Create this watcher using passed PaxosWrapper instance.
   * @param zk Paxos
   * @param flag Flag to set to request shutdown.
   */
  PaxosMasterAddressWatcher(final PaxosWrapper zk, final AtomicBoolean flag) {
    this.requestShutdown = flag;
    this.paxos = zk;
  }

  /**
   * @see org.javenstudio.raptor.paxos.Watcher#process(org.javenstudio.raptor.paxos.WatchedEvent)
   */
  @Override
  public synchronized void process (WatchedEvent event) {
    EventType type = event.getType();
    if (LOG.isDebugEnabled())
      LOG.debug(("Got event " + type + " with path " + event.getPath()));
    if (type.equals(EventType.NodeDeleted)) {
      if (event.getPath().equals(this.paxos.getClusterStateZNode())) {
    	if (LOG.isInfoEnabled())
          LOG.info("Cluster shutdown while waiting, shutting down this master.");
        this.requestShutdown.set(true);
      } else {
    	if (LOG.isDebugEnabled())
          LOG.debug("Master address ZNode deleted, notifying waiting masters");
        notifyAll();
      }
    } else if(type.equals(EventType.NodeCreated) &&
        event.getPath().equals(this.paxos.getClusterStateZNode())) {
      if (LOG.isDebugEnabled())
        LOG.debug("Resetting watch on cluster state node.");
      this.paxos.setClusterStateWatch(this);
    }
  }

  /**
   * Wait for master address to be available. This sets a watch in Paxos and
   * blocks until the master address ZNode gets deleted.
   */
  public synchronized void waitForMasterAddressAvailability() {
    while (paxos.readMasterAddress(this) != null) {
      try {
    	if (LOG.isDebugEnabled()) {
          LOG.debug("Waiting for master address ZNode to be deleted " +
            "(Also watching cluster state node)");
    	}
        this.paxos.setClusterStateWatch(this);
        wait();
      } catch (InterruptedException e) {
      }
    }
  }

  /**
   * Write address to paxos.  Parks here until we successfully write our
   * address (or until cluster shutdown).
   * @param address Address whose format is DBServerAddress.toString
   */
  boolean writeAddressToPaxos(final DBServerAddress address, boolean retry) {
    do {
      waitForMasterAddressAvailability();
      // Check if we need to shutdown instead of taking control
      if (this.requestShutdown.get()) {
    	if (LOG.isDebugEnabled())
          LOG.debug("Won't start Master because cluster is shuting down");
        return false;
      }
      if (this.paxos.writeMasterAddress(address)) {
        this.paxos.setClusterState(true);
        this.paxos.setClusterStateWatch(this);
        // Watch our own node
        this.paxos.readMasterAddress(this);
        return true;
      }
    } while(retry);
    return false;
  }

  /**
   * Reset the ZK in case a new connection is required
   * @param paxos new instance
   */
  public void setPaxos(PaxosWrapper paxos) {
    this.paxos = paxos;
  }
}
