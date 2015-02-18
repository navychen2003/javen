package org.javenstudio.raptor.bigdb;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.io.WritableComparable;


/**
 * DBServerAddress is a "label" for a bigdb server made of host and port number.
 */
public class DBServerAddress implements WritableComparable<DBServerAddress> {
  private static final Logger LOG = Logger.getLogger(DBServerAddress.class);
  
  private InetSocketAddress address;
  String stringValue;

  public DBServerAddress() {
    this.address = null;
    this.stringValue = null;
  }

  /**
   * Construct an instance from an {@link InetSocketAddress}.
   * @param address InetSocketAddress of server
   */
  public DBServerAddress(InetSocketAddress address) {
    this.address = address;
    this.stringValue = address.getAddress().getHostAddress() + ":" +
      address.getPort();
    checkBindAddressCanBeResolved();
  }

  /**
   * @param hostAndPort Hostname and port formatted as <code>&lt;hostname> ':' &lt;port></code>
   */
  public DBServerAddress(String hostAndPort) {
    int colonIndex = hostAndPort.lastIndexOf(':');
    if (colonIndex < 0) {
      throw new IllegalArgumentException("Not a host:port pair: " + hostAndPort);
    }
    String host = hostAndPort.substring(0, colonIndex);
    int port = Integer.parseInt(hostAndPort.substring(colonIndex + 1));
    this.address = new InetSocketAddress(host, port);
    this.stringValue = hostAndPort;
    checkBindAddressCanBeResolved();
  }

  /**
   * @param bindAddress Hostname
   * @param port Port number
   */
  public DBServerAddress(String bindAddress, int port) {
    this.address = new InetSocketAddress(bindAddress, port);
    this.stringValue = bindAddress + ":" + port;
    checkBindAddressCanBeResolved();
  }

  /**
   * Copy-constructor.
   * @param other DBServerAddress to copy from
   */
  public DBServerAddress(DBServerAddress other) {
    String bindAddress = other.getBindAddress();
    int port = other.getPort();
    this.address = new InetSocketAddress(bindAddress, port);
    stringValue = other.stringValue;
    checkBindAddressCanBeResolved();
  }

  /** @return Bind address */
  public String getBindAddress() {
    final InetAddress addr = address.getAddress();
    if (addr != null) {
      return addr.getHostAddress();
    } else {
      LOG.error("Could not resolve the"
          + " DNS name of " + stringValue);
      return null;
    }
  }

  private void checkBindAddressCanBeResolved() {
    if (getBindAddress() == null) {
      throw new IllegalArgumentException("Could not resolve the"
          + " DNS name of " + stringValue);
    }
  }

  /** @return Port number */
  public int getPort() {
    return address.getPort();
  }

  /** @return Hostname */
  public String getHostname() {
    return address.getHostName();
  }

  /** @return The InetSocketAddress */
  public InetSocketAddress getInetSocketAddress() {
    return address;
  }

  /**
   * @return String formatted as <code>&lt;bind address> ':' &lt;port></code>
   */
  @Override
  public String toString() {
    return stringValue == null ? "" : stringValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (getClass() != o.getClass()) {
      return false;
    }
    return compareTo((DBServerAddress) o) == 0;
  }

  @Override
  public int hashCode() {
    int result = address.hashCode();
    result ^= stringValue.hashCode();
    return result;
  }

  //
  // Writable
  //

  public void readFields(DataInput in) throws IOException {
    String bindAddress = in.readUTF();
    int port = in.readInt();

    if (bindAddress == null || bindAddress.length() == 0) {
      address = null;
      stringValue = null;
    } else {
      address = new InetSocketAddress(bindAddress, port);
      stringValue = bindAddress + ":" + port;
      checkBindAddressCanBeResolved();
    }
  }

  public void write(DataOutput out) throws IOException {
    if (address == null) {
      out.writeUTF("");
      out.writeInt(0);
    } else {
      out.writeUTF(address.getAddress().getHostAddress());
      out.writeInt(address.getPort());
    }
  }

  //
  // Comparable
  //

  public int compareTo(DBServerAddress o) {
    // Addresses as Strings may not compare though address is for the one
    // server with only difference being that one address has hostname
    // resolved whereas other only has IP.
    if (address.equals(o.address)) return 0;
    return toString().compareTo(o.toString());
  }
}

