package org.javenstudio.lightning.util;

import org.javenstudio.raptor.dfs.server.datanode.DataNode;

public class SimpleDatanode extends SimpleShell {

	public static void main(String[] args) throws Exception { 
		String name = null;
		for (int i=0; args != null && i < args.length; i++) { 
			String arg = args[i];
			if (arg.equals("-name") && i+1 < args.length) { 
				name = args[i+1];
				break;
			}
		}
		DataNode.doMain(loadConf().createConf(name), args);
	}
	
}
