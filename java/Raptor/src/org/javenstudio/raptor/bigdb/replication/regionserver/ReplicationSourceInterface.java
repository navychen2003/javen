package org.javenstudio.raptor.bigdb.replication.regionserver;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;

/**
 * Interface that defines a replication source
 */
public interface ReplicationSourceInterface {

  /**
   * Initializer for the source
   * @param conf the configuration to use
   * @param fs the file system to use
   * @param manager the manager to use
   * @param stopper the stopper object for this region server
   * @param replicating the status of the replication on this cluster
   * @param peerClusterId the id of the peer cluster
   * @throws IOException
   */
  public void init(final Configuration conf,
                   final FileSystem fs,
                   final ReplicationSourceManager manager,
                   final AtomicBoolean stopper,
                   final AtomicBoolean replicating,
                   final String peerClusterId) throws IOException;

  /**
   * Add a log to the list of logs to replicate
   * @param log path to the log to replicate
   */
  public void enqueueLog(Path log);

  /**
   * Get the current log that's replicated
   * @return the current log
   */
  public Path getCurrentPath();

  /**
   * Start the replication
   */
  public void startup();

  /**
   * End the replication
   */
  public void terminate();

  /**
   * Get the id that the source is replicating to
   *
   * @return peer cluster id
   */
  public String getPeerClusterZnode();
}

