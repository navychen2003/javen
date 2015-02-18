package org.javenstudio.raptor.bigdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.paxos.server.NIOServerCnxn;
import org.javenstudio.raptor.paxos.server.PaxosServer;
import org.javenstudio.raptor.paxos.server.persistence.FileTxnLog;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeerConfig;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeerConfig.ConfigException;
import org.javenstudio.raptor.util.InputSource;

/**
 * TODO: Most of the code in this class is ripped from Paxos tests. Instead
 * of redoing it, we should contribute updates to their code which let us more
 * easily access testing helper objects.
 */
public class MiniPaxosCluster {
  private static final Logger LOG = Logger.getLogger(MiniPaxosCluster.class);

  private static final int TICK_TIME = 2000;
  private static final int CONNECTION_TIMEOUT = 30000;

  private boolean started;
  private int clientPort = 21810; // use non-standard port

  private NIOServerCnxn.Factory standaloneServerFactory;
  private int tickTime = 0;

  /** Create mini Paxos cluster. */
  public MiniPaxosCluster() {
    this.started = false;
  }

  public int startup(InputSource source) 
	  throws IOException, ConfigException, InterruptedException {
	QuorumPeerConfig config = new QuorumPeerConfig();
	config.parse(source);
	
	setTickTime(config.getTickTime());
	setClientPort(config.getClientPort());
	
	return startup(new File(config.getDataDir()));
  }
  
  public void setClientPort(int clientPort) {
    this.clientPort = clientPort;
  }

  public void setTickTime(int tickTime) {
    this.tickTime = tickTime;
  }

  // / XXX: From o.a.zk.t.ClientBase
  private static void setupTestEnv() {
    // during the tests we run with 100K prealloc in the logs.
    // on windows systems prealloc of 64M was seen to take ~15seconds
    // resulting in test failure (client timeout on first session).
    // set env and directly in order to handle static init/gc issues
    System.setProperty("paxos.preAllocSize", "100");
    FileTxnLog.setPreallocSize(100);
  }

  /**
   * @param baseDir
   * @return ClientPort server bound to.
   * @throws IOException
   * @throws InterruptedException
   */
  public int startup(File dir) throws IOException, InterruptedException {
    setupTestEnv();
    shutdown();

    //File dir = new File(baseDir, "paxos").getAbsoluteFile();
    recreateDir(dir);

    int tickTimeToUse;
    if (this.tickTime > 0) {
      tickTimeToUse = this.tickTime;
    } else {
      tickTimeToUse = TICK_TIME;
    }
    
    PaxosServer server = new PaxosServer(dir, dir, tickTimeToUse);
    while (true) {
      try {
        standaloneServerFactory =
          new NIOServerCnxn.Factory(new InetSocketAddress(clientPort));
      } catch (BindException e) {
        LOG.info("Faild binding ZK Server to client port: " + clientPort);
        //this port is already in use. try to use another
        clientPort++;
        continue;
      }
      break;
    }
    standaloneServerFactory.startup(server);

    if (!waitForServerUp(clientPort, CONNECTION_TIMEOUT)) {
      throw new IOException("Waiting for startup of standalone server");
    }

    started = true;

    return clientPort;
  }

  private void recreateDir(File dir) throws IOException {
    if (dir.exists()) {
      //FileUtil.fullyDelete(dir);
    }
    try {
      dir.mkdirs();
    } catch (SecurityException e) {
      throw new IOException("creating dir: " + dir, e);
    }
  }

  /**
   * @throws IOException
   */
  public void shutdown() throws IOException {
    if (!started) {
      return;
    }

    standaloneServerFactory.shutdown();
    if (!waitForServerDown(clientPort, CONNECTION_TIMEOUT)) {
      throw new IOException("Waiting for shutdown of standalone server");
    }

    started = false;
  }

  // XXX: From o.a.zk.t.ClientBase
  private static boolean waitForServerDown(int port, long timeout) {
    long start = System.currentTimeMillis();
    while (true) {
      try {
        Socket sock = new Socket("localhost", port);
        try {
          OutputStream outstream = sock.getOutputStream();
          outstream.write("stat".getBytes());
          outstream.flush();
        } finally {
          sock.close();
        }
      } catch (IOException e) {
        return true;
      }

      if (System.currentTimeMillis() > start + timeout) {
        break;
      }
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        // ignore
      }
    }
    return false;
  }

  // XXX: From o.a.zk.t.ClientBase
  private static boolean waitForServerUp(int port, long timeout) {
    long start = System.currentTimeMillis();
    while (true) {
      try {
        Socket sock = new Socket("localhost", port);
        BufferedReader reader = null;
        try {
          OutputStream outstream = sock.getOutputStream();
          outstream.write("stat".getBytes());
          outstream.flush();

          Reader isr = new InputStreamReader(sock.getInputStream());
          reader = new BufferedReader(isr);
          String line = reader.readLine();
          if (line != null && line.startsWith("Paxos version:")) {
            return true;
          }
        } finally {
          sock.close();
          if (reader != null) {
            reader.close();
          }
        }
      } catch (IOException e) {
        // ignore as this is expected
        LOG.info("server localhost:" + port + " not up " + e);
      }

      if (System.currentTimeMillis() > start + timeout) {
        break;
      }
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        // ignore
      }
    }
    return false;
  }
}

