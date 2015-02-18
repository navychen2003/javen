package org.javenstudio.raptor.dfs.server.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.io.WritableFactories;
import org.javenstudio.raptor.io.WritableFactory;

/**
 * This as a generic distributed upgrade command.
 * 
 * During the upgrade cluster components send upgrade commands to each other
 * in order to obtain or share information with them.
 * It is supposed that each upgrade defines specific upgrade command by
 * deriving them from this class.
 * The upgrade command contains version of the upgrade, which is verified 
 * on the receiving side and current status of the upgrade.
 */
public class UpgradeCommand extends DatanodeCommand {
  final static int UC_ACTION_UNKNOWN = DatanodeProtocol.DNA_UNKNOWN;
  public final static int UC_ACTION_REPORT_STATUS = 100; // report upgrade status
  public final static int UC_ACTION_START_UPGRADE = 101; // start upgrade

  private int version;
  private short upgradeStatus;

  public UpgradeCommand() {
    super(UC_ACTION_UNKNOWN);
    this.version = 0;
    this.upgradeStatus = 0;
  }

  public UpgradeCommand(int action, int version, short status) {
    super(action);
    this.version = version;
    this.upgradeStatus = status;
  }

  public int getVersion() {
    return this.version;
  }

  public short getCurrentStatus() {
    return this.upgradeStatus;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  static {                                      // register a ctor
    WritableFactories.setFactory
      (UpgradeCommand.class,
       new WritableFactory() {
         public Writable newInstance() { return new UpgradeCommand(); }
       });
  }

  /**
   */
  public void write(DataOutput out) throws IOException {
    super.write(out);
    out.writeInt(this.version);
    out.writeShort(this.upgradeStatus);
  }

  /**
   */
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    this.version = in.readInt();
    this.upgradeStatus = in.readShort();
  }
}

