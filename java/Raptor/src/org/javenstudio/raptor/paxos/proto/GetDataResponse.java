package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays; 

import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.io.WritableComparator;
import org.javenstudio.raptor.util.StringUtils; 
import org.javenstudio.raptor.paxos.data.Stat; 


public class GetDataResponse implements WritableComparable<GetDataResponse> {

  private byte[] data = null; 
  private Stat stat = null; 

  public GetDataResponse() {} 

  public GetDataResponse(byte[] data, Stat stat) {
    this.data = data; 
    this.stat = stat; 
  }

  public byte[] getData() { return data; }
  public Stat getStat() { return stat; } 

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof GetDataResponse)) 
      return false;
    GetDataResponse other = (GetDataResponse)to; 
    return WritableComparator.compareBytes(data, other.data) == 0 && 
           StringUtils.objectEquals(stat, other.stat); 
  }

  public int compareTo(GetDataResponse that) { 
    if (that == null) return 1;
    int ret = 0;
    ret = WritableComparator.compareBytes(data, that.data);
    if (ret != 0) return ret;
    ret = stat != null ? stat.compareTo(that.stat) : (that.stat == null ? 0 : -1);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = data != null ? Arrays.toString(data).hashCode() : 0;
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
    out.writeInt(data != null ? data.length : 0);
    if (data != null) out.write(data);
    out.writeBoolean(stat != null);
    if (stat != null) stat.write(out); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    int size = in.readInt();
    if (size > 0) {
      data = new byte[size];
      in.readFully(data, 0, size);
    } else
      data = null;
    if (in.readBoolean()) 
      stat = Stat.read(in); 
    else
      stat = null; 
  }

  public static GetDataResponse read(DataInput in) throws IOException {
    GetDataResponse result = new GetDataResponse();
    result.readFields(in);
    return result;
  }
}
