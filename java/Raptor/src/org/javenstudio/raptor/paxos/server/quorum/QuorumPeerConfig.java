package org.javenstudio.raptor.paxos.server.quorum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.paxos.server.PaxosServer;
import org.javenstudio.raptor.paxos.server.ServerConfig;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeer.LearnerType;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeer.QuorumServer;
import org.javenstudio.raptor.paxos.server.quorum.flexible.QuorumHierarchical;
import org.javenstudio.raptor.paxos.server.quorum.flexible.QuorumMaj;
import org.javenstudio.raptor.paxos.server.quorum.flexible.QuorumVerifier;
import org.javenstudio.raptor.paxos.server.util.ConfigProps;
import org.javenstudio.raptor.util.InputSource;
import org.javenstudio.util.StringUtils;

public class QuorumPeerConfig {
    private static final Logger LOG = Logger.getLogger(QuorumPeerConfig.class);

    protected InetSocketAddress clientPortAddress = 
    		new InetSocketAddress(ServerConfig.CLIENT_PORT);
    
    protected String dataDir = System.getProperty("paxos.data.dir");
    protected String dataLogDir = dataDir;
    
    protected int tickTime = PaxosServer.DEFAULT_TICK_TIME;
    protected int maxClientCnxns = 10;
    
    /** defaults to -1 if not set explicitly */
    protected int minSessionTimeout = -1;
    /** defaults to -1 if not set explicitly */
    protected int maxSessionTimeout = -1;

    protected int initLimit;
    protected int syncLimit;
    protected int electionAlg = 3;
    protected int electionPort = 2182;
    
    protected final HashMap<Long,QuorumServer> servers =
        new HashMap<Long, QuorumServer>();
    protected final HashMap<Long,QuorumServer> observers =
        new HashMap<Long, QuorumServer>();

    protected long serverId;
    protected int numGroups = 0;
    
    protected HashMap<Long, Long> serverWeight = new HashMap<Long, Long>();
    protected HashMap<Long, Long> serverGroup = new HashMap<Long, Long>();
    
    protected QuorumVerifier quorumVerifier;
    protected LearnerType peerType = LearnerType.PARTICIPANT;

    @SuppressWarnings("serial")
    public static class ConfigException extends Exception {
        public ConfigException(String msg) {
            super(msg);
        }
        public ConfigException(String msg, Exception e) {
            super(msg, e);
        }
    }

    /**
     * Parse a Paxos configuration file
     * @param path the patch of the configuration file
     * @throws ConfigException error processing configuration
     */
    public void parse(String path) throws IOException, ConfigException {
        File configFile = new File(path);

        if (LOG.isDebugEnabled())
        	LOG.debug("Reading configuration from: " + configFile);

        try {
            if (!configFile.exists()) {
                throw new IllegalArgumentException(configFile.toString()
                        + " file is missing");
            }

            FileInputStream in = new FileInputStream(configFile);
            parse(in);

        //} catch (IOException e) {
        //    throw new ConfigException("Error parsing " + path, e);
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Error parsing " + path, e);
        }
    }

    //public void parse(Configuration conf) throws ConfigException {
    //    String path = conf.get("paxos.conf.file", "paxos.conf"); 
    //
    //    try {
    //        File file = new File(path);
    //        if (file.exists()) {
    //            parse(path); 
    //            return; 
    //        }
    //
    //        InputStream in = conf.getConfResourceAsInputStream(path); 
    //        parse0(in); 
    //
    //    } catch (IOException e) {
    //        throw new ConfigException("Error processing " + path, e);
    //    } catch (IllegalArgumentException e) {
    //        throw new ConfigException("Error processing " + path, e);
    //    }
    //}

    public void parse(InputSource source) throws IOException, ConfigException { 
    	parse(source.openStream());
    }
    
    public void parse(InputStream in) throws IOException, ConfigException {
        Properties cfg = new Properties();
        try {
            cfg.load(in);
        } finally {
            in.close();
        }

        parse(cfg);
    }

