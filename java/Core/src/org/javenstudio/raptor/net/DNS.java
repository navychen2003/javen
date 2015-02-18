package org.javenstudio.raptor.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.ArrayList; 
import java.util.regex.Pattern;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.javenstudio.common.util.Logger;


/**
 * 
 * A class that provides direct and reverse lookup functionalities, allowing
 * the querying of specific network interfaces or nameservers.
 * 
 * 
 */
public class DNS {
  public static final Logger LOG = Logger.getLogger(DNS.class);

  private static InetAddress localAddress = null; 

  public static synchronized InetAddress getLocalHost() throws UnknownHostException {
    try {
      if (localAddress == null) 
        localAddress = InetAddress.getLocalHost(); 
    } catch (UnknownHostException e) {
      localAddress = InetAddress.getByName("localhost"); 
    }
    return localAddress; 
  }

  //public static String getMachineName() {
  //  return getMachineName(ConfigurationFactory.create()); 
  //}

  //public static String getMachineName(Configuration conf) {
  //  return ClusterHelper.getLocalHostName() + "/" + ClusterHelper.getLocalNetworkAddress(conf); 
  //}

  /**
   * Returns the hostname associated with the specified IP address by the
   * provided nameserver.
   * 
   * @param hostIp
   *            The address to reverse lookup
   * @param ns
   *            The host name of a reachable DNS server
   * @return The host name associated with the provided IP
   * @throws NamingException
   *             If a NamingException is encountered
   */
  public static String reverseDns(InetAddress hostIp, String ns)
    throws NamingException {
    //
    // Builds the reverse IP lookup form
    // This is formed by reversing the IP numbers and appending in-addr.arpa
    //
    String[] parts = hostIp.getHostAddress().split("\\.");
    String reverseIP = parts[3] + "." + parts[2] + "." + parts[1] + "."
      + parts[0] + ".in-addr.arpa";

    DirContext ictx = new InitialDirContext();
    Attributes attribute =
      ictx.getAttributes("dns://"               // Use "dns:///" if the default
                         + ((ns == null) ? "" : ns) + 
                         // nameserver is to be used
                         "/" + reverseIP, new String[] { "PTR" });
    ictx.close();
    
    return attribute.get("PTR").get().toString();
  }

  /**
   * Returns all the IPs associated with the provided interface, if any, in
   * textual form.
   * 
   * @param strInterface
   *            The name of the network interface to query (e.g. eth0)
   * @return A string vector of all the IPs associated with the provided
   *         interface
   * @throws UnknownHostException
   *             If an UnknownHostException is encountered in querying the
   *             default interface
   * 
   */
  @SuppressWarnings("rawtypes")
  public static String[] getIPs(String strInterface)
      throws UnknownHostException {
    try {
      NetworkInterface netIF = NetworkInterface.getByName(strInterface);
      if (netIF == null)
        return new String[] { InetAddress.getLocalHost()
                              .getHostAddress() };
      else {
        Vector<String> ips = new Vector<String>();
        Enumeration e = netIF.getInetAddresses();
        while (e.hasMoreElements())
          ips.add(((InetAddress) e.nextElement()).getHostAddress());
        return ips.toArray(new String[] {});
      }
    } catch (SocketException e) {
      return new String[] { InetAddress.getLocalHost().getHostAddress() };
    }
  }

  /**
   * Returns the first available IP address associated with the provided
   * network interface
   * 
   * @param strInterface
   *            The name of the network interface to query (e.g. eth0)
   * @return The IP address in text form
   * @throws UnknownHostException
   *             If one is encountered in querying the default interface
   */
  public static String getDefaultIP(String strInterface)
    throws UnknownHostException {
    String[] ips = getIPs(strInterface);
    return ips[0];
  }

