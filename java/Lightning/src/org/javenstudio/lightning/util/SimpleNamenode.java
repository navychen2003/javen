package org.javenstudio.lightning.util;

import org.javenstudio.raptor.dfs.server.namenode.NameNode;

public class SimpleNamenode extends SimpleShell {

	public static void main(String[] args) throws Exception { 
		NameNode.doMain(loadConf().getConf(), args);
	}
	
}
