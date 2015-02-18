package org.javenstudio.raptor.dfs.server.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.dfs.protocol.FSConstants;
import org.javenstudio.raptor.dfs.server.common.Storage;
import org.javenstudio.raptor.dfs.server.common.StorageInfo;
import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.io.WritableFactories;
import org.javenstudio.raptor.io.WritableFactory;

/**
 * NamespaceInfo is returned by the name-node in reply 
 * to a data-node handshake.
 * 
 */
@SuppressWarnings("deprecation")
public class NamespaceInfo extends StorageInfo implements Writable {
  String  buildVersion;
  int distributedUpgradeVersion;

  public NamespaceInfo() {
    super();
    buildVersion = null;
  }
  
  public NamespaceInfo(int nsID, long cT, int duVersion) {
    super(FSConstants.LAYOUT_VERSION, nsID, cT);
    buildVersion = Storage.getBuildVersion();
    this.distributedUpgradeVersion = duVersion;
  }
  
  public String getBuildVersion() {
    return buildVersion;
  }

  public int getDistributedUpgradeVersion() {
    return distributedUpgradeVersion;
  }
  
  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  static {                                      // register a ctor
    WritableFactories.setFactory
      (NamespaceInfo.class,
       new WritableFactory() {
         public Writable newInstance() { return new NamespaceInfo(); }
       });
  }

  public void write(DataOutput out) throws IOException {
    UTF8.writeString(out, getBuildVersion());
    out.writeInt(getLayoutVersion());
    out.writeInt(getNamespaceID());
    out.writeLong(getCTime());
    out.writeInt(getDistributedUpgradeVersion());
  }

  public void readFields(DataInput in) throws IOException {
    buildVersion = UTF8.readString(in);
    layoutVersion = in.readInt();
    namespaceID = in.readInt();
    cTime = in.readLong();
    distributedUpgradeVersion = in.readInt();
  }
}

