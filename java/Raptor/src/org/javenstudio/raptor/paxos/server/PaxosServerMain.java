package org.javenstudio.raptor.paxos.server;

import java.io.File;
import java.io.IOException;

import javax.management.JMException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.paxos.jmx.ManagedUtil;
import org.javenstudio.raptor.paxos.server.persistence.FileTxnSnapLog;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeerConfig.ConfigException;
import org.javenstudio.raptor.util.InputSource;

/**
 * This class starts and runs a standalone PaxosServer.
 */
public class PaxosServerMain {
    private static final Logger LOG =
    		Logger.getLogger(PaxosServerMain.class);

    private static final String USAGE =
        "Usage: PaxosServerMain configfile | port datadir [ticktime] [maxcnxns]";

    private NIOServerCnxn.Factory cnxnFactory;
    //private PaxosServer mServer;

    //private ServiceClient client = null;
    //private ServiceInfo serviceInfo = null;
    private long startTime = 0;

    /**
     * Start up the Paxos server.
     *
     * @param args the configfile or the port datadir [ticktime]
     */
    public static void doMain(InputSource source, String[] args) {
    	PaxosServerMain main = new PaxosServerMain();
    	doMain(main, source, args);
    }
    
    public static void doMain(PaxosServerMain main, InputSource source, String[] args) {
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
        try {
            ManagedUtil.registerLog4jMBeans();
        } catch (JMException e) {
            LOG.warn("Unable to register log4j JMX control", e);
        }

        //Configuration conf = ConfigurationFactory.create();
        //QuorumPeerOptions options = StartupOptions.getQuorumPeerOptions();
        //options.parse(conf, args);

        //if (options.hasOption(options.SINGLED)) {
        //  conf.setBoolean("paxos.singled.mode", true);
        //}

        ServerConfig config = new ServerConfig();
        if (source != null)
        	config.parse(source);
        
        //if (options.hasOption(options.CONF)) {
        //    String val = options.getOptionValue(options.CONF);
        //    config.parse(val);
        //} else {
        //    config.parse(conf); 
        //}

        runFromConfig(config);
    }

    /**
     * Run from a ServerConfig.
     * @param config ServerConfig to use.
     * @throws IOException
     */
    //public void runFromConfig(ServerConfig config) throws IOException {
    //    runFromConfig(config, ConfigurationFactory.create()); 
    //}

    public void runFromConfig(ServerConfig config) throws IOException {
        LOG.info("Starting server");
        try {
            // Note that this thread isn't going to be doing anything else,
            // so rather than spawning another thread, we will just call
            // run() in this thread.
            // create a file logger url from the command line args
            PaxosServer server = new PaxosServer();
            //mServer = server;

            if (LOG.isDebugEnabled()) { 
            	LOG.debug("runFromConfig: dataDir=" + config.dataDir);
            	LOG.debug("runFromConfig: dataLogDir=" + config.dataLogDir);
            }
            
            //if (config.dataLogDir != null && config.dataDir != null) {
	            FileTxnSnapLog ftxn = new FileTxnSnapLog(
	            		new File(config.dataLogDir), new File(config.dataDir));
	            server.setTxnLogFactory(ftxn);
            //}
            
            server.setTickTime(config.tickTime);
            server.setMinSessionTimeout(config.minSessionTimeout);
            server.setMaxSessionTimeout(config.maxSessionTimeout);
            
            cnxnFactory = new NIOServerCnxn.Factory(
            		config.getClientPortAddress(), config.getMaxClientCnxns());
            cnxnFactory.startup(server);

            //serviceInfo = ServiceInfo.create(QuorumPeerMain.class, server.getClientPort(), conf);
            //serviceInfo.addServer(this.getClass().getSimpleName(),
            //  ClusterHelper.getLocalNetworkAddress(conf), server.getClientPort(), 1,
            //  new String[]{PaxosServer.class.getName()});

            startTime = System.currentTimeMillis();
            //initServiceClient(conf);

            cnxnFactory.join();
            if (server.isRunning()) 
                server.shutdown();
        } catch (InterruptedException e) {
            // warn, but generally this is ok
            LOG.warn("Server interrupted", e);
        }
    }

    /**
     * Shutdown the serving instance
     */
    public void shutdown() {
    	if (cnxnFactory != null)
    		cnxnFactory.shutdown();
    }

    //private void initServiceClient(Configuration conf) throws IOException {
      //if (this.client != null) return;

      //serviceInfo.setStartTime(getStartTime());
      //serviceInfo.setUserName(VersionInfo.getUserName());

      //this.client = new ServiceClient(conf, serviceInfo);
      //if (!conf.getBoolean("paxos.singled.mode", false))
      //  this.client.startup();
    //}

    public long getStartTime() {
      return this.startTime;
    }

}

