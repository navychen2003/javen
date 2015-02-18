package org.javenstudio.raptor.dfs.server.namenode;

import java.io.IOException;

import org.javenstudio.raptor.dfs.server.common.DfsConstants;
import org.javenstudio.raptor.dfs.server.common.UpgradeObject;
import org.javenstudio.raptor.dfs.server.protocol.UpgradeCommand;

/**
 * Base class for name-node upgrade objects.
 * Data-node upgrades are run in separate threads.
 */
public abstract class UpgradeObjectNamenode extends UpgradeObject {

  /**
   * Process an upgrade command.
   * RPC has only one very generic command for all upgrade related inter 
   * component communications. 
   * The actual command recognition and execution should be handled here.
   * The reply is sent back also as an UpgradeCommand.
   * 
   * @param command
   * @return the reply command which is analyzed on the client side.
   */
  public abstract UpgradeCommand processUpgradeCommand(UpgradeCommand command
                                               ) throws IOException;

  public DfsConstants.NodeType getType() {
    return DfsConstants.NodeType.NAME_NODE;
  }

  /**
   */
  public UpgradeCommand startUpgrade() throws IOException {
    // broadcast that data-nodes must start the upgrade
    return new UpgradeCommand(UpgradeCommand.UC_ACTION_START_UPGRADE,
                              getVersion(), (short)0);
  }

  protected FSNamesystem getFSNamesystem() {
    return FSNamesystem.getFSNamesystem();
  }

  public void forceProceed() throws IOException {
    // do nothing by default
    NameNode.LOG.info("forceProceed() is not defined for the upgrade. " 
        + getDescription());
  }
}

