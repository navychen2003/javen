package org.javenstudio.raptor.bigdb.replication.regionserver;

import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.DBServerInfo;
import org.javenstudio.raptor.bigdb.KeyValue;
import org.javenstudio.raptor.bigdb.regionserver.wal.DBLog;
import org.javenstudio.raptor.bigdb.regionserver.wal.DBLogKey;
import org.javenstudio.raptor.bigdb.regionserver.wal.LogEntryVisitor;
import org.javenstudio.raptor.bigdb.regionserver.wal.WALEdit;
import org.javenstudio.raptor.bigdb.replication.ReplicationPaxosWrapper;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.bigdb.paxos.PaxosWrapper;

import java.io.IOException;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Replication serves as an umbrella over the setup of replication and
 * is used by HRS.
 */
public class Replication implements LogEntryVisitor {

  private final boolean replication;
  private final ReplicationSourceManager replicationManager;
  private boolean replicationMaster;
  private final AtomicBoolean replicating = new AtomicBoolean(true);
  private final ReplicationPaxosWrapper zkHelper;
  private final Configuration conf;
  private final AtomicBoolean  stopRequested;
  private ReplicationSink replicationSink;

  /**
   * Instantiate the replication management (if rep is enabled).
   * @param conf conf to use
   * @param hsi the info if this region server
   * @param fs handle to the filesystem
   * @param oldLogDir directory where logs are archived
   * @param stopRequested boolean that tells us if we are shutting down
   * @throws IOException
   */
  public Replication(Configuration conf, DBServerInfo hsi,
                     FileSystem fs, Path logDir, Path oldLogDir,
                     AtomicBoolean stopRequested) throws IOException {
    this.conf = conf;
    this.stopRequested = stopRequested;
    this.replication =
        conf.getBoolean(DBConstants.REPLICATION_ENABLE_KEY, false);
    if (replication) {
      this.zkHelper = new ReplicationPaxosWrapper(
        PaxosWrapper.createInstance(conf, hsi.getServerName()), conf,
        this.replicating, hsi.getServerName());
      this.replicationMaster = zkHelper.isReplicationMaster();
      this.replicationManager = this.replicationMaster ?
        new ReplicationSourceManager(zkHelper, conf, stopRequested,
          fs, this.replicating, logDir, oldLogDir) : null;
    } else {
      replicationManager = null;
      zkHelper = null;
    }
  }

  /**
   * Join with the replication threads
   */
  public void join() {
    if (this.replication) {
      if (this.replicationMaster) {
        this.replicationManager.join();
      }
      this.zkHelper.deleteOwnRSZNode();
    }
  }

  /**
   * Carry on the list of log entries down to the sink
   * @param entries list of entries to replicate
   * @throws IOException
   */
  public void replicateLogEntries(DBLog.Entry[] entries) throws IOException {
    if (this.replication && !this.replicationMaster) {
      this.replicationSink.replicateEntries(entries);
    }
  }

  /**
   * If replication is enabled and this cluster is a master,
   * it starts
   * @throws IOException
   */
  public void startReplicationServices() throws IOException {
    if (this.replication) {
      if (this.replicationMaster) {
        this.replicationManager.init();
      } else {
        this.replicationSink =
            new ReplicationSink(this.conf, this.stopRequested);
      }
    }
  }

  /**
   * Get the replication sources manager
   * @return the manager if replication is enabled, else returns false
   */
  public ReplicationSourceManager getReplicationManager() {
    return replicationManager;
  }

  @Override
  public void visitLogEntryBeforeWrite(DBRegionInfo info, DBLogKey logKey,
                                       WALEdit logEdit) {
    NavigableMap<byte[], Integer> scopes =
        new TreeMap<byte[], Integer>(Bytes.BYTES_COMPARATOR);
    byte[] family;
    for (KeyValue kv : logEdit.getKeyValues()) {
      family = kv.getFamily();
      int scope = info.getTableDesc().getFamily(family).getScope();
      if (scope != DBConstants.REPLICATION_SCOPE_LOCAL &&
          !scopes.containsKey(family)) {
        scopes.put(family, scope);
      }
    }
    if (!scopes.isEmpty()) {
      logEdit.setScopes(scopes);
    }
  }

  /**
   * Add this class as a log entry visitor for DBLog if replication is enabled
   * @param hlog log that was add ourselves on
   */
  public void addLogEntryVisitor(DBLog hlog) {
    if (replication) {
      hlog.addLogEntryVisitor(this);
    }
  }
}

