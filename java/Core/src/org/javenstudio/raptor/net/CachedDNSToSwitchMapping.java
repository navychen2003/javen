package org.javenstudio.raptor.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cached implementation of DNSToSwitchMapping that takes an
 * raw DNSToSwitchMapping and stores the resolved network location in 
 * a cache. The following calls to a resolved network location
 * will get its location from the cache. 
 *
 */
public class CachedDNSToSwitchMapping implements DNSToSwitchMapping {
  private Map<String, String> cache = new ConcurrentHashMap<String, String>();
  protected DNSToSwitchMapping rawMapping;
  
  public CachedDNSToSwitchMapping(DNSToSwitchMapping rawMapping) {
    this.rawMapping = rawMapping;
  }
  
  public List<String> resolve(List<String> names) {
    // normalize all input names to be in the form of IP addresses
    names = NetUtils.normalizeHostNames(names);
    
    List <String> result = new ArrayList<String>(names.size());
    if (names.isEmpty()) {
      return result;
    }


    // find out all names without cached resolved location
    List<String> unCachedHosts = new ArrayList<String>(names.size());
    for (String name : names) {
      if (cache.get(name) == null) {
        unCachedHosts.add(name);
      } 
    }
    
    // Resolve those names
    List<String> rNames = rawMapping.resolve(unCachedHosts);
    
    // Cache the result
    if (rNames != null) {
      for (int i=0; i<unCachedHosts.size(); i++) {
        cache.put(unCachedHosts.get(i), rNames.get(i));
      }
    }
    
    // Construct the result
    for (String name : names) {
      //now everything is in the cache
      String networkLocation = cache.get(name);
      if (networkLocation != null) {
        result.add(networkLocation);
      } else { //resolve all or nothing
        return null;
      }
    }
    return result;
  }
}

