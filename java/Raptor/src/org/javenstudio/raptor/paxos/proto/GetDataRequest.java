package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.util.StringUtils; 


@SuppressWarnings("deprecation")
public class GetDataRequest implements WritableComparable<GetDataRequest> {

  private String path = null; 
  private boolean watch = false; 

  public GetDataRequest() {} 

  public GetDataRequest(String path, boolean watch) {
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
    if (!(to instanceof GetDataRequest)) 
      return false;
    GetDataRequest other = (GetDataRequest)to; 
    return StringUtils.stringEquals(path, other.path) && 
           watch == other.watch; 
  }

  public int compareTo(GetDataRequest that) {
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

  public static GetDataRequest read(DataInput in) throws IOException {
    GetDataRequest result = new GetDataRequest();
    result.readFields(in);
    return result;
  }
}
