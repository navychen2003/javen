package org.javenstudio.raptor.bigdb;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

import org.javenstudio.raptor.bigdb.regionserver.DBRegionServer;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.io.WritableComparable;


/**
 * DBServerInfo is meta info about an {@link DBRegionServer}.  It is the token
 * by which a master distingushes a particular regionserver from the rest.
 * It holds hostname, ports, regionserver startcode, and load.  Each server has
 * a <code>servername</code> where servername is made up of a concatenation of
 * hostname, port, and regionserver startcode.  This servername is used in
 * various places identifying this regionserver.  Its even used as part of
 * a pathname in the filesystem.  As part of the initialization,
 * master will pass the regionserver the address that it knows this regionserver
 * by.  In subsequent communications, the regionserver will pass a DBServerInfo
 * with the master-supplied address.
 */
public class DBServerInfo implements WritableComparable<DBServerInfo> {
  /*
   * This character is used as separator between server hostname and port and
   * its startcode. Servername is formatted as
   * <code>&lt;hostname> '{@ink #SERVERNAME_SEPARATOR"}' &lt;port> '{@ink #SERVERNAME_SEPARATOR"}' &lt;startcode></code>.
   */
  private static final String SERVERNAME_SEPARATOR = ",";

  private DBServerAddress serverAddress;
  private long startCode;
  private DBServerLoad load;
  private int infoPort;
  // Servername is made of hostname, port and startcode.
  private String serverName = null;
  // Hostname of the regionserver.
  private String hostname;
  private String cachedHostnamePort = null;

  public DBServerInfo() {
    this(new DBServerAddress(), 0, DBConstants.DEFAULT_REGIONSERVER_INFOPORT,
      "default name");
  }

  /**
   * Constructor that creates a DBServerInfo with a generated startcode and an
   * empty load.
   * @param serverAddress An {@link InetSocketAddress} encased in a {@link Writable}
   * @param infoPort Port the webui runs on.
   * @param hostname Server hostname.
   */
  public DBServerInfo(DBServerAddress serverAddress, final int infoPort,
      final String hostname) {
    this(serverAddress, System.currentTimeMillis(), infoPort, hostname);
  }

  public DBServerInfo(DBServerAddress serverAddress, long startCode,
      final int infoPort, String hostname) {
    this.serverAddress = serverAddress;
    this.startCode = startCode;
    this.load = new DBServerLoad();
    this.infoPort = infoPort;
    this.hostname = hostname;
  }

  /**
   * Copy-constructor
   * @param other
   */
  public DBServerInfo(DBServerInfo other) {
    this.serverAddress = new DBServerAddress(other.getServerAddress());
    this.startCode = other.getStartCode();
    this.load = other.getLoad();
    this.infoPort = other.getInfoPort();
    this.hostname = other.hostname;
  }

  public DBServerLoad getLoad() {
    return load;
  }

  public void setLoad(DBServerLoad load) {
    this.load = load;
  }

  public synchronized DBServerAddress getServerAddress() {
    return new DBServerAddress(serverAddress);
  }

  public synchronized void setServerAddress(DBServerAddress serverAddress) {
    this.serverAddress = serverAddress;
    this.serverName = null;
  }

  public synchronized long getStartCode() {
    return startCode;
  }

  public int getInfoPort() {
    return this.infoPort;
  }

  public String getHostname() {
    return this.hostname;
  }

  /**
   * @return The hostname and port concatenated with a ':' as separator.
   */
  public synchronized String getHostnamePort() {
    if (this.cachedHostnamePort == null) {
      this.cachedHostnamePort = getHostnamePort(this.hostname, this.serverAddress.getPort());
    }
    return this.cachedHostnamePort;
  }

  /**
   * @param hostname
   * @param port
   * @return The hostname and port concatenated with a ':' as separator.
   */
  public static String getHostnamePort(final String hostname, final int port) {
    return hostname + ":" + port;
  }

  /**
   * @return Server name made of the concatenation of hostname, port and
   * startcode formatted as <code>&lt;hostname> ',' &lt;port> ',' &lt;startcode></code>
   */
  public synchronized String getServerName() {
    if (this.serverName == null) {
      this.serverName = getServerName(this.hostname,
        this.serverAddress.getPort(), this.startCode);
    }
    return this.serverName;
  }

  public static synchronized String getServerName(final String hostAndPort,
      final long startcode) {
    int index = hostAndPort.indexOf(":");
    if (index <= 0) throw new IllegalArgumentException("Expected <hostname> ':' <port>");
    return getServerName(hostAndPort.substring(0, index),
      Integer.parseInt(hostAndPort.substring(index + 1)), startcode);
  }

  /**
   * @param address Server address
   * @param startCode Server startcode
   * @return Server name made of the concatenation of hostname, port and
   * startcode formatted as <code>&lt;hostname> ',' &lt;port> ',' &lt;startcode></code>
   */
  public static String getServerName(DBServerAddress address, long startCode) {
    return getServerName(address.getHostname(), address.getPort(), startCode);
  }

  /*
   * @param hostName
   * @param port
   * @param startCode
   * @return Server name made of the concatenation of hostname, port and
   * startcode formatted as <code>&lt;hostname> ',' &lt;port> ',' &lt;startcode></code>
   */
  public static String getServerName(String hostName, int port, long startCode) {
    StringBuilder name = new StringBuilder(hostName);
    name.append(SERVERNAME_SEPARATOR);
    name.append(port);
    name.append(SERVERNAME_SEPARATOR);
    name.append(startCode);
    return name.toString();
  }

  /**
   * @return ServerName and load concatenated.
   * @see #getServerName()
   * @see #getLoad()
   */
  @Override
  public String toString() {
    return "serverName=" + getServerName() +
      ", load=(" + this.load.toString() + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return compareTo((DBServerInfo)obj) == 0;
  }

  @Override
  public int hashCode() {
    return this.getServerName().hashCode();
  }

  public void readFields(DataInput in) throws IOException {
    this.serverAddress.readFields(in);
    this.startCode = in.readLong();
    this.load.readFields(in);
    this.infoPort = in.readInt();
    this.hostname = in.readUTF();
  }

  public void write(DataOutput out) throws IOException {
    this.serverAddress.write(out);
    out.writeLong(this.startCode);
    this.load.write(out);
    out.writeInt(this.infoPort);
    out.writeUTF(hostname);
  }

  public int compareTo(DBServerInfo o) {
    return this.getServerName().compareTo(o.getServerName());
  }

  /**
   * Utility method that does a find of a servername or a hostandport combination
   * in the passed Set.
   * @param servers Set of server names
   * @param serverName Name to look for
   * @param hostAndPortOnly If <code>serverName</code> is a
   * <code>hostname ':' port</code>
   * or <code>hostname , port , startcode</code>.
   * @return True if <code>serverName</code> found in <code>servers</code>
   */
  public static boolean isServer(final Set<String> servers,
      final String serverName, final boolean hostAndPortOnly) {
    if (!hostAndPortOnly) return servers.contains(serverName);
    String serverNameColonReplaced =
      serverName.replaceFirst(":", SERVERNAME_SEPARATOR);
    for (String hostPortStartCode: servers) {
      int index = hostPortStartCode.lastIndexOf(SERVERNAME_SEPARATOR);
      String hostPortStrippedOfStartCode = hostPortStartCode.substring(0, index);
      if (hostPortStrippedOfStartCode.equals(serverNameColonReplaced)) return true;
    }
    return false;
  }
}

