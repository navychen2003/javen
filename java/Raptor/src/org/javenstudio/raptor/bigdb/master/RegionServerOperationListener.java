package org.javenstudio.raptor.bigdb.master;

import java.io.IOException;

import org.javenstudio.raptor.bigdb.DBMsg;
import org.javenstudio.raptor.bigdb.DBServerInfo;

/**
 * Listener for regionserver events in master.
 * @see HMaster#registerRegionServerOperationListener(RegionServerOperationListener)
 * @see HMaster#unregisterRegionServerOperationListener(RegionServerOperationListener)
 */
public interface RegionServerOperationListener {
  /**
   * Called for each message passed the master.  Most of the messages that come
   * in here will go on to become {@link #process(RegionServerOperation)}s but
   * others like {@linke DBMsg.Type#MSG_REPORT_PROCESS_OPEN} go no further;
   * only in here can you see them come in.
   * @param serverInfo Server we got the message from.
   * @param incomingMsg The message received.
   * @return True to continue processing, false to skip.
   */
  public boolean process(final DBServerInfo serverInfo,
      final DBMsg incomingMsg);

  /**
   * Called before processing <code>op</code>
   * @param op
   * @return True if we are to proceed w/ processing.
   * @exception IOException
   */
  public boolean process(final RegionServerOperation op) throws IOException;

  /**
   * Called after <code>op</code> has been processed.
   * @param op The operation that just completed.
   */
  public void processed(final RegionServerOperation op);
}

