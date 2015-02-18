package org.javenstudio.lightning.util;

import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeerMain;
import org.javenstudio.raptor.util.InputSource;

public class SimplePaxos extends SimpleShell {

	public static void main(String[] args) throws Exception { 
		final ContextLoader loader = loadConf().getLoader();
		QuorumPeerMain.doMain(new InputSource() {
				@Override
				public InputStream openStream() throws IOException {
					return loader.openResourceAsStream("paxos.conf");
				}
			}, args);
	}
	
}
