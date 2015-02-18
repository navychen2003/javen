package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List; 
import java.util.ArrayList; 

import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.util.StringUtils; 


@SuppressWarnings("deprecation")
public class GetChildrenResponse implements WritableComparable<GetChildrenResponse> {

  private List<String> children = null; 

  public GetChildrenResponse() {} 

  public GetChildrenResponse(List<String> children) {
    this.children = children; 
  }

  public List<String> getChildren() { return children; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof GetChildrenResponse)) 
      return false;
    GetChildrenResponse other = (GetChildrenResponse)to; 
    return StringUtils.objectEquals(children, other.children); 
  }

  public int compareTo(GetChildrenResponse that) throws ClassCastException {
    throw new UnsupportedOperationException("comparing GetChildrenResponse is unimplemented");
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = children != null ? children.hashCode() : 0;
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
  }

  public static GetChildrenResponse read(DataInput in) throws IOException {
    GetChildrenResponse result = new GetChildrenResponse();
    result.readFields(in);
    return result;
  }
}
