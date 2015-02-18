package org.javenstudio.raptor.paxos.txn;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.WritableComparable;


public class TxnHeader implements WritableComparable<TxnHeader> {

  private long clientId = 0; 
  private int cxid = 0; 
  private long zxid = 0; 
  private long time = 0; 
  private int type = 0; 

  public TxnHeader() {} 

  public TxnHeader(long clientId, int cxid, long zxid, long time, int type) {
    this.clientId = clientId; 
    this.cxid = cxid; 
    this.zxid = zxid; 
    this.time = time; 
    this.type = type; 
  }

  public long getClientId() { return clientId; }
  public void setClientId(long val) { this.clientId = val; }

  public int getCxid() { return cxid; }
  public void setCxid(int val) { this.cxid = val; }

  public long getZxid() { return zxid; }
  public void setZxid(long val) { this.zxid = val; }

  public long getTime() { return time; }
  public void setTime(long val) { this.time = val; }

  public int getType() { return type; }
  public void setType(int val) { this.type = val; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof TxnHeader)) 
      return false;
    TxnHeader other = (TxnHeader)to; 
    return clientId == other.clientId &&
           cxid == other.cxid && 
           zxid == other.zxid && 
           time == other.time && 
           type == other.type; 
  }

  public int compareTo(TxnHeader that) {
    if (that == null) return 1; 
    int ret = 0;
    ret = (clientId == that.clientId)? 0 :((clientId<that.clientId)?-1:1);
    if (ret != 0) return ret;
    ret = (cxid == that.cxid)? 0 :((cxid<that.cxid)?-1:1);
    if (ret != 0) return ret;
    ret = (zxid == that.zxid)? 0 :((zxid<that.zxid)?-1:1);
    if (ret != 0) return ret;
    ret = (time == that.time)? 0 :((time<that.time)?-1:1);
    if (ret != 0) return ret;
    ret = (type == that.type)? 0 :((type<that.type)?-1:1);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = (int) (clientId^(clientId>>>32));
    result = 37*result + ret;
    ret = (int)cxid;
    result = 37*result + ret;
    ret = (int) (zxid^(zxid>>>32));
    result = 37*result + ret;
    ret = (int) (time^(time>>>32));
    result = 37*result + ret;
    ret = (int)type;
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeLong(clientId); 
    out.writeInt(cxid); 
    out.writeLong(zxid); 
    out.writeLong(time); 
    out.writeInt(type); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    clientId = in.readLong(); 
    cxid = in.readInt(); 
    zxid = in.readLong(); 
    time = in.readLong(); 
    type = in.readInt(); 
  }

  public static TxnHeader read(DataInput in) throws IOException {
    TxnHeader result = new TxnHeader();
    result.readFields(in);
    return result;
  }
}
