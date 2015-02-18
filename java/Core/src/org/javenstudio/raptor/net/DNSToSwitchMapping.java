package org.javenstudio.raptor.net;

import java.util.List;

/**
 * An interface that should be implemented to allow pluggable 
 * DNS-name/IP-address to RackID resolvers.
 *
 */
public interface DNSToSwitchMapping {
  /**
   * Resolves a list of DNS-names/IP-addresses and returns back a list of
   * switch information (network paths). One-to-one correspondence must be 
   * maintained between the elements in the lists. 
   * Consider an element in the argument list - x.y.com. The switch information
   * that is returned must be a network path of the form /foo/rack, 
   * where / is the root, and 'foo' is the switch where 'rack' is connected.
   * Note the hostname/ip-address is not part of the returned path.
   * The network topology of the cluster would determine the number of
   * components in the network path.
   * @param names
   * @return list of resolved network paths
   */
  public List<String> resolve(List<String> names);
}

