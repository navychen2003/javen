package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.WritableComparable;


public class ReplyHeader implements WritableComparable<ReplyHeader> {

  private int xid = 0; 
  private long zxid = 0; 
  private int err = 0; 

  public ReplyHeader() {} 

  public ReplyHeader(int xid, long zxid, int err) {
    this.xid = xid; 
    this.zxid = zxid; 
    this.err = err; 
  }

  public int getXid() { return xid; }
  public void setXid(int val) { this.xid = val; }

  public long getZxid() { return zxid; }
  public void setZxid(long val) { this.zxid = val; }

  public int getErr() { return err; }
  public void setErr(int val) { this.err = val; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof ReplyHeader)) 
      return false;
    ReplyHeader other = (ReplyHeader)to; 
    return xid == other.xid && 
           zxid == other.zxid && 
           err == other.err; 
  }

  public int compareTo(ReplyHeader that) {
    if (that == null) return 1; 
    int ret = 0;
    ret = (xid == that.xid)? 0 :((xid<that.xid)?-1:1);
    if (ret != 0) return ret;
    ret = (zxid == that.zxid)? 0 :((zxid<that.zxid)?-1:1);
    if (ret != 0) return ret;
    ret = (err == that.err)? 0 :((err<that.err)?-1:1);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret;
    ret = (int)xid;
    result = 37*result + ret;
    ret = (int)(zxid^(zxid>>>32));
    result = 37*result + ret;
    ret = (int)err;
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeInt(xid);
    out.writeLong(zxid);
    out.writeInt(err);
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    xid = in.readInt(); 
    zxid = in.readLong(); 
    err = in.readInt(); 
  }

  public static ReplyHeader read(DataInput in) throws IOException {
    ReplyHeader result = new ReplyHeader();
    result.readFields(in);
    return result;
  }
}
