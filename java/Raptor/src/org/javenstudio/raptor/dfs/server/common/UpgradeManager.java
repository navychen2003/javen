package org.javenstudio.raptor.dfs.server.common;

import java.io.IOException;
import java.util.SortedSet;

import org.javenstudio.raptor.dfs.protocol.FSConstants;
import org.javenstudio.raptor.dfs.server.protocol.UpgradeCommand;

/**
 * Generic upgrade manager.
 * 
 * {@link #broadcastCommand} is the command that should be 
 *
 */
public abstract class UpgradeManager {
  protected SortedSet<Upgradeable> currentUpgrades = null;
  protected boolean upgradeState = false; // true if upgrade is in progress
  protected int upgradeVersion = 0;
  protected UpgradeCommand broadcastCommand = null;

  public synchronized UpgradeCommand getBroadcastCommand() {
    return this.broadcastCommand;
  }

  public boolean getUpgradeState() {
    return this.upgradeState;
  }

  public int getUpgradeVersion(){
    return this.upgradeVersion;
  }

  public void setUpgradeState(boolean uState, int uVersion) {
    this.upgradeState = uState;
    this.upgradeVersion = uVersion;
  }

  public SortedSet<Upgradeable> getDistributedUpgrades() throws IOException {
    return UpgradeObjectCollection.getDistributedUpgrades(
                                            getUpgradeVersion(), getType());
  }

  public short getUpgradeStatus() {
    if(currentUpgrades == null)
      return 100;
    return currentUpgrades.first().getUpgradeStatus();
  }

  public boolean initializeUpgrade() throws IOException {
    currentUpgrades = getDistributedUpgrades();
    if(currentUpgrades == null) {
      // set new upgrade state
      setUpgradeState(false, FSConstants.LAYOUT_VERSION);
      return false;
    }
    Upgradeable curUO = currentUpgrades.first();
    // set and write new upgrade state into disk
    setUpgradeState(true, curUO.getVersion());
    return true;
  }

  public boolean isUpgradeCompleted() {
    if (currentUpgrades == null) {
      return true;
    }
    return false;
  }

  public abstract DfsConstants.NodeType getType();
  public abstract boolean startUpgrade() throws IOException;
  public abstract void completeUpgrade() throws IOException;
}

