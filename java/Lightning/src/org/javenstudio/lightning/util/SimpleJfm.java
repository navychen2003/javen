package org.javenstudio.lightning.util;

public class SimpleJfm extends SimpleShell {

	public static void main(String[] args) throws Exception { 
		loadConf();
		org.javenstudio.jfm.main.Main.doMain(args);
	}
	
}
