package org.javenstudio.raptor.dfs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.net.DNS;


public class DFSUtil {
  private static final Logger LOG = Logger.getLogger(DFSUtil.class);
  
  /**
   * Whether the pathname is valid.  Currently prohibits relative paths, 
   * and names which contain a ":" or "/" 
   */
  public static boolean isValidName(String src) {
      
    // Path must be absolute.
    if (!src.startsWith(Path.SEPARATOR)) {
      return false;
    }
      
    // Check for ".." "." ":" "/"
    StringTokenizer tokens = new StringTokenizer(src, Path.SEPARATOR);
    while(tokens.hasMoreTokens()) {
      String element = tokens.nextToken();
      if (element.equals("..") || 
          element.equals(".")  ||
          (element.indexOf(":") >= 0)  ||
          (element.indexOf("/") >= 0)) {
        return false;
      }
    }
    return true;
  }

  private static Object lockHost = new Object(); 
  private static String localHostName = null; 

  public static String getLocalHostName() {
    synchronized (lockHost) {
      if (localHostName == null) {
        try {
          InetAddress addr = InetAddress.getLocalHost();
          localHostName = addr.getHostName();
        } catch (Exception e) {
          LOG.warn("get hostname error: "+e); 
          localHostName = "localhost"; 
        }
      }
      return localHostName; 
    }
  }

  public static String getLocalNetworkAddress(Configuration conf) {
    try { 
      return getNetworkAddress(getLocalAddressPattern(conf)); 
    } catch (Exception e) { 
      LOG.warn("get local network address failed, use '127.0.0.1' as default: "+e); 
      return "127.0.0.1"; 
    } 
  }

  private static HashMap<String, String> localAddressMap = new HashMap<String, String>(); 
  private static String localPattern = null; 
  private static Object lockAddress = new Object(); 

  public static String getNetworkAddress(String pattern) throws IOException {
    synchronized (lockAddress) {
      String key = pattern != null ? pattern : ""; 
      String address = localAddressMap.get(key); 
      if (address != null) return address; 
      try {
        address = DNS.getNetworkAddress(pattern); 
        address = address != null ? address : "127.0.0.1"; 
        localAddressMap.put(key, address); 
        return address; 
      } catch (Exception e) {
        throw new IOException(e.toString()); 
      }
    }
  }

  private static String getLocalAddressPattern(Configuration conf) {
    if (conf == null) return null; 
    synchronized (lockAddress) {
      if (localPattern != null) return localPattern; 
      try {
        String nodename = getLocalHostName();
        localPattern = nodename;
        //ServerDefinition serverDef = ServerDefinition.get(conf);
        //NodeDefinition node = serverDef.getNode(nodename);
        //if (node == null) {
        //  nodename = "localhost";
        //  node = serverDef.getNode(nodename);
        //}
        //if (node != null) {
        //  String addr = node.getHostAddress();
        //  if (addr != null && addr.length() > 0)
        //    return addr;
        //  localPattern = node.getHostAddressPattern();
        //}
      } catch (Exception ex) {
        LOG.warn("get node address pattern error: "+ex);
        localPattern = null;
      }
      return localPattern; 
    }
  }
  
}

