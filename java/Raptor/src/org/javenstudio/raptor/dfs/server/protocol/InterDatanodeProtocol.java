package org.javenstudio.raptor.dfs.server.protocol;

import java.io.IOException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.dfs.protocol.Block;
import org.javenstudio.raptor.ipc.VersionedProtocol;

/** An inter-datanode protocol for updating generation stamp
 */
public interface InterDatanodeProtocol extends VersionedProtocol {
  public static final Logger LOG = Logger.getLogger(InterDatanodeProtocol.class);

  /**
   * 3: added a finalize parameter to updateBlock
   */
  public static final long versionID = 3L;

  /** @return the BlockMetaDataInfo of a block;
   *  null if the block is not found 
   */
  BlockMetaDataInfo getBlockMetaDataInfo(Block block) throws IOException;

  /**
   * Update the block to the new generation stamp and length.  
   */
  void updateBlock(Block oldblock, Block newblock, boolean finalize) throws IOException;
}

