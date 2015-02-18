package org.javenstudio.raptor.metrics.spi;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;


/**
 * Static utility methods
 */
public class Util {
    
  /**
   * This class is not intended to be instantiated
   */
  private Util() {}
    
  /**
   * Parses a space and/or comma separated sequence of server specifications
   * of the form <i>hostname</i> or <i>hostname:port</i>.  If 
   * the specs string is null, defaults to localhost:defaultPort.
   * 
   * @return a list of InetSocketAddress objects.
   */
  public static List<InetSocketAddress> parse(String specs, int defaultPort) {
    List<InetSocketAddress> result = new ArrayList<InetSocketAddress>(1);
    if (specs == null) {
      result.add(new InetSocketAddress("localhost", defaultPort));
    }
    else {
      String[] specStrings = specs.split("[ ,]+");
      for (String specString : specStrings) {
        int colon = specString.indexOf(':');
        if (colon < 0 || colon == specString.length() - 1) {
          result.add(new InetSocketAddress(specString, defaultPort));
        } else {
          String hostname = specString.substring(0, colon);
          int port = Integer.parseInt(specString.substring(colon+1));
          result.add(new InetSocketAddress(hostname, port));
        }
      }
    }
    return result;
  }
    
}
