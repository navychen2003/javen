package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.util.StringUtils; 
import org.javenstudio.raptor.paxos.data.Stat; 


public class SetDataResponse implements WritableComparable<SetDataResponse> {

  private Stat stat = null; 

  public SetDataResponse() {} 

  public SetDataResponse(Stat stat) {
    this.stat = stat; 
  }

  public Stat getStat() { return stat; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof SetDataResponse)) 
      return false;
    SetDataResponse other = (SetDataResponse)to; 
    return StringUtils.objectEquals(stat, other.stat); 
  }

  public int compareTo(SetDataResponse that) {
    if (that == null) return 1;
    int ret = 0;
    ret = stat != null ? stat.compareTo(that.stat) : (that.stat == null ? 0 : -1);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = stat != null ? stat.hashCode() : 0;
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeBoolean(stat != null); 
    if (stat != null) stat.write(out); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    if (in.readBoolean()) 
      stat = Stat.read(in); 
    else
      stat = null; 
  }

  public static SetDataResponse read(DataInput in) throws IOException {
    SetDataResponse result = new SetDataResponse();
    result.readFields(in);
    return result;
  }
}
