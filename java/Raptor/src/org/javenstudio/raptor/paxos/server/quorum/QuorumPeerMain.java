package org.javenstudio.raptor.paxos.server.quorum;

import java.io.File;
import java.io.IOException;

import javax.management.JMException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.util.StringUtils;
import org.javenstudio.raptor.paxos.jmx.ManagedUtil;
import org.javenstudio.raptor.paxos.server.NIOServerCnxn;
import org.javenstudio.raptor.paxos.server.PaxosDatabase;
import org.javenstudio.raptor.paxos.server.PaxosServerMain;
import org.javenstudio.raptor.paxos.server.persistence.FileTxnSnapLog;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeerConfig.ConfigException;
import org.javenstudio.raptor.util.InputSource;

/**
 *
 * <h2>Configuration file</h2>
 *
 * When the main() method of this class is used to start the program, the first
 * argument is used as a path to the config file, which will be used to obtain
 * configuration information. This file is a Properties file, so keys and
 * values are separated by equals (=) and the key/value pairs are separated
 * by new lines. The following is a general summary of keys used in the
 * configuration file. For full details on this see the documentation in
 * docs/index.html
 * <ol>
 * <li>dataDir - The directory where the Paxos data is stored.</li>
 * <li>dataLogDir - The directory where the Paxos transaction log is stored.</li>
 * <li>clientPort - The port used to communicate with clients.</li>
 * <li>tickTime - The duration of a tick in milliseconds. This is the basic
 * unit of time in Paxos.</li>
 * <li>initLimit - The maximum number of ticks that a follower will wait to
 * initially synchronize with a leader.</li>
 * <li>syncLimit - The maximum number of ticks that a follower will wait for a
 * message (including heartbeats) from the leader.</li>
 * <li>server.<i>id</i> - This is the host:port[:port] that the server with the
 * given id will use for the quorum protocol.</li>
 * </ol>
 * In addition to the config file. There is a file in the data directory called
 * "myid" that contains the server id as an ASCII decimal value.
 *
 */
public class QuorumPeerMain {
    private static final Logger LOG = Logger.getLogger(QuorumPeerMain.class);

    private static final String USAGE = "Usage: QuorumPeerMain configfile";

    protected PaxosServerMain quorumServer = null;
    protected QuorumPeer quorumPeer = null;

    //private ServiceClient client = null;
    //private ServiceInfo serviceInfo = null; 
    private long startTime = 0; 

    /**
     */
    public static void doMain(InputSource source, String argv[]) {
    	QuorumPeerMain main = new QuorumPeerMain();
    	doMain(main, source, argv);
    }
    
    public static void doMain(QuorumPeerMain main, InputSource source, String argv[]) {
        try {
            main0(main, source, argv); 
        } catch (Throwable e) {
            LOG.error(StringUtils.stringifyException(e));
            System.exit(-1);
        }
    }

    /**
     * To start the replicated server specify the configuration file name on
     * the command line.
     * @param args path to the configfile
     */
    private static void main0(QuorumPeerMain main, 
    		InputSource source, String[] args) throws Exception {
        //ConfigurationFactory.setQuietMode(StartupOptions.getQuorumPeerOptions().hasCommandOption(args)); 
        StringUtils.startupShutdownMessage(QuorumPeerMain.class, args, LOG);
        //QuorumPeerMain main = new QuorumPeerMain();
        try {
            main.initializeAndRun(source, args);
        } catch (IllegalArgumentException e) {
            LOG.fatal("Invalid arguments, exiting abnormally", e);
            LOG.info(USAGE);
            System.err.println(USAGE);
            System.exit(2);
        } catch (ConfigException e) {
            LOG.fatal("Invalid config, exiting abnormally", e);
            System.err.println("Invalid config, exiting abnormally");
            System.exit(2);
        } catch (Exception e) {
            LOG.fatal("Unexpected exception, exiting abnormally", e);
            System.exit(1);
        }
        LOG.info("Exiting normally");
        System.exit(0);
    }

