package org.javenstudio.raptor.paxos.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.WritableComparable;


public class StatPersisted implements WritableComparable<StatPersisted> {

  private long czxid = 0; 
  private long mzxid = 0; 
  private long ctime = 0; 
  private long mtime = 0; 
  private int version = 0; 
  private int cversion = 0; 
  private int aversion = 0; 
  private long ephemeralOwner = 0; 
  private long pzxid = 0; 

  public StatPersisted() {} 

  public StatPersisted(long czxid, long mzxid, long ctime, long mtime, int version, int cversion, int aversion, long ephemeralOwner, long pzxid) {
    this.czxid = czxid; 
    this.mzxid = mzxid; 
    this.ctime = ctime; 
    this.mtime = mtime; 
    this.version = version; 
    this.cversion = cversion; 
    this.aversion = aversion; 
    this.ephemeralOwner = ephemeralOwner; 
    this.pzxid = pzxid; 
  }

  public long getCzxid() { return czxid; }
  public void setCzxid(long val) { this.czxid = val; } 

  public long getMzxid() { return mzxid; }
  public void setMzxid(long val) { this.mzxid = val; } 

  public long getCtime() { return ctime; }
  public void setCtime(long val) { this.ctime = val; } 

  public long getMtime() { return mtime; }
  public void setMtime(long val) { this.mtime = val; } 

  public int getVersion() { return version; }
  public void setVersion(int val) { this.version = val; } 

  public int getCversion() { return cversion; }
  public void setCversion(int val) { this.cversion = val; } 

  public int getAversion() { return aversion; }
  public void setAversion(int val) { this.aversion = val; } 

  public long getEphemeralOwner() { return ephemeralOwner; }
  public void setEphemeralOwner(long val) { this.ephemeralOwner = val; } 

  public long getPzxid() { return pzxid; }
  public void setPzxid(long val) { this.pzxid = val; } 

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof StatPersisted)) 
      return false;
    StatPersisted other = (StatPersisted)to; 
    return czxid == other.czxid &&
           mzxid == other.mzxid && 
           ctime == other.ctime && 
           mtime == other.mtime && 
           version == other.version && 
           cversion == other.cversion && 
           aversion == other.aversion && 
           ephemeralOwner == other.ephemeralOwner && 
           pzxid == other.pzxid; 
  }

  public int compareTo(StatPersisted that) {
    if (that == null) return 1; 
    int ret = 0;
    ret = (czxid == that.czxid)? 0 :((czxid<that.czxid)?-1:1);
    if (ret != 0) return ret;
    ret = (mzxid == that.mzxid)? 0 :((mzxid<that.mzxid)?-1:1);
    if (ret != 0) return ret;
    ret = (ctime == that.ctime)? 0 :((ctime<that.ctime)?-1:1);
    if (ret != 0) return ret;
    ret = (mtime == that.mtime)? 0 :((mtime<that.mtime)?-1:1);
    if (ret != 0) return ret;
    ret = (version == that.version)? 0 :((version<that.version)?-1:1);
    if (ret != 0) return ret;
    ret = (cversion == that.cversion)? 0 :((cversion<that.cversion)?-1:1);
    if (ret != 0) return ret;
    ret = (aversion == that.aversion)? 0 :((aversion<that.aversion)?-1:1);
    if (ret != 0) return ret;
    ret = (ephemeralOwner == that.ephemeralOwner)? 0 :((ephemeralOwner<that.ephemeralOwner)?-1:1);
    if (ret != 0) return ret;
    ret = (pzxid == that.pzxid)? 0 :((pzxid<that.pzxid)?-1:1);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = (int) (czxid^(czxid>>>32));
    result = 37*result + ret;
    ret = (int) (mzxid^(mzxid>>>32));
    result = 37*result + ret;
    ret = (int) (ctime^(ctime>>>32));
    result = 37*result + ret;
    ret = (int) (mtime^(mtime>>>32));
    result = 37*result + ret;
    ret = (int)version;
    result = 37*result + ret;
    ret = (int)cversion;
    result = 37*result + ret;
    ret = (int)aversion;
    result = 37*result + ret;
    ret = (int) (ephemeralOwner^(ephemeralOwner>>>32));
    result = 37*result + ret;
    ret = (int) (pzxid^(pzxid>>>32));
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeLong(czxid); 
    out.writeLong(mzxid); 
    out.writeLong(ctime); 
    out.writeLong(mtime); 
    out.writeInt(version); 
    out.writeInt(cversion); 
    out.writeInt(aversion); 
    out.writeLong(ephemeralOwner); 
    out.writeLong(pzxid); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    czxid = in.readLong(); 
    mzxid = in.readLong(); 
    ctime = in.readLong(); 
    mtime = in.readLong(); 
    version = in.readInt(); 
    cversion = in.readInt(); 
    aversion = in.readInt(); 
    ephemeralOwner = in.readLong(); 
    pzxid = in.readLong(); 
  }

  public static StatPersisted read(DataInput in) throws IOException {
    StatPersisted result = new StatPersisted();
    result.readFields(in);
    return result;
  }
}
