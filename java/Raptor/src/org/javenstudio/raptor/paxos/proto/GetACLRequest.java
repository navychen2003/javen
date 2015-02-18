package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.util.StringUtils; 


@SuppressWarnings("deprecation")
public class GetACLRequest implements WritableComparable<GetACLRequest> {

  private String path = null; 

  public GetACLRequest() {} 

  public GetACLRequest(String path) {
    this.path = path; 
  }

  public String getPath() { return path; }
  public void setPath(String val) { this.path = val; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof GetACLRequest)) 
      return false;
    GetACLRequest other = (GetACLRequest)to; 
    return StringUtils.stringEquals(path, other.path); 
  }

  public int compareTo(GetACLRequest that) {
    if (that == null) return 1;
    int ret = 0;
    ret = path != null ? path.compareTo(that.path) : (that.path == null ? 0 : -1);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = path.hashCode();
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    UTF8.writeString(out, path);
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    path = UTF8.readString(in);
  }

  public static GetACLRequest read(DataInput in) throws IOException {
    GetACLRequest result = new GetACLRequest();
    result.readFields(in);
    return result;
  }
}