  /**
   * Returns all the host names associated by the provided nameserver with the
   * address bound to the specified network interface
   * 
   * @param strInterface
   *            The name of the network interface to query (e.g. eth0)
   * @param nameserver
   *            The DNS host name
   * @return A string vector of all host names associated with the IPs tied to
   *         the specified interface
   * @throws UnknownHostException
   */
  public static String[] getHosts(String strInterface, String nameserver)
    throws UnknownHostException {
    String[] ips = getIPs(strInterface);
    Vector<String> hosts = new Vector<String>();
    for (int ctr = 0; ctr < ips.length; ctr++)
      try {
        hosts.add(reverseDns(InetAddress.getByName(ips[ctr]),
                             nameserver));
      } catch (Exception e) {
      }

    if (hosts.size() == 0)
      return new String[] { InetAddress.getLocalHost().getCanonicalHostName() };
    else
      return hosts.toArray(new String[] {});
  }

  /**
   * Returns all the host names associated by the default nameserver with the
   * address bound to the specified network interface
   * 
   * @param strInterface
   *            The name of the network interface to query (e.g. eth0)
   * @return The list of host names associated with IPs bound to the network
   *         interface
   * @throws UnknownHostException
   *             If one is encountered while querying the deault interface
   * 
   */
  public static String[] getHosts(String strInterface)
    throws UnknownHostException {
    return getHosts(strInterface, null);
  }

  /**
   * Returns the default (first) host name associated by the provided
   * nameserver with the address bound to the specified network interface
   * 
   * @param strInterface
   *            The name of the network interface to query (e.g. eth0)
   * @param nameserver
   *            The DNS host name
   * @return The default host names associated with IPs bound to the network
   *         interface
   * @throws UnknownHostException
   *             If one is encountered while querying the deault interface
   */
  public static String getDefaultHost(String strInterface, String nameserver)
    throws UnknownHostException {
    if (strInterface.equals("default")) 
      return InetAddress.getLocalHost().getCanonicalHostName();

    if (nameserver != null && nameserver.equals("default"))
      return getDefaultHost(strInterface);

    String[] hosts = getHosts(strInterface, nameserver);
    return hosts[0];
  }

  /**
   * Returns the default (first) host name associated by the default
   * nameserver with the address bound to the specified network interface
   * 
   * @param strInterface
   *            The name of the network interface to query (e.g. eth0)
   * @return The default host name associated with IPs bound to the network
   *         interface
   * @throws UnknownHostException
   *             If one is encountered while querying the deault interface
   */
  public static String getDefaultHost(String strInterface)
    throws UnknownHostException {
    return getDefaultHost(strInterface, null);
  }

  public static String getNetworkAddress() throws SocketException {
    return getNetworkAddress(null); 
  }

  public static String getNetworkAddress(String pattern) throws SocketException {
    String[] addrs = getNetworkAddresses(pattern); 
    return addrs != null && addrs.length > 0 ? addrs[0] : null; 
  }

  public static String[] getNetworkAddresses() throws SocketException {
    return getNetworkAddresses(null); 
  }

  public static String[] getNetworkAddresses(String regexstr) 
		  throws SocketException {
    try {
      ArrayList<String> ips = new ArrayList<String>(); 
      Pattern pattern = null; 
      if (regexstr != null && regexstr.length() > 0) {
        pattern = Pattern.compile(regexstr);
        if (LOG.isDebugEnabled()) 
          LOG.debug("getNetworkAddresses: compiled pattern: " + regexstr); 
      }

      Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
      while(netInterfaces.hasMoreElements()) {
        NetworkInterface ni = (NetworkInterface)netInterfaces.nextElement();
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        
        while (addresses.hasMoreElements()) {
          InetAddress ip = (InetAddress)addresses.nextElement();
          if (LOG.isDebugEnabled()) 
            LOG.debug("getNetworkAddresses: inetaddress: " + ip.toString()); 
          
          //if (ip.isSiteLocalAddress() && !ip.isLoopbackAddress()) 
          String address = ip.getHostAddress();
          if (!"127.0.0.1".equals(address) && address.indexOf(":") < 0) {
            if (pattern == null || pattern.matcher(address).find()) {
              ips.add(address); 
              if (LOG.isDebugEnabled()) 
            	LOG.debug("getNetworkAddresses:  found ip: "+address); 
            }
          }
        }
      }
      
      return ips.toArray(new String[ips.size()]);
    } catch (SocketException e) {
      throw e; 
    }
  }

}

