package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List; 
import java.util.ArrayList; 
import java.util.Arrays; 

import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.io.WritableComparator;
import org.javenstudio.raptor.util.StringUtils; 
import org.javenstudio.raptor.paxos.data.ACL; 


@SuppressWarnings("deprecation")
public class CreateRequest implements WritableComparable<CreateRequest> {

  private String path = null; 
  private byte[] data = null; 
  private List<ACL> acl = null; 
  private int flags = 0; 

  public CreateRequest() {} 

  public CreateRequest(String path, byte[] data, List<ACL> acl, int flags) {
    this.path = path; 
    this.data = data; 
    this.acl = acl; 
    this.flags = flags; 
  }

  public String getPath() { return path; }
  public void setPath(String val) { this.path = val; }

  public byte[] getData() { return data; }
  public void setData(byte[] val) { this.data = val; }

  public List<ACL> getAcl() { return acl; } 
  public void setAcl(List<ACL> val) { this.acl = val; }

  public int getFlags() { return flags; }
  public void setFlags(int val) { this.flags = val; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof CreateRequest)) 
      return false;
    CreateRequest other = (CreateRequest)to; 
    return StringUtils.stringEquals(path, other.path) &&
           WritableComparator.compareBytes(data, other.data) == 0 && 
           StringUtils.objectEquals(acl, other.acl) && 
           flags == other.flags; 
  }

  public int compareTo(CreateRequest that) throws ClassCastException {
    throw new UnsupportedOperationException("comparing CreateRequest is unimplemented");
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = path != null ? path.hashCode() : 0;
    result = 37*result + ret;
    ret = data != null ? Arrays.toString(data).hashCode() : 0;
    result = 37*result + ret;
    ret = acl != null ? acl.hashCode() : 0;
    result = 37*result + ret;
    ret = (int)flags;
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
    out.writeInt(acl != null ? acl.size() : 0);
    if (acl != null) {
      for (ACL id: acl) id.write(out);
    }
    out.writeInt(flags); 
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
    size = in.readInt();
    if (size > 0) {
      acl = new ArrayList<ACL>();
      for (int i=0; i < size; i++) acl.add(ACL.read(in));
    } else
      acl = null;
    flags = in.readInt(); 
  }

  public static CreateRequest read(DataInput in) throws IOException {
    CreateRequest result = new CreateRequest();
    result.readFields(in);
    return result;
  }
}
