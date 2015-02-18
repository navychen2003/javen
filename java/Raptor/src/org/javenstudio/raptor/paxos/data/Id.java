package org.javenstudio.raptor.paxos.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.util.StringUtils; 


@SuppressWarnings("deprecation")
public class Id implements WritableComparable<Id> {

  private String scheme = null; 
  private String id = null; 

  private Id() {} 

  public Id(String scheme, String id) {
    this.scheme = scheme; 
    this.id = id; 
  }

  public String getScheme() { return scheme; }
  public String getId() { return id; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof Id)) 
      return false;
    Id other = (Id)to; 
    return StringUtils.stringEquals(scheme, other.scheme) &&
           StringUtils.stringEquals(id, other.id);
  }

  public int compareTo(Id that) {
    if (that == null) return 1; 
    int ret = 0;
    ret = scheme != null ? scheme.compareTo(that.scheme) : (that.scheme != null ? -1 : 0);
    if (ret != 0) return ret;
    ret = id != null ? id.compareTo(that.id) : (that.id != null ? -1 : 0);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = scheme != null ? scheme.hashCode() : 0;
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
    UTF8.writeString(out, scheme);
    UTF8.writeString(out, id);
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    scheme = UTF8.readString(in);
    id = UTF8.readString(in);
  }

  public static Id read(DataInput in) throws IOException {
    Id result = new Id();
    result.readFields(in);
    return result;
  }
}
