package org.javenstudio.lightning.util;

import org.javenstudio.raptor.dfs.tools.DFSShell;

public class SimpleFsShell extends SimpleShell {

	public static void main(String[] args) throws Exception { 
		DFSShell.doMain(loadConf().getConf(), args);
	}
	
}
