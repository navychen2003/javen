package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.util.StringUtils; 


@SuppressWarnings("deprecation")
public class GetChildren2Request implements WritableComparable<GetChildren2Request> {

  private String path = null; 
  private boolean watch = false; 

  public GetChildren2Request() {} 

  public GetChildren2Request(String path, boolean watch) {
    this.path = path; 
    this.watch = watch; 
  }

  public String getPath() { return path; }
  public void setPath(String val) { this.path = val; }

  public boolean getWatch() { return watch; }
  public void setWatch(boolean val) { this.watch = val; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof GetChildren2Request)) 
      return false;
    GetChildren2Request other = (GetChildren2Request)to; 
    return StringUtils.stringEquals(path, other.path) && 
           watch == other.watch; 
  }

  public int compareTo(GetChildren2Request that) {
    if (that == null) return 1;
    int ret = 0;
    ret = path != null ? path.compareTo(that.path) : (that.path == null ? 0 : -1);
    if (ret != 0) return ret;
    ret = (watch == that.watch)? 0 : (watch?1:-1);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = path != null ? path.hashCode() : 0;
    result = 37*result + ret;
    ret = (watch)?0:1;
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    UTF8.writeString(out, path);
    out.writeBoolean(watch); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    path = UTF8.readString(in);
    watch = in.readBoolean(); 
  }

  public static GetChildren2Request read(DataInput in) throws IOException {
    GetChildren2Request result = new GetChildren2Request();
    result.readFields(in);
    return result;
  }
}
