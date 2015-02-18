package org.javenstudio.raptor.paxos.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.util.StringUtils; 


public class ACL implements WritableComparable<ACL> {

  private int perms = 0; 
  private Id id = null; 

  public ACL() {} 

  public ACL(int perms, Id id) {
    this.perms = perms; 
    this.id = id; 
  }

  public int getPerms() { return perms; }
  public void setPerms(int val) { this.perms = val; }

  public Id getId() { return id; }
  public void setId(Id val) { this.id = val; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof ACL)) 
      return false;
    ACL other = (ACL)to; 
    return perms == other.perms &&
           StringUtils.objectEquals(id, other.id);
  }

  public int compareTo(ACL that) {
    if (that == null) return 1; 
    int ret = 0;
    ret = (perms == that.perms)? 0 :((perms<that.perms)?-1:1);
    if (ret != 0) return ret;
    ret = id != null ? id.compareTo(that.id) : -1;
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = (int)perms;
    result = 37*result + ret;
    ret = id != null ? id.hashCode() : 0;
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeInt(perms); 
    out.writeBoolean(id != null);
    if (id != null) id.write(out); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    perms = in.readInt(); 
    if (in.readBoolean()) 
      id = Id.read(in); 
    else
      id = null; 
  }

  public static ACL read(DataInput in) throws IOException {
    ACL result = new ACL();
    result.readFields(in);
    return result;
  }
}
