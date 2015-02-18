package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List; 
import java.util.ArrayList; 

import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.util.StringUtils; 
import org.javenstudio.raptor.paxos.data.Stat; 


@SuppressWarnings("deprecation")
public class GetChildren2Response implements WritableComparable<GetChildren2Response> {

  private List<String> children = null; 
  private Stat stat = null; 

  public GetChildren2Response() {} 

  public GetChildren2Response(List<String> children, Stat stat) {
    this.children = children; 
    this.stat = stat; 
  }

  public List<String> getChildren() { return children; }
  public Stat getStat() { return stat; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof GetChildren2Response)) 
      return false;
    GetChildren2Response other = (GetChildren2Response)to; 
    return StringUtils.objectEquals(children, other.children) && 
           StringUtils.objectEquals(stat, other.stat); 
  }

  public int compareTo(GetChildren2Response that) throws ClassCastException {
    throw new UnsupportedOperationException("comparing GetChildren2Response is unimplemented");
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = children != null ? children.hashCode() : 0;
    result = 37*result + ret;
    ret = stat != null ? stat.hashCode() : 0;
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeInt(children != null ? children.size() : 0); 
    if (children != null) {
      for (String child: children) UTF8.writeString(out, child); 
    }
    out.writeBoolean(stat != null); 
    if (stat != null) stat.write(out); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    int size = in.readInt(); 
    if (size > 0) {
      children = new ArrayList<String>(); 
      for (int i=0; i < size; i++) 
        children.add(UTF8.readString(in)); 
    } else
      children = null; 
    if (in.readBoolean()) 
      stat = Stat.read(in); 
    else
      stat = null; 
  }

  public static GetChildren2Response read(DataInput in) throws IOException {
    GetChildren2Response result = new GetChildren2Response();
    result.readFields(in);
    return result;
  }
}
