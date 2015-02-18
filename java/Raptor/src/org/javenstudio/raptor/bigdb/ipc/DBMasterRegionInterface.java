package org.javenstudio.raptor.bigdb.ipc;

import org.javenstudio.raptor.bigdb.DBMsg;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.DBServerInfo;
import org.javenstudio.raptor.io.MapWritable;

import java.io.IOException;

/**
 * DBRegionServers interact with the DBMasterRegionInterface to report on local
 * goings-on and to obtain data-handling instructions from the DBMaster.
 * <p>Changes here need to be reflected in HbaseObjectWritable HbaseRPC#Invoker.
 *
 * <p>NOTE: if you change the interface, you must change the RPC version
 * number in DBRPCProtocolVersion
 *
 */
public interface DBMasterRegionInterface extends DBRPCProtocolVersion {

  /**
   * Called when a region server first starts
   * @param info server info
   * @throws IOException e
   * @return Configuration for the regionserver to use: e.g. filesystem,
   * bigdb rootdir, etc.
   */
  public MapWritable regionServerStartup(DBServerInfo info) throws IOException;

  /**
   * Called to renew lease, tell master what the region server is doing and to
   * receive new instructions from the master
   *
   * @param info server's address and start code
   * @param msgs things the region server wants to tell the master
   * @param mostLoadedRegions Array of DBRegionInfos that should contain the
   * reporting server's most loaded regions. These are candidates for being
   * rebalanced.
   * @return instructions from the master to the region server
   * @throws IOException e
   */
  public DBMsg[] regionServerReport(DBServerInfo info, DBMsg msgs[],
    DBRegionInfo mostLoadedRegions[])
  throws IOException;
}
