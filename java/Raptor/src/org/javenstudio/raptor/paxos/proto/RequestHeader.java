package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.WritableComparable;


public class RequestHeader implements WritableComparable<RequestHeader> {

  private int xid = 0; 
  private int type = 0; 

  public RequestHeader() {} 

  public RequestHeader(int xid, int type) {
    this.xid = xid; 
    this.type = type; 
  }

  public int getXid() { return xid; }
  public void setXid(int val) { this.xid = val; }

  public int getType() { return type; }
  public void setType(int val) { this.type = val; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof RequestHeader)) 
      return false;
    RequestHeader other = (RequestHeader)to; 
    return xid == other.xid && 
           type == other.type; 
  }

  public int compareTo(RequestHeader that) {
    if (that == null) return 1; 
    int ret = 0;
    ret = (xid == that.xid)? 0 :((xid<that.xid)?-1:1);
    if (ret != 0) return ret;
    ret = (type == that.type)? 0 :((type<that.type)?-1:1);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret;
    ret = (int)xid;
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
    out.writeInt(xid);
    out.writeInt(type);
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    xid = in.readInt(); 
    type = in.readInt(); 
  }

  public static RequestHeader read(DataInput in) throws IOException {
    RequestHeader result = new RequestHeader();
    result.readFields(in);
    return result;
  }
}
