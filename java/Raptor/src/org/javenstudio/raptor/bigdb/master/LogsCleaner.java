package org.javenstudio.raptor.bigdb.master;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.bigdb.Chore;
import org.javenstudio.raptor.bigdb.RemoteExceptionHandler;
import org.javenstudio.raptor.bigdb.regionserver.wal.DBLog;

/**
 * This Chore, everytime it runs, will clear the logs in the old logs folder
 * that are deletable for each log cleaner in the chain, in order to limit the
 * number of deletes it sends, will only delete maximum 20 in a single run.
 */
public class LogsCleaner extends Chore {

  static final Logger LOG = Logger.getLogger(LogsCleaner.class);

  // Max number we can delete on every chore, this is to make sure we don't
  // issue thousands of delete commands around the same time
  private final int maxDeletedLogs;
  private final FileSystem fs;
  private final Path oldLogDir;
  private List<LogCleanerDelegate> logCleanersChain;
  private final Configuration conf;

  /**
   *
   * @param p the period of time to sleep between each run
   * @param s the stopper boolean
   * @param conf configuration to use
   * @param fs handle to the FS
   * @param oldLogDir the path to the archived logs
   */
  public LogsCleaner(final int p, final AtomicBoolean s,
                        Configuration conf, FileSystem fs,
                        Path oldLogDir) {
    super("LogsCleaner", p, s);

    this.maxDeletedLogs =
        conf.getInt("bigdb.master.logcleaner.maxdeletedlogs", 20);
    this.fs = fs;
    this.oldLogDir = oldLogDir;
    this.conf = conf;
    this.logCleanersChain = new LinkedList<LogCleanerDelegate>();

    initLogCleanersChain();
  }

  /*
   * Initialize the chain of log cleaners from the configuration. The default
   * three LogCleanerDelegates in this chain are: TimeToLiveLogCleaner,
   * ReplicationLogCleaner and SnapshotLogCleaner.
   */
  private void initLogCleanersChain() {
    String[] logCleaners = conf.getStrings("bigdb.master.logcleaner.plugins");
    if (logCleaners != null) {
      for (String className : logCleaners) {
        LogCleanerDelegate logCleaner = newLogCleaner(className, conf);
        addLogCleaner(logCleaner);
      }
    }
  }

  /**
   * A utility method to create new instances of LogCleanerDelegate based
   * on the class name of the LogCleanerDelegate.
   * @param className fully qualified class name of the LogCleanerDelegate
   * @param conf
   * @return the new instance
   */
  @SuppressWarnings("rawtypes")
  public static LogCleanerDelegate newLogCleaner(String className, Configuration conf) {
    try {
      Class c = Class.forName(className);
      LogCleanerDelegate cleaner = (LogCleanerDelegate) c.newInstance();
      cleaner.setConf(conf);
      return cleaner;
    } catch(Exception e) {
      LOG.warn("Can NOT create LogCleanerDelegate: " + className, e);
      // skipping if can't instantiate
      return null;
    }
  }

  /**
   * Add a LogCleanerDelegate to the log cleaner chain. A log file is deletable
   * if it is deletable for each LogCleanerDelegate in the chain.
   * @param logCleaner
   */
  public void addLogCleaner(LogCleanerDelegate logCleaner) {
    if (logCleaner != null && !logCleanersChain.contains(logCleaner)) {
      logCleanersChain.add(logCleaner);
      LOG.debug("Add log cleaner in chain: " + logCleaner.getClass().getName());
    }
  }

  @Override
  protected void chore() {
    try {
      FileStatus[] files = this.fs.listStatus(this.oldLogDir);
      int nbDeletedLog = 0;
      FILE: for (FileStatus file : files) {
        Path filePath = file.getPath();
        if (DBLog.validateDBLogFilename(filePath.getName())) {
          for (LogCleanerDelegate logCleaner : logCleanersChain) {
            if (!logCleaner.isLogDeletable(filePath) ) {
              // this log is not deletable, continue to process next log file
              continue FILE;
            }
          }
          // delete this log file if it passes all the log cleaners
          this.fs.delete(filePath, true);
          nbDeletedLog++;
        } else {
          LOG.warn("Found a wrongly formated file: "
              + file.getPath().getName());
          this.fs.delete(filePath, true);
          nbDeletedLog++;
        }
        if (nbDeletedLog >= maxDeletedLogs) {
          break;
        }
      }
    } catch (IOException e) {
      e = RemoteExceptionHandler.checkIOException(e);
      LOG.warn("Error while cleaning the logs", e);
    }
  }
}

