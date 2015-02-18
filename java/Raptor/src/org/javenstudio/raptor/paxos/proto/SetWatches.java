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
public class SetWatches implements WritableComparable<SetWatches> {

  private long relativeZxid = 0; 
  private List<String> dataWatches = null; 
  private List<String> existWatches = null; 
  private List<String> childWatches = null; 

  public SetWatches() {} 

  public SetWatches(long relativeZxid, List<String> dataWatches, List<String> existWatches, List<String> childWatches) {
    this.relativeZxid = relativeZxid; 
    this.dataWatches = dataWatches; 
    this.existWatches = existWatches; 
    this.childWatches = childWatches; 
  }

  public long getRelativeZxid() { return relativeZxid; } 
  public List<String> getDataWatches() { return dataWatches; }
  public List<String> getExistWatches() { return existWatches; }
  public List<String> getChildWatches() { return childWatches; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof SetWatches)) 
      return false;
    SetWatches other = (SetWatches)to; 
    return relativeZxid == other.relativeZxid && 
           StringUtils.objectEquals(dataWatches, other.dataWatches) && 
           StringUtils.objectEquals(existWatches, other.existWatches) && 
           StringUtils.objectEquals(childWatches, other.childWatches); 
  }

  public int compareTo(SetWatches that) throws ClassCastException {
    throw new UnsupportedOperationException("comparing SetWatches is unimplemented");
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = (int) (relativeZxid^(relativeZxid>>>32));
    result = 37*result + ret;
    ret = dataWatches != null ? dataWatches.hashCode() : 0;
    result = 37*result + ret;
    ret = existWatches != null ? existWatches.hashCode() : 0;
    result = 37*result + ret;
    ret = childWatches != null ? childWatches.hashCode() : 0;
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    writeList(out, dataWatches); 
    writeList(out, existWatches); 
    writeList(out, childWatches); 
  }

  private static void writeList(DataOutput out, List<String> watches) throws IOException {
    out.writeInt(watches != null ? watches.size() : 0); 
    if (watches != null) {
      for (String child: watches) UTF8.writeString(out, child); 
    }
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    dataWatches = readList(in); 
    existWatches = readList(in); 
    childWatches = readList(in); 
  }

  private static List<String> readList(DataInput in) throws IOException {
    List<String> watches = null; 
    int size = in.readInt(); 
    if (size > 0) {
      watches = new ArrayList<String>(); 
      for (int i=0; i < size; i++) 
        watches.add(UTF8.readString(in)); 
    } else
      watches = null; 
    return watches; 
  }

  public static SetWatches read(DataInput in) throws IOException {
    SetWatches result = new SetWatches();
    result.readFields(in);
    return result;
  }
}
