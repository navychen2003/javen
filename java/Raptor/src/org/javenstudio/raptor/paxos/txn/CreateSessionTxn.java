package org.javenstudio.raptor.paxos.txn;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.WritableComparable;


public class CreateSessionTxn implements WritableComparable<CreateSessionTxn> {

  private int timeOut = 0; 

  public CreateSessionTxn() {} 

  public CreateSessionTxn(int timeOut) {
    this.timeOut = timeOut; 
  }

  public int getTimeOut() { return timeOut; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof CreateSessionTxn)) 
      return false;
    CreateSessionTxn other = (CreateSessionTxn)to; 
    return timeOut == other.timeOut; 
  }

  public int compareTo(CreateSessionTxn that) {
    if (that == null) return 1; 
    int ret = 0;
    ret = (timeOut == that.timeOut)? 0 :((timeOut<that.timeOut)?-1:1);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = (int)timeOut;
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeInt(timeOut); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    timeOut = in.readInt(); 
  }

  public static CreateSessionTxn read(DataInput in) throws IOException {
    CreateSessionTxn result = new CreateSessionTxn();
    result.readFields(in);
    return result;
  }
}
