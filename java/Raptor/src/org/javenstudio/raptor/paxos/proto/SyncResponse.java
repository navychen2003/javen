package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.util.StringUtils; 


@SuppressWarnings("deprecation")
public class SyncResponse implements WritableComparable<SyncResponse> {

  private String path = null; 

  public SyncResponse() {} 

  public SyncResponse(String path) {
    this.path = path; 
  }

  public String getPath() { return path; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof SyncResponse)) 
      return false;
    SyncResponse other = (SyncResponse)to; 
    return StringUtils.stringEquals(path, other.path); 
  }

  public int compareTo(SyncResponse that) {
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

  public static SyncResponse read(DataInput in) throws IOException {
    SyncResponse result = new SyncResponse();
    result.readFields(in);
    return result;
  }
}