    protected void initializeAndRun(InputSource source, String[] args)
    		throws ConfigException, IOException {
        //Configuration conf = ConfigurationFactory.create();
        //QuorumPeerOptions options = StartupOptions.getQuorumPeerOptions(); 
        //options.parse(conf, args); 

        //if (options.hasOption(options.SINGLED)) {
        //  conf.setBoolean("paxos.singled.mode", true);
        //}

        QuorumPeerConfig config = new QuorumPeerConfig();
        if (source != null)
        	config.parse(source);
        
        //if (options.hasOption(options.CONF)) {
        //    String val = options.getOptionValue(options.CONF); 
        //    config.parse(val);
        //} else {
        //    config.parse(conf); 
        //}

        if (config.servers.size() > 0) {
            runFromConfig(config);
            
        } else {
            LOG.warn("Either no config or no quorum defined in config, running "
                    + " in standalone mode");
            
            // there is only server in the quorum -- run as standalone
            PaxosServerMain main = new PaxosServerMain();
            quorumServer = main;
            
            PaxosServerMain.doMain(main, source, args);
        }
    }

    //public void runFromConfig(QuorumPeerConfig config) throws IOException {
    //    runFromConfig(config, ConfigurationFactory.create()); 
    //}

    public void runFromConfig(QuorumPeerConfig config) throws IOException {
      try {
          ManagedUtil.registerLog4jMBeans();
      } catch (JMException e) {
          LOG.warn("Unable to register log4j JMX control", e);
      }
  
      LOG.info("Starting quorum peer");
      try {
          NIOServerCnxn.Factory cnxnFactory =
              new NIOServerCnxn.Factory(config.getClientPortAddress(),
                      config.getMaxClientCnxns());
  
          quorumPeer = new QuorumPeer();
          quorumPeer.setClientPortAddress(config.getClientPortAddress());
          quorumPeer.setTxnFactory(new FileTxnSnapLog(
                      new File(config.getDataLogDir()),
                      new File(config.getDataDir())));
          quorumPeer.setQuorumPeers(config.getServers());
          quorumPeer.setElectionType(config.getElectionAlg());
          quorumPeer.setMyid(config.getServerId());
          quorumPeer.setTickTime(config.getTickTime());
          quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout());
          quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout());
          quorumPeer.setInitLimit(config.getInitLimit());
          quorumPeer.setSyncLimit(config.getSyncLimit());
          quorumPeer.setQuorumVerifier(config.getQuorumVerifier());
          quorumPeer.setCnxnFactory(cnxnFactory);
          quorumPeer.setPaxosDatabase(new PaxosDatabase(quorumPeer.getTxnFactory()));
          quorumPeer.setLearnerType(config.getPeerType());
  
          quorumPeer.start();

          //InetSocketAddress serverAddress = quorumPeer.getQuorumAddress(); 
          //serviceInfo = ServiceInfo.create(QuorumPeerMain.class, serverAddress.getPort(), conf); 
          //serviceInfo.addServer(this.getClass().getSimpleName(), 
          //  serverAddress.getAddress().getHostAddress(), serverAddress.getPort(), quorumPeer.getQuorumSize(), 
          //  new String[]{QuorumPeer.class.getName()}); 

          startTime = System.currentTimeMillis(); 
          //initServiceClient(conf); 

          quorumPeer.join();
      } catch (InterruptedException e) {
          // warn, but generally this is ok
          LOG.warn("Quorum Peer interrupted", e);
      }
    }

    //private void initServiceClient(Configuration conf) throws IOException {
      //if (this.client != null) return; 

      //serviceInfo.setStartTime(getStartTime());
      //serviceInfo.setUserName(VersionInfo.getUserName());

      //this.client = new ServiceClient(conf, serviceInfo); 
      //if (!conf.getBoolean("paxos.singled.mode", false))
      //  this.client.startup(); 
    //}

    public void shutdown() { 
    	if (quorumPeer != null)
    		quorumPeer.shutdown();
    	if (quorumServer != null)
    		quorumServer.shutdown();
    }
    
    public long getStartTime() {
      return this.startTime; 
    }

}

