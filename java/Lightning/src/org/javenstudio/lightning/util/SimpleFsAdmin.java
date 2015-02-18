package org.javenstudio.lightning.util;

import org.javenstudio.raptor.dfs.tools.DFSAdmin;

public class SimpleFsAdmin extends SimpleShell {

	public static void main(String[] args) throws Exception { 
		DFSAdmin.doMain(loadConf().getConf(), args);
	}
	
}
