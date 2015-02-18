package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.util.StringUtils; 


@SuppressWarnings("deprecation")
public class DeleteRequest implements WritableComparable<DeleteRequest> {

  private String path = null; 
  private int version = 0; 

  public DeleteRequest() {} 

  public DeleteRequest(String path, int version) {
    this.path = path; 
    this.version = version; 
  }

  public String getPath() { return path; }
  public void setPath(String val) { this.path = val; }

  public int getVersion() { return version; }
  public void setVersion(int val) { this.version = val; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof DeleteRequest)) 
      return false;
    DeleteRequest other = (DeleteRequest)to; 
    return StringUtils.stringEquals(path, other.path) &&
           version == other.version; 
  }

  public int compareTo(DeleteRequest that) {
    if (that == null) return 1;
    int ret = 0;
    ret = path != null ? path.compareTo(that.path) : (that.path == null ? 0 : -1);
    if (ret != 0) return ret;
    ret = (version == that.version)? 0 : (version > that.version ? 1 : -1);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = path != null ? path.hashCode() : 0;
    result = 37*result + ret;
    ret = (int)version;
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    UTF8.writeString(out, path);
    out.writeInt(version); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    path = UTF8.readString(in);
    version = in.readInt(); 
  }

  public static DeleteRequest read(DataInput in) throws IOException {
    DeleteRequest result = new DeleteRequest();
    result.readFields(in);
    return result;
  }
}
