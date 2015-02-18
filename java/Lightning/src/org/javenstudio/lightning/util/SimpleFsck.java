package org.javenstudio.lightning.util;

import org.javenstudio.raptor.dfs.tools.DFSck;

public class SimpleFsck extends SimpleShell {

	public static void main(String[] args) throws Exception { 
		DFSck.doMain(loadConf().getConf(), args);
	}
	
}
