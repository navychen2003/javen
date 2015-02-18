package org.javenstudio.raptor.bigdb.paxos;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.net.DNS;
import org.javenstudio.raptor.util.StringUtils;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.paxos.server.ServerConfig;
import org.javenstudio.raptor.paxos.server.PaxosServerMain;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeerConfig;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeerMain;
import org.javenstudio.raptor.paxos.server.util.ConfigProps;

/**
 * BigDB's version of Paxos's QuorumPeer. When BigDB is set to manage
 * Paxos, this class is used to start up QuorumPeer instances. By doing
 * things in here rather than directly calling to Paxos, we have more
 * control over the process. Currently, this class allows us to parse the
 * paxos.cfg and inject variables from BigDB's site.xml configuration in.
 */
public class DBQuorumPeer {
  private static final Logger LOG = Logger.getLogger(DBQuorumPeer.class);

  private static final String VARIABLE_START = "${";
  private static final int VARIABLE_START_LENGTH = VARIABLE_START.length();
  private static final String VARIABLE_END = "}";
  private static final int VARIABLE_END_LENGTH = VARIABLE_END.length();

  private static final String PAXOS_CFG_PROPERTY = "bigdb.paxos.property.";
  private static final int PAXOS_CFG_PROPERTY_SIZE = PAXOS_CFG_PROPERTY.length();
  private static final String PAXOS_CLIENT_PORT_KEY = PAXOS_CFG_PROPERTY
      + "clientPort";

