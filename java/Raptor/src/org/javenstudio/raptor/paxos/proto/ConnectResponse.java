package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays; 

import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.io.WritableComparator;


public class ConnectResponse implements WritableComparable<ConnectResponse> {

  private int protocolVersion = 0; 
  private int timeOut = 0; 
  private long sessionId = 0; 
  private byte[] passwd = null; 

  private ConnectResponse() {} 

  public ConnectResponse(int protocolVersion, int timeOut, long sessionId, byte[] passwd) {
    this.protocolVersion = protocolVersion; 
    this.timeOut = timeOut; 
    this.sessionId = sessionId; 
    this.passwd = passwd; 
  }

  public int getProtocolVersion() { return protocolVersion; }
  public int getTimeOut() { return timeOut; }
  public long getSessionId() { return sessionId; }
  public byte[] getPasswd() { return passwd; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof ConnectResponse)) 
      return false;
    ConnectResponse other = (ConnectResponse)to; 
    return protocolVersion == other.protocolVersion && 
           timeOut == other.timeOut && 
           sessionId == other.sessionId && 
           WritableComparator.compareBytes(passwd, other.passwd) == 0;
  }

  public int compareTo(ConnectResponse that) {
    if (that == null) return 1; 
    int ret = 0;
    ret = (protocolVersion == that.protocolVersion)? 0 :((protocolVersion<that.protocolVersion)?-1:1);
    if (ret != 0) return ret;
    ret = (timeOut == that.timeOut)? 0 :((timeOut<that.timeOut)?-1:1);
    if (ret != 0) return ret;
    ret = (sessionId == that.sessionId)? 0 :((sessionId<that.sessionId)?-1:1);
    if (ret != 0) return ret;
    ret = WritableComparator.compareBytes(passwd, that.passwd); 
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret;
    ret = (int)protocolVersion;
    result = 37*result + ret;
    ret = (int)timeOut;
    result = 37*result + ret;
    ret = (int)(sessionId^(sessionId>>>32));
    result = 37*result + ret;
    ret = passwd != null ? Arrays.toString(passwd).hashCode() : 0; 
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeInt(protocolVersion);
    out.writeInt(timeOut);
    out.writeLong(sessionId);
    out.writeInt(passwd != null ? passwd.length : 0); 
    if (passwd != null) out.write(passwd); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    protocolVersion = in.readInt(); 
    timeOut = in.readInt(); 
    sessionId = in.readLong(); 
    int size = in.readInt(); 
    if (size > 0) {
      passwd = new byte[size]; 
      in.readFully(passwd, 0, size); 
    } else 
      passwd = null; 
  }

  public static ConnectResponse read(DataInput in) throws IOException {
    ConnectResponse result = new ConnectResponse();
    result.readFields(in);
    return result;
  }
}
