package org.javenstudio.raptor.bigdb.master;

import org.javenstudio.raptor.conf.Configurable;
import org.javenstudio.raptor.fs.Path;

/**
 * Interface for the log cleaning function inside the master. By default, three
 * cleaners <code>TimeToLiveLogCleaner</code>,  <code>ReplicationLogCleaner</code>,
 * <code>SnapshotLogCleaner</code> are called in order. So if other effects are
 * needed, implement your own LogCleanerDelegate and add it to the configuration
 * "bigdb.master.logcleaner.plugins", which is a comma-separated list of fully
 * qualified class names. LogsCleaner will add it to the chain.
 *
 * HBase ships with LogsCleaner as the default implementation.
 *
 * This interface extends Configurable, so setConf needs to be called once
 * before using the cleaner.
 * Since LogCleanerDelegates are created in LogsCleaner by reflection. Classes
 * that implements this interface should provide a default constructor.
 */
public interface LogCleanerDelegate extends Configurable {

  /**
   * Should the master delete the log or keep it?
   * @param filePath full path to log.
   * @return true if the log is deletable, false if not
   */
  public boolean isLogDeletable(Path filePath);
}

