package org.javenstudio.raptor.dfs.server.common;

import java.io.IOException;

import org.javenstudio.raptor.dfs.server.protocol.UpgradeCommand;

/**
 * Common interface for distributed upgrade objects.
 * 
 * Each upgrade object corresponds to a layout version,
 * which is the latest version that should be upgraded using this object.
 * That is all components whose layout version is greater or equal to the
 * one returned by {@link #getVersion()} must be upgraded with this object.
 */
public interface Upgradeable extends Comparable<Upgradeable> {
  /**
   * Get the layout version of the upgrade object.
   * @return layout version
   */
  int getVersion();

  /**
   * Get the type of the software component, which this object is upgrading.
   * @return type
   */
  DfsConstants.NodeType getType();

  /**
   * Description of the upgrade object for displaying.
   * @return description
   */
  String getDescription();

  /**
   * Upgrade status determines a percentage of the work done out of the total 
   * amount required by the upgrade.
   * 
   * 100% means that the upgrade is completed.
   * Any value < 100 means it is not complete.
   * 
   * The return value should provide at least 2 values, e.g. 0 and 100.
   * @return integer value in the range [0, 100].
   */
  short getUpgradeStatus();

  /**
   * Prepare for the upgrade.
   * E.g. initialize upgrade data structures and set status to 0.
   * 
   * Returns an upgrade command that is used for broadcasting to other cluster
   * components. 
   * E.g. name-node informs data-nodes that they must perform a distributed upgrade.
   * 
   * @return an UpgradeCommand for broadcasting.
   * @throws IOException
   */
  UpgradeCommand startUpgrade() throws IOException;

  /**
   * Complete upgrade.
   * E.g. cleanup upgrade data structures or write metadata to disk.
   * 
   * Returns an upgrade command that is used for broadcasting to other cluster
   * components. 
   * E.g. data-nodes inform the name-node that they completed the upgrade
   * while other data-nodes are still upgrading.
   * 
   * @throws IOException
   */
  UpgradeCommand completeUpgrade() throws IOException;

  /**
   * Get status report for the upgrade.
   * 
   * @param details true if upgradeStatus details need to be included, 
   *                false otherwise
   * @return {@link UpgradeStatusReport}
   * @throws IOException
   */
  UpgradeStatusReport getUpgradeStatusReport(boolean details) throws IOException;
}

