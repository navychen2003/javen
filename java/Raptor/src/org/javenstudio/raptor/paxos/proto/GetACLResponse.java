package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List; 
import java.util.ArrayList; 

import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.util.StringUtils; 
import org.javenstudio.raptor.paxos.data.ACL; 
import org.javenstudio.raptor.paxos.data.Stat; 


public class GetACLResponse implements WritableComparable<GetACLResponse> {

  private List<ACL> acl = null; 
  private Stat stat = null; 

  public GetACLResponse() {} 

  public GetACLResponse(List<ACL> acl, Stat stat) {
    this.acl = acl; 
    this.stat = stat; 
  }

  public List<ACL> getAcl() { return acl; }
  public Stat getStat() { return stat; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof GetACLResponse)) 
      return false;
    GetACLResponse other = (GetACLResponse)to; 
    return StringUtils.objectEquals(acl, other.acl) && 
           StringUtils.objectEquals(stat, other.stat); 
  }

  public int compareTo(GetACLResponse that) throws ClassCastException {
    throw new UnsupportedOperationException("comparing GetACLResponse is unimplemented");
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = acl != null ? acl.hashCode() : 0;
    result = 37*result + ret;
    ret = acl != null ? stat.hashCode() : 0;
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeInt(acl != null ? acl.size() : 0);
    if (acl != null) {
      for (ACL id: acl) id.write(out);
    }
    out.writeBoolean(stat != null); 
    if (stat != null) stat.write(out); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    int size = in.readInt();
    if (size > 0) {
      acl = new ArrayList<ACL>();
      for (int i=0; i < size; i++) acl.add(ACL.read(in));
    } else
      acl = null;
    if (in.readBoolean()) 
      stat = Stat.read(in); 
    else
      stat = null; 
  }

  public static GetACLResponse read(DataInput in) throws IOException {
    GetACLResponse result = new GetACLResponse();
    result.readFields(in);
    return result;
  }
}
