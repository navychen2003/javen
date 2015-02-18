package org.javenstudio.raptor.paxos.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays; 

import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.io.WritableComparator;
import org.javenstudio.raptor.util.StringUtils; 


@SuppressWarnings("deprecation")
public class AuthPacket implements WritableComparable<AuthPacket> {

  private int type = 0; 
  private String scheme = null; 
  private byte[] auth = null; 

  public AuthPacket() {} 

  public AuthPacket(int type, String scheme, byte[] auth) {
    this.type = type; 
    this.scheme = scheme; 
    this.auth = auth; 
  }

  public int getType() { return type; }
  public String getScheme() { return scheme; }
  public byte[] getAuth() { return auth; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof AuthPacket)) 
      return false;
    AuthPacket other = (AuthPacket)to; 
    return type == other.type && 
           StringUtils.stringEquals(scheme, other.scheme) &&
           WritableComparator.compareBytes(auth, other.auth) == 0;
  }

  public int compareTo(AuthPacket that) {
    if (that == null) return 1; 
    int ret = 0;
    ret = (type == that.type)? 0 :((type<that.type)?-1:1);
    if (ret != 0) return ret;
    ret = scheme != null ? scheme.compareTo(that.scheme) : (that.scheme != null ? -1 : 0);
    if (ret != 0) return ret;
    ret = WritableComparator.compareBytes(auth, that.auth); 
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret;
    ret = (int)type;
    result = 37*result + ret;
    ret = scheme != null ? scheme.hashCode() : 0;
    result = 37*result + ret;
    ret = auth != null ? Arrays.toString(auth).hashCode() : 0; 
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeInt(type);
    UTF8.writeString(out, scheme);
    out.writeInt(auth != null ? auth.length : 0); 
    if (auth != null) out.write(auth); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    type = in.readInt(); 
    scheme = UTF8.readString(in);
    int size = in.readInt(); 
    if (size > 0) {
      auth = new byte[size]; 
      in.readFully(auth, 0, size); 
    } else 
      auth = null; 
  }

  public static AuthPacket read(DataInput in) throws IOException {
    AuthPacket result = new AuthPacket();
    result.readFields(in);
    return result;
  }
}
