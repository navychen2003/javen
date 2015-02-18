package org.javenstudio.raptor.security.authorize;

import java.security.Permission;

import org.javenstudio.raptor.ipc.VersionedProtocol;


/**
 * {@link Permission} to initiate a connection to a given service.
 */
public class ConnectionPermission extends Permission {

  private static final long serialVersionUID = 1L;
  private final Class<?> protocol;

  /**
   * {@link ConnectionPermission} for a given service.
   * @param protocol service to be accessed
   */
  public ConnectionPermission(Class<?> protocol) {
    super(protocol.getName());
    this.protocol = protocol;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ConnectionPermission) {
      return protocol == ((ConnectionPermission)obj).protocol;
    }
    return false;
  }

  @Override
  public String getActions() {
    return "ALLOW";
  }

  @Override
  public int hashCode() {
    return protocol.hashCode();
  }

  @Override
  public boolean implies(Permission permission) {
    if (permission instanceof ConnectionPermission) {
      ConnectionPermission that = (ConnectionPermission)permission;
      if (that.protocol.equals(VersionedProtocol.class)) {
        return true;
      }
      return this.protocol.equals(that.protocol);
    }
    return false;
  }

  public String toString() {
    return "ConnectionPermission(" + protocol.getName() + ")";
  }
}
