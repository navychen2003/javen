package org.javenstudio.raptor.paxos.txn;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List; 
import java.util.ArrayList; 

import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.util.StringUtils; 
import org.javenstudio.raptor.paxos.data.ACL; 


@SuppressWarnings("deprecation")
public class SetACLTxn implements WritableComparable<SetACLTxn> {

  private String path = null; 
  private List<ACL> acl = null; 
  private int version = 0; 

  public SetACLTxn() {} 

  public SetACLTxn(String path, List<ACL> acl, int version) {
    this.path = path; 
    this.acl = acl; 
    this.version = version; 
  }

  public String getPath() { return path; }
  public List<ACL> getAcl() { return acl; } 
  public int getVersion() { return version; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof SetACLTxn)) 
      return false;
    SetACLTxn other = (SetACLTxn)to; 
    return StringUtils.stringEquals(path, other.path) &&
           StringUtils.objectEquals(acl, other.acl) && 
           version == other.version; 
  }

  public int compareTo(SetACLTxn that) throws ClassCastException {
    throw new UnsupportedOperationException("comparing SetACLTxn is unimplemented");
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = path != null ? path.hashCode() : 0;
    result = 37*result + ret;
    ret = acl != null ? acl.hashCode() : 0;
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
    out.writeInt(acl != null ? acl.size() : 0);
    if (acl != null) {
      for (ACL id: acl) id.write(out);
    }
    out.writeInt(version); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    path = UTF8.readString(in);
    int size = in.readInt();
    if (size > 0) {
      acl = new ArrayList<ACL>();
      for (int i=0; i < size; i++) acl.add(ACL.read(in));
    } else
      acl = null;
    version = in.readInt(); 
  }

  public static SetACLTxn read(DataInput in) throws IOException {
    SetACLTxn result = new SetACLTxn();
    result.readFields(in);
    return result;
  }
}