    /**
     * Parse config from a Properties.
     * @param props Properties to parse from.
     * @throws IOException
     * @throws ConfigException
     */
    public synchronized void parse(Properties properties) 
    		throws IOException, ConfigException {
    	ConfigProps props = new ConfigProps(properties);
        String[] names = props.getNames();

        clientPort = 0;
        clientAddress = null;
        
        for (int i=0; names != null && i < names.length; i++) {
            String key = names[i];
            String value = props.get(key);
            
            parseOne(StringUtils.trim(key), StringUtils.trim(value));
        }
        
        parseDone();
    }
    
    private int clientPort = 0;
    private String clientAddress = null;
    
    private void parseOne(String key, String value) 
    		throws IOException, ConfigException {
    	if (key != null && value != null) {
    		if (LOG.isDebugEnabled())
    			LOG.debug("parseConfig: " + key + "=" + value);
    		
            if (key.equals("dataDir")) {
                dataDir = value;
            } else if (key.equals("dataLogDir")) {
                dataLogDir = value;
            } else if (key.equals("clientPort")) {
                clientPort = Integer.parseInt(value);
            } else if (key.equals("clientPortAddress")) {
            	clientAddress = value.trim();
            } else if (key.equals("tickTime")) {
                tickTime = Integer.parseInt(value);
            } else if (key.equals("maxClientCnxns")) {
                maxClientCnxns = Integer.parseInt(value);
            } else if (key.equals("minSessionTimeout")) {
                minSessionTimeout = Integer.parseInt(value);
            } else if (key.equals("maxSessionTimeout")) {
                maxSessionTimeout = Integer.parseInt(value);
            } else if (key.equals("initLimit")) {
                initLimit = Integer.parseInt(value);
            } else if (key.equals("syncLimit")) {
                syncLimit = Integer.parseInt(value);
            } else if (key.equals("electionAlg")) {
                electionAlg = Integer.parseInt(value);
            } else if (key.equals("peerType")) {
                if (value.toLowerCase().equals("observer")) {
                    peerType = LearnerType.OBSERVER;
                } else if (value.toLowerCase().equals("participant")) {
                    peerType = LearnerType.PARTICIPANT;
                } else {
                    throw new ConfigException("Unrecognised peertype: " + value);
                }
            } else if (key.startsWith("server.")) {
                int dot = key.indexOf('.');
                long sid = Long.parseLong(key.substring(dot + 1));
                String parts[] = value.split(":");
                if ((parts.length != 2) && (parts.length != 3) && (parts.length !=4)) {
                    LOG.error(value
                       + " does not have the form host:port or host:port:port " +
                       " or host:port:port:type");
                }
                InetSocketAddress addr = new InetSocketAddress(parts[0],
                        Integer.parseInt(parts[1]));
                if (parts.length == 2) {
                    servers.put(Long.valueOf(sid), new QuorumServer(sid, addr));
                } else if (parts.length == 3) {
                    InetSocketAddress electionAddr = new InetSocketAddress(
                            parts[0], Integer.parseInt(parts[2]));
                    servers.put(Long.valueOf(sid), new QuorumServer(sid, addr,
                            electionAddr));
                } else if (parts.length == 4) {
                    InetSocketAddress electionAddr = new InetSocketAddress(
                            parts[0], Integer.parseInt(parts[2]));
                    LearnerType type = LearnerType.PARTICIPANT;
                    if (parts[3].toLowerCase().equals("observer")) {
                        type = LearnerType.OBSERVER;
                        observers.put(Long.valueOf(sid), new QuorumServer(sid, addr,
                                electionAddr,type));
                    } else if (parts[3].toLowerCase().equals("participant")) {
                        type = LearnerType.PARTICIPANT;
                        servers.put(Long.valueOf(sid), new QuorumServer(sid, addr,
                                electionAddr,type));
                    } else {
                        throw new ConfigException("Unrecognised peertype: " + value);
                    }
                }
            } else if (key.startsWith("group")) {
                int dot = key.indexOf('.');
                long gid = Long.parseLong(key.substring(dot + 1));

                numGroups++;

                String parts[] = value.split(":");
                for(String s : parts){
                    long sid = Long.parseLong(s);
                    if(serverGroup.containsKey(sid))
                        throw new ConfigException("Server " + sid + "is in multiple groups");
                    else
                        serverGroup.put(sid, gid);
                }

            } else if(key.startsWith("weight")) {
                int dot = key.indexOf('.');
                long sid = Long.parseLong(key.substring(dot + 1));
                serverWeight.put(sid, Long.parseLong(value));
            } else {
                System.setProperty("paxos." + key, value);
            }
        }
    }
    
