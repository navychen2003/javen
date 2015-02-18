package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.util.StringUtils; 


@SuppressWarnings("deprecation")
public class WatcherEvent implements WritableComparable<WatcherEvent> {

  private int type = 0; 
  private int state = 0; 
  private String path = null; 

  public WatcherEvent() {} 

  public WatcherEvent(int type, int state, String path) {
    this.type = type; 
    this.state = state; 
    this.path = path; 
  }

  public int getType() { return type; }
  public void setType(int val) { this.type = val; }

  public int getState() { return state; }
  public void setState(int val) { this.state = val; } 

  public String getPath() { return path; }
  public void setPath(String val) { this.path = val; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof WatcherEvent)) 
      return false;
    WatcherEvent other = (WatcherEvent)to; 
    return type == other.type && 
           state == other.state && 
           StringUtils.stringEquals(path, other.path);
  }

  public int compareTo(WatcherEvent that) {
    if (that == null) return 1; 
    int ret = 0;
    ret = (type == that.type)? 0 :((type<that.type)?-1:1);
    if (ret != 0) return ret;
    ret = (state == that.state)? 0 :((state<that.state)?-1:1);
    if (ret != 0) return ret;
    ret = path != null ? path.compareTo(that.path) : (that.path != null ? -1 : 0);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret;
    ret = (int)type;
    result = 37*result + ret;
    ret = (int)state;
    result = 37*result + ret;
    ret = path != null ? path.hashCode() : 0;
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeInt(type);
    out.writeInt(state);
    UTF8.writeString(out, path);
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    type = in.readInt(); 
    state = in.readInt(); 
    path = UTF8.readString(in);
  }

  public static WatcherEvent read(DataInput in) throws IOException {
    WatcherEvent result = new WatcherEvent();
    result.readFields(in);
    return result;
  }
}
