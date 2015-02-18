package org.javenstudio.raptor.paxos.txn;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.WritableComparable;


public class ErrorTxn implements WritableComparable<ErrorTxn> {

  private int err = 0; 

  public ErrorTxn() {} 

  public ErrorTxn(int err) {
    this.err = err; 
  }

  public int getErr() { return err; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof ErrorTxn)) 
      return false;
    ErrorTxn other = (ErrorTxn)to; 
    return err == other.err; 
  }

  public int compareTo(ErrorTxn that) {
    if (that == null) return 1; 
    int ret = 0;
    ret = (err == that.err)? 0 :((err<that.err)?-1:1);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = (int)err;
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeInt(err); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    err = in.readInt(); 
  }

  public static ErrorTxn read(DataInput in) throws IOException {
    ErrorTxn result = new ErrorTxn();
    result.readFields(in);
    return result;
  }
}