    private void parseDone() throws IOException, ConfigException {
        if (dataDir == null) 
        	dataDir = System.getProperty("paxos.data.dir");
        if (dataDir == null) 
            throw new IllegalArgumentException("dataDir is not set");
        
        if (dataLogDir == null) {
            dataLogDir = dataDir;
        } else {
            if (!new File(dataLogDir).isDirectory()) {
                throw new IllegalArgumentException("dataLogDir " + dataLogDir
                        + " is missing.");
            }
        }
        if (clientPort == 0) {
            throw new IllegalArgumentException("clientPort is not set");
        }
        if (clientAddress != null) {
            this.clientPortAddress = new InetSocketAddress(
                    InetAddress.getByName(clientAddress), clientPort);
        } else {
            this.clientPortAddress = new InetSocketAddress(clientPort);
        }

        if (tickTime == 0) {
            throw new IllegalArgumentException("tickTime is not set");
        }
        if (minSessionTimeout > maxSessionTimeout) {
            throw new IllegalArgumentException(
                    "minSessionTimeout must not be larger than maxSessionTimeout");
        }
        if (servers.size() > 1) {
            if (initLimit == 0) {
                throw new IllegalArgumentException("initLimit is not set");
            }
            if (syncLimit == 0) {
                throw new IllegalArgumentException("syncLimit is not set");
            }
            /*
             * If using FLE, then every server requires a separate election
             * port.
             */
            if (electionAlg != 0) {
                for (QuorumServer s : servers.values()) {
                    if (s.electionAddr == null)
                        throw new IllegalArgumentException(
                                "Missing election port for server: " + s.id);
                }
            }

            /*
             * Default of quorum config is majority
             */
            if(serverGroup.size() > 0){
                if(servers.size() != serverGroup.size())
                    throw new ConfigException("Every server must be in exactly one group");
                /*
                 * The deafult weight of a server is 1
                 */
                for(QuorumServer s : servers.values()){
                    if(!serverWeight.containsKey(s.id))
                        serverWeight.put(s.id, (long) 1);
                }

                /*
                 * Set the quorumVerifier to be QuorumHierarchical
                 */
                quorumVerifier = new QuorumHierarchical(numGroups,
                        serverWeight, serverGroup);
            } else {
                /*
                 * The default QuorumVerifier is QuorumMaj
                 */

                LOG.info("Defaulting to majority quorums");
                quorumVerifier = new QuorumMaj(servers.size());
            }

            // Now add observers to servers, once the quorums have been
            // figured out
            servers.putAll(observers);

            File myIdFile = new File(dataDir, "myid");
            if (!myIdFile.exists()) {
                throw new IllegalArgumentException(myIdFile.toString()
                        + " file is missing");
            }
            BufferedReader br = new BufferedReader(new FileReader(myIdFile));
            String myIdString;
            try {
                myIdString = br.readLine();
            } finally {
                br.close();
            }
            try {
                serverId = Long.parseLong(myIdString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("serverid " + myIdString
                        + " is not a number");
            }
        }
    }

    public InetSocketAddress getClientPortAddress() { return clientPortAddress; }
    public int getClientPort() { return clientPort; }
    
    public String getDataDir() { return dataDir; }
    public String getDataLogDir() { return dataLogDir; }
    
    public int getTickTime() { return tickTime; }
    public int getMaxClientCnxns() { return maxClientCnxns; }
    public int getMinSessionTimeout() { return minSessionTimeout; }
    public int getMaxSessionTimeout() { return maxSessionTimeout; }

    public int getInitLimit() { return initLimit; }
    public int getSyncLimit() { return syncLimit; }
    public int getElectionAlg() { return electionAlg; }
    public int getElectionPort() { return electionPort; }    
    
    public QuorumVerifier getQuorumVerifier() {   
        return quorumVerifier;
    }

    public Map<Long,QuorumServer> getServers() {
        return Collections.unmodifiableMap(servers);
    }

    public long getServerId() { return serverId; }

    public boolean isDistributed() { return servers.size() > 1; }

    public LearnerType getPeerType() {
        return peerType;
    }
}

