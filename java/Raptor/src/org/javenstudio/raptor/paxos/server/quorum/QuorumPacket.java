package org.javenstudio.raptor.paxos.server.quorum;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays; 
import java.util.List; 
import java.util.ArrayList; 

import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.io.WritableComparator;
import org.javenstudio.raptor.paxos.data.Id; 


public class QuorumPacket implements WritableComparable<QuorumPacket> {

  private int type = 0; 
  private long zxid = 0; 
  private byte[] data = null; 
  private List<Id> authinfo = null; 

  public QuorumPacket() {} 

  public QuorumPacket(int type, long zxid, byte[] data, List<Id> authinfo) {
    this.type = type; 
    this.zxid = zxid; 
    this.data = data; 
    this.authinfo = authinfo; 
  }

  public int getType() { return type; }
  public void setType(int val) { this.type = val; } 

  public long getZxid() { return zxid; }
  public void setZxid(long val) { this.zxid = val; } 

  public byte[] getData() { return data; }
  public void setData(byte[] val) { this.data = val; } 

  public List<Id> getAuthinfo() { return authinfo; }
  public void setAuthinfo(List<Id> val) { this.authinfo = val; } 

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof QuorumPacket)) 
      return false;
    QuorumPacket other = (QuorumPacket)to; 
    return type == other.type && 
           zxid == other.zxid && 
           WritableComparator.compareBytes(data, other.data) == 0 && 
           (authinfo != null ? authinfo.equals(other.authinfo) : false);
  }

  public int compareTo(QuorumPacket that) throws ClassCastException {
    throw new UnsupportedOperationException("comparing QuorumPacket is unimplemented");
  }

  public int hashCode() {
    int result = 17;
    int ret;
    ret = (int)type;
    result = 37*result + ret;
    ret = (int) (zxid^(zxid>>>32));
    result = 37*result + ret;
    ret = data != null ? Arrays.toString(data).hashCode() : 0; 
    result = 37*result + ret;
    ret = authinfo.hashCode();
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeInt(type);
    out.writeLong(zxid);
    out.writeInt(data != null ? data.length : 0); 
    if (data != null) out.write(data); 
    out.writeInt(authinfo != null ? authinfo.size() : 0); 
    if (authinfo != null) {
      for (Id id: authinfo) id.write(out); 
    }
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    type = in.readInt(); 
    zxid = in.readLong(); 
    int size = in.readInt(); 
    if (size > 0) {
      data = new byte[size]; 
      in.readFully(data, 0, size); 
    } else 
      data = null; 
    size = in.readInt(); 
    if (size > 0) {
      authinfo = new ArrayList<Id>(); 
      for (int i=0; i < size; i++) authinfo.add(Id.read(in)); 
    } else 
      authinfo = null; 
  }

  public static QuorumPacket read(DataInput in) throws IOException {
    QuorumPacket result = new QuorumPacket();
    result.readFields(in);
    return result;
  }
}
