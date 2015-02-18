package org.javenstudio.raptor.ipc;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.io.Text;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.security.UnixUserGroupInformation;
import org.javenstudio.raptor.security.UserGroupInformation;


/**
 * The IPC connection header sent by the client to the server
 * on connection establishment.
 */
class ConnectionHeader implements Writable {
  public static final Logger LOG = Logger.getLogger(ConnectionHeader.class);
  
  private String protocol;
  private UserGroupInformation ugi = new UnixUserGroupInformation();
  
  public ConnectionHeader() {}
  
  /**
   * Create a new {@link ConnectionHeader} with the given <code>protocol</code>
   * and {@link UserGroupInformation}. 
   * @param protocol protocol used for communication between the IPC client
   *                 and the server
   * @param ugi {@link UserGroupInformation} of the client communicating with
   *            the server
   */
  public ConnectionHeader(String protocol, UserGroupInformation ugi) {
    this.protocol = protocol;
    this.ugi = ugi;
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    protocol = Text.readString(in);
    if (protocol.isEmpty()) {
      protocol = null;
    }
    
    boolean ugiPresent = in.readBoolean();
    if (ugiPresent) {
      ugi.readFields(in);
    } else {
      ugi = null;
    }
  }

  @Override
  public void write(DataOutput out) throws IOException {
    Text.writeString(out, (protocol == null) ? "" : protocol);
    if (ugi != null) {
      out.writeBoolean(true);
      ugi.write(out);
    } else {
      out.writeBoolean(false);
    }
  }

  public String getProtocol() {
    return protocol;
  }

  public UserGroupInformation getUgi() {
    return ugi;
  }

  public String toString() {
    return protocol + "-" + ugi;
  }
}
