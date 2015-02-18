package org.javenstudio.raptor.security.authorize;

import java.io.IOException;

import org.javenstudio.raptor.ipc.VersionedProtocol;


/**
 * Protocol which is used to refresh the authorization policy in use currently.
 */
public interface RefreshAuthorizationPolicyProtocol extends VersionedProtocol {
  
  /**
   * Version 1: Initial version
   */
  public static final long versionID = 1L;

  /**
   * Refresh the service-level authorization policy in-effect.
   * @throws IOException
   */
  void refreshServiceAcl() throws IOException;
}
