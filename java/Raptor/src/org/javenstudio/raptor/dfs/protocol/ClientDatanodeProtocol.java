package org.javenstudio.raptor.dfs.protocol;

import java.io.IOException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.ipc.VersionedProtocol;


/** An client-datanode protocol for block recovery
 */
public interface ClientDatanodeProtocol extends VersionedProtocol {
  public static final Logger LOG = Logger.getLogger(ClientDatanodeProtocol.class);

  /**
   * 3: add keepLength parameter.
   */
  public static final long versionID = 3L;

  /** Start generation-stamp recovery for specified block
   * @param block the specified block
   * @param keepLength keep the block length
   * @param targets the list of possible locations of specified block
   * @return the new blockid if recovery successful and the generation stamp
   * got updated as part of the recovery, else returns null if the block id
   * not have any data and the block was deleted.
   * @throws IOException
   */
  LocatedBlock recoverBlock(Block block, boolean keepLength,
      DatanodeInfo[] targets) throws IOException;
}

