package org.javenstudio.raptor.dfs.server.common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.io.WritableFactories;
import org.javenstudio.raptor.io.WritableFactory;


/**
 * Base upgrade upgradeStatus class.
 * Overload this class if specific status fields need to be reported.
 * 
 * Describes status of current upgrade.
 */
public class UpgradeStatusReport implements Writable {
  protected int version;
  protected short upgradeStatus;
  protected boolean finalized;

  public UpgradeStatusReport() {
    this.version = 0;
    this.upgradeStatus = 0;
    this.finalized = false;
  }

  public UpgradeStatusReport(int version, short status, boolean isFinalized) {
    this.version = version;
    this.upgradeStatus = status;
    this.finalized = isFinalized;
  }

  /**
   * Get the layout version of the currently running upgrade.
   * @return layout version
   */
  public int getVersion() {
    return this.version;
  }

  /**
   * Get upgrade upgradeStatus as a percentage of the total upgrade done.
   * 
   * @see Upgradeable#getUpgradeStatus() 
   */ 
  public short getUpgradeStatus() {
    return upgradeStatus;
  }

  /**
   * Is current upgrade finalized.
   * @return true if finalized or false otherwise.
   */
  public boolean isFinalized() {
    return this.finalized;
  }

  /**
   * Get upgradeStatus data as a text for reporting.
   * Should be overloaded for a particular upgrade specific upgradeStatus data.
   * 
   * @param details true if upgradeStatus details need to be included, 
   *                false otherwise
   * @return text
   */
  public String getStatusText(boolean details) {
    return "Upgrade for version " + getVersion() 
            + (upgradeStatus<100 ? 
              " is in progress. Status = " + upgradeStatus + "%" : 
              " has been completed."
              + "\nUpgrade is " + (finalized ? "" : "not ")
              + "finalized.");
  }

  /**
   * Print basic upgradeStatus details.
   */
  public String toString() {
    return getStatusText(false);
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  static {                                      // register a ctor
    WritableFactories.setFactory
      (UpgradeStatusReport.class,
       new WritableFactory() {
         public Writable newInstance() { return new UpgradeStatusReport(); }
       });
  }

  /**
   */
  public void write(DataOutput out) throws IOException {
    out.writeInt(this.version);
    out.writeShort(this.upgradeStatus);
  }

  /**
   */
  public void readFields(DataInput in) throws IOException {
    this.version = in.readInt();
    this.upgradeStatus = in.readShort();
  }
}

