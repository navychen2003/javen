package org.javenstudio.lightning.util;

import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.lightning.core.CoreAdminConfig;
import org.javenstudio.raptor.bigdb.master.DBMaster;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.util.InputSource;

public class SimpleBigdb extends SimpleShell implements DBMaster.Callback {

	public static void main(String[] args) throws Exception { 
		CoreAdminConfig config = loadConf();
		SimpleBigdb bigdb = new SimpleBigdb();
		bigdb.startup(config.getLoader(), config.getConf());
	}
	
	private DBMaster.Instance mInstance = null;
	
	private void startup(final ContextLoader loader, 
			final Configuration conf) throws Exception { 
		DBMaster.doMain(conf, new InputSource() {
				@Override
				public InputStream openStream() throws IOException {
					return loader.openResourceAsStream("paxos.conf");
				}
			}, this);
	}
	
	public void shutdown() {
		DBMaster.Instance instance = mInstance;
		if (instance != null)
			instance.shutdown();
	}
	
	@Override
	public void onInstanceCreated(DBMaster.Instance instance) {
		mInstance = instance;
	}
	
}
