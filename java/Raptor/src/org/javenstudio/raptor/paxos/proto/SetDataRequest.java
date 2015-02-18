package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays; 

import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.io.WritableComparator;
import org.javenstudio.raptor.util.StringUtils; 


@SuppressWarnings("deprecation")
public class SetDataRequest implements WritableComparable<SetDataRequest> {

  private String path = null; 
  private byte[] data = null; 
  private int version = 0; 

  public SetDataRequest() {} 

  public SetDataRequest(String path, byte[] data, int version) {
    this.path = path; 
    this.data = data; 
    this.version = version; 
  }

  public String getPath() { return path; }
  public void setPath(String val) { this.path = val; }

  public byte[] getData() { return data; }
  public void setData(byte[] val) { this.data = val; }

  public int getVersion() { return version; }
  public void setVersion(int val) { this.version = val; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof SetDataRequest)) 
      return false;
    SetDataRequest other = (SetDataRequest)to; 
    return StringUtils.stringEquals(path, other.path) &&
           WritableComparator.compareBytes(data, other.data) == 0 && 
           version == other.version; 
  }

  public int compareTo(SetDataRequest that) {
    if (that == null) return 1;
    int ret = 0;
    ret = path != null ? path.compareTo(that.path) : (that.path != null ? -1 : 0);
    if (ret != 0) return ret;
    ret = WritableComparator.compareBytes(data, that.data);
    if (ret != 0) return ret;
    ret = (version == that.version)? 0 :((version<that.version)?-1:1);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = path != null ? path.hashCode() : 0;
    result = 37*result + ret;
    ret = data != null ? Arrays.toString(data).hashCode() : 0;
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
    out.writeInt(data != null ? data.length : 0);
    if (data != null) out.write(data);
    out.writeInt(version); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    path = UTF8.readString(in);
    int size = in.readInt();
    if (size > 0) {
      data = new byte[size];
      in.readFully(data, 0, size);
    } else
      data = null;
    version = in.readInt(); 
  }

  public static SetDataRequest read(DataInput in) throws IOException {
    SetDataRequest result = new SetDataRequest();
    result.readFields(in);
    return result;
  }
}