  /**
   * Parse Paxos configuration from BigDB XML config and run a QuorumPeer.
   * @param args String[] of command line arguments. Not used.
   */
  public static void main(String[] args) {
    Configuration conf = ConfigurationFactory.get();
    try {
      Properties paxosProperties = makePaxosProps(conf);
      writeMyID(paxosProperties);
      QuorumPeerConfig config = new QuorumPeerConfig();
      config.parse(paxosProperties);
      runPaxosServer(config);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  private static void runPaxosServer(QuorumPeerConfig config) throws UnknownHostException, IOException {
    if (config.isDistributed()) {
      QuorumPeerMain qp = new QuorumPeerMain();
      qp.runFromConfig(config);
    } else {
      PaxosServerMain main = new PaxosServerMain();
      ServerConfig serverConfig = new ServerConfig();
      serverConfig.readFrom(config);
      main.runFromConfig(serverConfig);
    }
  }

  private static boolean addressIsLocalHost(String address) {
    return address.equals("localhost") || address.equals("127.0.0.1");
  }

  private static void writeMyID(Properties properties) throws IOException {
    long myId = -1;

    Configuration conf = ConfigurationFactory.get();
    String myAddress = DNS.getDefaultHost(
        conf.get("bigdb.paxos.dns.interface","default"),
        conf.get("bigdb.paxos.dns.nameserver","default"));

    List<String> ips = new ArrayList<String>();

    // Add what could be the best (configured) match
    ips.add(myAddress.contains(".") ?
        myAddress :
        StringUtils.simpleHostname(myAddress));

    // For all nics get all hostnames and IPs
    Enumeration<?> nics = NetworkInterface.getNetworkInterfaces();
    while(nics.hasMoreElements()) {
      Enumeration<?> rawAdrs =
          ((NetworkInterface)nics.nextElement()).getInetAddresses();
      while(rawAdrs.hasMoreElements()) {
        InetAddress inet = (InetAddress) rawAdrs.nextElement();
        ips.add(StringUtils.simpleHostname(inet.getHostName()));
        ips.add(inet.getHostAddress());
      }
    }

    for (Entry<Object, Object> entry : properties.entrySet()) {
      String key = entry.getKey().toString().trim();
      String value = entry.getValue().toString().trim();
      if (key.startsWith("server.")) {
        int dot = key.indexOf('.');
        long id = Long.parseLong(key.substring(dot + 1));
        String[] parts = value.split(":");
        String address = parts[0];
        if (addressIsLocalHost(address) || ips.contains(address)) {
          myId = id;
          break;
        }
      }
    }

    if (myId == -1) {
      throw new IOException("Could not find my address: " + myAddress +
                            " in list of Paxos quorum servers");
    }

    String dataDirStr = properties.get("dataDir").toString().trim();
    File dataDir = new File(dataDirStr);
    if (!dataDir.isDirectory()) {
      if (!dataDir.mkdirs()) {
        throw new IOException("Unable to create data dir " + dataDir);
      }
    }

    File myIdFile = new File(dataDir, "myid");
    PrintWriter w = new PrintWriter(myIdFile);
    w.println(myId);
    w.close();
  }

  /**
   * Make a Properties object holding Paxos config equivalent to paxos.cfg.
   * If there is a paxos.cfg in the classpath, simply read it in. Otherwise parse
   * the corresponding config options from the BigDB XML configs and generate
   * the appropriate Paxos properties.
   * @param conf Configuration to read from.
   * @return Properties holding mappings representing Paxos paxos.cfg file.
   */
  public static Properties makePaxosProps(Configuration conf) {
    // First check if there is a paxos.cfg in the CLASSPATH. If so, simply read
    // it and grab its configuration properties.
    ClassLoader cl = DBQuorumPeer.class.getClassLoader();
    final InputStream inputStream =
      cl.getResourceAsStream(DBConstants.PAXOS_CONFIG_NAME);
    if (inputStream != null) {
      try {
        return parsePaxosCfg(conf, inputStream);
      } catch (IOException e) {
    	if (LOG.isWarnEnabled()) {
          LOG.warn("Cannot read " + DBConstants.PAXOS_CONFIG_NAME +
                 ", loading from XML files", e);
    	}
      }
    } else { 
      if (LOG.isWarnEnabled())
    	LOG.warn("Cannot load " + DBConstants.PAXOS_CONFIG_NAME);
    }

    // Otherwise, use the configuration options from BigDB's XML files.
    Properties paxosProperties = new Properties();

    // Directly map all of the bigdb.paxos.property.KEY properties.
    for (Entry<String, String> entry : conf) {
      String key = entry.getKey();
      if (key.startsWith(PAXOS_CFG_PROPERTY)) {
        String zkKey = key.substring(PAXOS_CFG_PROPERTY_SIZE);
        String value = entry.getValue();
        // If the value has variables substitutions, need to do a get.
        if (value.contains(VARIABLE_START)) {
          value = conf.get(key);
        }
        paxosProperties.put(zkKey, value);
      }
    }

    // If clientPort is not set, assign the default
    if (paxosProperties.getProperty(PAXOS_CLIENT_PORT_KEY) == null) {
      paxosProperties.put(PAXOS_CLIENT_PORT_KEY,
                       DBConstants.DEFAULT_PAXOS_CLIENT_PORT);
    }

    // Create the server.X properties.
    int peerPort = conf.getInt("bigdb.paxos.peerport", 10028);
    int leaderPort = conf.getInt("bigdb.paxos.leaderport", 10038);

    final String[] serverHosts = conf.getStrings(DBConstants.PAXOS_QUORUM,
                                                 "localhost");
    for (int i = 0; i < serverHosts.length; ++i) {
      String serverHost = serverHosts[i];
      String address = serverHost + ":" + peerPort + ":" + leaderPort;
      String key = "server." + i;
      paxosProperties.put(key, address);
    }

    return paxosProperties;
  }
  
  /**
   * Return the Paxos Quorum servers string given zk properties returned by 
   * makePaxosProps
   * @param properties
   * @return
   */
  public static String getPaxosQuorumServersString(Properties properties) {
    String clientPort = null;
    List<String> servers = new ArrayList<String>();

    String propHost = System.getProperty("paxos.server.host");
    String propPort = System.getProperty("paxos.server.port");
    
    if (propHost != null && propHost.length() > 0) {
      if (LOG.isDebugEnabled())
        LOG.debug("set paxos host property: " + propHost);
      properties.setProperty("server.0", propHost);
    }
    if (propPort != null && propPort.length() > 0) {
      if (LOG.isDebugEnabled())
        LOG.debug("set paxos port property: " + propPort);
      properties.setProperty("clientPort", propPort);
    }
    
    // The clientPort option may come after the server.X hosts, so we need to
    // grab everything and then create the final host:port comma separated list.
    boolean anyValid = false;
    for (Entry<Object,Object> property : properties.entrySet()) {
      String key = property.getKey().toString().trim();
      String value = property.getValue().toString().trim();
      if (key.equals("clientPort")) {
        clientPort = value;
      }
      else if (key.startsWith("server.")) {
    	String host = value;
    	int pos = value.indexOf(':');
    	if (pos >= 0)
          host = value.substring(0, pos);
        servers.add(host);
        try {
          //noinspection ResultOfMethodCallIgnored
          InetAddress.getByName(host);
          anyValid = true;
        } catch (UnknownHostException e) {
          if (LOG.isWarnEnabled())
            LOG.warn(StringUtils.stringifyException(e));
        }
      }
    }

    if (!anyValid) {
      if (LOG.isWarnEnabled())
        LOG.warn("no valid quorum servers found in " + DBConstants.PAXOS_CONFIG_NAME);
      servers.add("127.0.0.1");
      //return null;
    }

    if (clientPort == null) {
      if (LOG.isErrorEnabled())
        LOG.error("no clientPort found in " + DBConstants.PAXOS_CONFIG_NAME);
      return null;
    }

    if (servers.isEmpty()) {
      if (LOG.isFatalEnabled()) {
        LOG.fatal("No server.X lines found in conf/paxos.cfg. BigDB must have a " +
                "Paxos cluster configured for its operation.");
      }
      return null;
    }

    StringBuilder hostPortBuilder = new StringBuilder();
    for (int i = 0; i < servers.size(); ++i) {
      String host = servers.get(i);
      if (i > 0) {
        hostPortBuilder.append(',');
      }
      hostPortBuilder.append(host);
      hostPortBuilder.append(':');
      hostPortBuilder.append(clientPort);
    }

    return hostPortBuilder.toString();
  }

  /**
   * Parse Paxos's paxos.cfg, injecting BigDB Configuration variables in.
   * This method is used for testing so we can pass our own InputStream.
   * @param conf Configuration to use for injecting variables.
   * @param inputStream InputStream to read from.
   * @return Properties parsed from config stream with variables substituted.
   * @throws IOException if anything goes wrong parsing config
   */
  public static Properties parsePaxosCfg(Configuration conf,
      InputStream inputStream) throws IOException {
    Properties properties = new Properties();
    try {
      properties.load(inputStream);
    } catch (IOException e) {
      final String msg = "fail to read properties from "
        + DBConstants.PAXOS_CONFIG_NAME;
      LOG.fatal(msg);
      throw new IOException(msg, e);
    }
    
    ConfigProps props = new ConfigProps(properties);
    String[] names = props.getNames();
    for (int i=0; names != null && i < names.length; i++) { 
      String key = names[i];
      String value = props.get(key);
      key = StringUtils.trim(key);
      value = StringUtils.trim(value);
      StringBuilder newValue = new StringBuilder();
      int varStart = value.indexOf(VARIABLE_START);
      int varEnd = 0;
      while (varStart != -1) {
        varEnd = value.indexOf(VARIABLE_END, varStart);
        if (varEnd == -1) {
          String msg = "variable at " + varStart + " has no end marker";
          LOG.fatal(msg);
          throw new IOException(msg);
        }
        String variable = value.substring(varStart + VARIABLE_START_LENGTH, varEnd);

        String substituteValue = System.getProperty(variable);
        if (substituteValue == null) {
          substituteValue = conf.get(variable);
        }
        if (substituteValue == null) {
          String msg = "variable " + variable + " not set in system property "
                     + "or bigdb configs";
          LOG.fatal(msg);
          throw new IOException(msg);
        }

        newValue.append(substituteValue);

        varEnd += VARIABLE_END_LENGTH;
        varStart = value.indexOf(VARIABLE_START, varEnd);
      }
      // Special case for 'bigdb.cluster.distributed' property being 'true'
      if (key.startsWith("server.")) {
        if (conf.get(DBConstants.CLUSTER_DISTRIBUTED).equals(DBConstants.CLUSTER_IS_DISTRIBUTED)
            && value.startsWith("localhost")) {
          String msg = "The server in paxos.cfg cannot be set to localhost " +
              "in a fully-distributed setup because it won't be reachable. " +
              "See \"Getting Started\" for more information.";
          LOG.fatal(msg);
          throw new IOException(msg);
        }
      }
      newValue.append(value.substring(varEnd));
      
      if (LOG.isDebugEnabled())
    	  LOG.debug("setProperty: " + key + "=" + newValue);
      properties.setProperty(key, newValue.toString());
    }
    return properties;
  }
}

