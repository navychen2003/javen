package org.javenstudio.raptor.dfs;

import org.javenstudio.raptor.dfs.protocol.ClientDatanodeProtocol;
import org.javenstudio.raptor.dfs.protocol.ClientProtocol;
import org.javenstudio.raptor.dfs.server.protocol.DatanodeProtocol;
import org.javenstudio.raptor.dfs.server.protocol.InterDatanodeProtocol;
import org.javenstudio.raptor.dfs.server.protocol.NamenodeProtocol;
import org.javenstudio.raptor.security.authorize.PolicyProvider;
import org.javenstudio.raptor.security.authorize.RefreshAuthorizationPolicyProtocol;
import org.javenstudio.raptor.security.authorize.Service;

/**
 * {@link PolicyProvider} for DFS protocols.
 */
public class DFSPolicyProvider extends PolicyProvider {
  private static final Service[] hdfsServices =
    new Service[] {
    new Service("security.client.protocol.acl", ClientProtocol.class),
    new Service("security.client.datanode.protocol.acl", 
                ClientDatanodeProtocol.class),
    new Service("security.datanode.protocol.acl", DatanodeProtocol.class),
    new Service("security.inter.datanode.protocol.acl", 
                InterDatanodeProtocol.class),
    new Service("security.namenode.protocol.acl", NamenodeProtocol.class),
    new Service("security.refresh.policy.protocol.acl", 
                RefreshAuthorizationPolicyProtocol.class),
  };
  
  @Override
  public Service[] getServices() {
    return hdfsServices;
  }
}

