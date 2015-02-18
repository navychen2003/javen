package org.javenstudio.raptor.dfs.server.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.dfs.protocol.DatanodeID;
import org.javenstudio.raptor.dfs.server.common.Storage;
import org.javenstudio.raptor.dfs.server.common.StorageInfo;
import org.javenstudio.raptor.dfs.server.datanode.DataStorage;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.io.WritableFactories;
import org.javenstudio.raptor.io.WritableFactory;

/** 
 * DatanodeRegistration class conatins all information the Namenode needs
 * to identify and verify a Datanode when it contacts the Namenode.
 * This information is sent by Datanode with each communication request.
 * 
 */
public class DatanodeRegistration extends DatanodeID implements Writable {
  static {                                      // register a ctor
    WritableFactories.setFactory
      (DatanodeRegistration.class,
       new WritableFactory() {
         public Writable newInstance() { return new DatanodeRegistration(); }
       });
  }

  public StorageInfo storageInfo;

  /**
   * Default constructor.
   */
  public DatanodeRegistration() {
    this("");
  }
  
  /**
   * Create DatanodeRegistration
   */
  public DatanodeRegistration(String nodeName) {
    super(nodeName);
    this.storageInfo = new StorageInfo();
  }
  
  public void setInfoPort(int infoPort) {
    this.infoPort = infoPort;
  }
  
  public void setIpcPort(int ipcPort) {
    this.ipcPort = ipcPort;
  }

  public void setStorageInfo(DataStorage storage) {
    this.storageInfo = new StorageInfo(storage);
    this.storageID = storage.getStorageID();
  }
  
  public void setName(String name) {
    this.name = name;
  }

  /**
   */
  public int getVersion() {
    return storageInfo.getLayoutVersion();
  }
  
  /**
   */
  public String getRegistrationID() {
    return Storage.getRegistrationID(storageInfo);
  }

  public String toString() {
    return getClass().getSimpleName()
      + "(" + name
      + ", storageID=" + storageID
      + ", infoPort=" + infoPort
      + ", ipcPort=" + ipcPort
      + ")";
  }
  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    super.write(out);

    //TODO: move it to DatanodeID once HADOOP-2797 has been committed
    out.writeShort(ipcPort);

    out.writeInt(storageInfo.getLayoutVersion());
    out.writeInt(storageInfo.getNamespaceID());
    out.writeLong(storageInfo.getCTime());
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);

    //TODO: move it to DatanodeID once HADOOP-2797 has been committed
    this.ipcPort = in.readShort() & 0x0000ffff;

    storageInfo.layoutVersion = in.readInt();
    storageInfo.namespaceID = in.readInt();
    storageInfo.cTime = in.readLong();
  }
}

