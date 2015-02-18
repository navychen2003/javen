package org.javenstudio.raptor.ipc;

import java.io.IOException;


/**
 * Superclass of all protocols that use Hawk RPC.
 * Subclasses of this interface are also supposed to have
 * a static final long versionID field.
 * @author milindb
 */
public interface VersionedProtocol {
  
  /**
   * Return protocol version corresponding to protocol interface.
   * @param protocol The classname of the protocol interface
   * @param clientVersion The version of the protocol that the client speaks
   * @return the version that the server will speak
   */
  public long getProtocolVersion(String protocol, 
                                 long clientVersion) throws IOException;
}
