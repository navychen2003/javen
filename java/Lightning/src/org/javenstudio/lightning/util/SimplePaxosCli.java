package org.javenstudio.lightning.util;

import org.javenstudio.raptor.paxos.PaxosMain;

public class SimplePaxosCli extends SimpleShell {

	public static void main(String[] args) throws Exception { 
		loadConf();
		PaxosMain.doMain(args);
	}
	
}
