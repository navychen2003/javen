package org.javenstudio.raptor.dfs.server.protocol;

import java.io.*;

import org.javenstudio.raptor.dfs.protocol.Block;
import org.javenstudio.raptor.dfs.protocol.DatanodeID;
import org.javenstudio.raptor.dfs.protocol.LocatedBlock;
import org.javenstudio.raptor.ipc.VersionedProtocol;

/**********************************************************************
 * Protocol that a DFS datanode uses to communicate with the NameNode.
 * It's used to upload current load information and block reports.
 *
 * The only way a NameNode can communicate with a DataNode is by
 * returning values from these functions.
 *
 **********************************************************************/
public interface DatanodeProtocol extends VersionedProtocol {
  /**
   * 19: SendHeartbeat returns an array of DatanodeCommand objects
   *     in stead of a DatanodeCommand object.
   */
  public static final long versionID = 19L;
  
  // error code
  final static int NOTIFY = 0;
  final static int DISK_ERROR = 1;
  final static int INVALID_BLOCK = 2;

  /**
   * Determines actions that data node should perform 
   * when receiving a datanode command. 
   */
  final static int DNA_UNKNOWN = 0;    // unknown action   
  final static int DNA_TRANSFER = 1;   // transfer blocks to another datanode
  final static int DNA_INVALIDATE = 2; // invalidate blocks
  final static int DNA_SHUTDOWN = 3;   // shutdown node
  final static int DNA_REGISTER = 4;   // re-register
  final static int DNA_FINALIZE = 5;   // finalize previous upgrade
  final static int DNA_RECOVERBLOCK = 6;  // request a block recovery

  /** 
   * Register Datanode.
   *
   * @see org.javenstudio.raptor.dfs.server.datanode.DataNode#dnRegistration
   * @see org.javenstudio.raptor.dfs.server.namenode.FSNamesystem#registerDatanode(DatanodeRegistration)
   * 
   * @return updated {@link org.javenstudio.raptor.dfs.server.protocol.DatanodeRegistration}, which contains 
   * new storageID if the datanode did not have one and
   * registration ID for further communication.
   */
  public DatanodeRegistration register(DatanodeRegistration registration
                                       ) throws IOException;
  /**
   * sendHeartbeat() tells the NameNode that the DataNode is still
   * alive and well.  Includes some status info, too. 
   * It also gives the NameNode a chance to return 
   * an array of "DatanodeCommand" objects.
   * A DatanodeCommand tells the DataNode to invalidate local block(s), 
   * or to copy them to other DataNodes, etc.
   */
  public DatanodeCommand[] sendHeartbeat(DatanodeRegistration registration,
                                       long capacity,
                                       long dfsUsed, long remaining,
                                       int xmitsInProgress,
                                       int xceiverCount) throws IOException;

  /**
   * blockReport() tells the NameNode about all the locally-stored blocks.
   * The NameNode returns an array of Blocks that have become obsolete
   * and should be deleted.  This function is meant to upload *all*
   * the locally-stored blocks.  It's invoked upon startup and then
   * infrequently afterwards.
   * @param registration
   * @param blocks - the block list as an array of longs.
   *     Each block is represented as 2 longs.
   *     This is done instead of Block[] to reduce memory used by block reports.
   *     
   * @return - the next command for DN to process.
   * @throws IOException
   */
  public DatanodeCommand blockReport(DatanodeRegistration registration,
                                     long[] blocks) throws IOException;
    
  /**
   * blockReceived() allows the DataNode to tell the NameNode about
   * recently-received block data, with a hint for pereferred replica
   * to be deleted when there is any excessive blocks.
   * For example, whenever client code
   * writes a new Block here, or another DataNode copies a Block to
   * this DataNode, it will call blockReceived().
   */
  public void blockReceived(DatanodeRegistration registration,
                            Block blocks[],
                            String[] delHints) throws IOException;

  /**
   * errorReport() tells the NameNode about something that has gone
   * awry.  Useful for debugging.
   */
  public void errorReport(DatanodeRegistration registration,
                          int errorCode, 
                          String msg) throws IOException;
    
  public NamespaceInfo versionRequest() throws IOException;

  /**
   * This is a very general way to send a command to the name-node during
   * distributed upgrade process.
   * 
   * The generosity is because the variety of upgrade commands is unpredictable.
   * The reply from the name-node is also received in the form of an upgrade 
   * command. 
   * 
   * @return a reply in the form of an upgrade command
   */
  UpgradeCommand processUpgradeCommand(UpgradeCommand comm) throws IOException;
  
  /**
   * same as {@link org.javenstudio.raptor.dfs.protocol.ClientProtocol#reportBadBlocks(LocatedBlock[])}
   * }
   */
  public void reportBadBlocks(LocatedBlock[] blocks) throws IOException;
  
  /**
   * @return the next GenerationStamp to be associated with the specified
   * block. 
   */
  public long nextGenerationStamp(Block block) throws IOException;

  /**
   * Commit block synchronization in lease recovery
   */
  public void commitBlockSynchronization(Block block,
      long newgenerationstamp, long newlength,
      boolean closeFile, boolean deleteblock, DatanodeID[] newtargets
      ) throws IOException;
}

