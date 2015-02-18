package org.javenstudio.raptor.paxos.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Properties;

import org.javenstudio.raptor.paxos.server.quorum.QuorumPeerConfig;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeerConfig.ConfigException;
import org.javenstudio.raptor.util.InputSource;

/**
 * Server configuration storage.
 *
 * We use this instead of Properties as it's typed.
 *
 */
public class ServerConfig {
	public static final int CLIENT_PORT = 10021;
    ////
    //// If you update the configuration parameters be sure
    //// to update the "conf" 4letter word
    ////
    protected InetSocketAddress clientPortAddress = new InetSocketAddress(ServerConfig.CLIENT_PORT);
    protected String dataDir = System.getProperty("paxos.data.dir");
    protected String dataLogDir = dataDir;
    protected int tickTime = PaxosServer.DEFAULT_TICK_TIME;
    protected int maxClientCnxns;
    /** defaults to -1 if not set explicitly */
    protected int minSessionTimeout = -1;
    /** defaults to -1 if not set explicitly */
    protected int maxSessionTimeout = -1;

    /**
     * Parse arguments for server configuration
     * @param args clientPort dataDir and optional tickTime
     * @return ServerConfig configured wrt arguments
     * @throws IllegalArgumentException on invalid usage
     */
    public void parse(String[] args) {
        if (args.length < 2 || args.length > 4) {
            throw new IllegalArgumentException("Invalid args:"
                    + Arrays.toString(args));
        }

        clientPortAddress = new InetSocketAddress(Integer.parseInt(args[0]));
        dataDir = args[1];
        dataLogDir = dataDir;
        if (args.length == 3) {
            tickTime = Integer.parseInt(args[2]);
        }
        if (args.length == 4) {
            maxClientCnxns = Integer.parseInt(args[3]);
        }
    }

    /**
     * Parse a Paxos configuration file
     * @param path the patch of the configuration file
     * @return ServerConfig configured wrt arguments
     * @throws ConfigException error processing configuration
     */
    public void parse(String path) throws IOException, ConfigException {
        QuorumPeerConfig config = new QuorumPeerConfig();
        config.parse(path);

        // let qpconfig parse the file and then pull the stuff we are
        // interested in
        readFrom(config);
    }

    public void parse(InputSource source) throws IOException, ConfigException {
        QuorumPeerConfig config = new QuorumPeerConfig();
        config.parse(source);

        readFrom(config);
    }
    
    public void parse(InputStream input) throws IOException, ConfigException {
        QuorumPeerConfig config = new QuorumPeerConfig();
        config.parse(input);

        readFrom(config);
    }
    
    public void parse(Properties props) throws IOException, ConfigException {
        QuorumPeerConfig config = new QuorumPeerConfig();
        config.parse(props);

        readFrom(config);
    }
    
    //public void parse(Configuration conf) throws ConfigException {
    //    QuorumPeerConfig config = new QuorumPeerConfig();
    //    config.parse(conf);
    //
    //    // let qpconfig parse the file and then pull the stuff we are
    //    // interested in
    //    readFrom(config);
    //}

    /**
     * Read attributes from a QuorumPeerConfig.
     * @param config
     */
    public void readFrom(QuorumPeerConfig config) {
      clientPortAddress = config.getClientPortAddress();
      dataDir = config.getDataDir();
      dataLogDir = config.getDataLogDir();
      tickTime = config.getTickTime();
      maxClientCnxns = config.getMaxClientCnxns();
      minSessionTimeout = config.getMinSessionTimeout();
      maxSessionTimeout = config.getMaxSessionTimeout();
    }

    public InetSocketAddress getClientPortAddress() {
        return clientPortAddress;
    }
    
    public String getDataDir() { return dataDir; }
    public String getDataLogDir() { return dataLogDir; }
    public int getTickTime() { return tickTime; }
    public int getMaxClientCnxns() { return maxClientCnxns; }
    /** minimum session timeout in milliseconds, -1 if unset */
    public int getMinSessionTimeout() { return minSessionTimeout; }
    /** maximum session timeout in milliseconds, -1 if unset */
    public int getMaxSessionTimeout() { return maxSessionTimeout; }
    
}

