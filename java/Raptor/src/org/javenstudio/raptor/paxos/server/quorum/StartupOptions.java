package org.javenstudio.raptor.paxos.server.quorum;

import org.javenstudio.raptor.util.Strings; 


public final class StartupOptions {

  static {
    Strings.addStrings(StartupOptions.class.getPackage().getName()); 
  }

  public static class QuorumPeerOptions /*extends ServiceOptions*/ {
    public static final String SINGLED = "singled";
    public static final String CONF = "conf"; 

    public QuorumPeerOptions() {
      //super(QuorumPeerMain.class); 
    }

    public void addOptions() {
      //addBooleanOption(SINGLED, Strings.get("paxos.singled"));

      //addArgumentOption(CONF, "filepath", Strings.get("paxos.configfile")); 
    }
  }

  public static QuorumPeerOptions getQuorumPeerOptions() {
    QuorumPeerOptions options = new QuorumPeerOptions(); 
    return options; 
  }

}
