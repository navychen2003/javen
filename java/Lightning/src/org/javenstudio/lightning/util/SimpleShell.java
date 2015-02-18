package org.javenstudio.lightning.util;

import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.common.util.Log;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.lightning.Constants;
import org.javenstudio.lightning.core.CoreAdminConfig;
import org.javenstudio.lightning.core.CoreFactory;
import org.javenstudio.raptor.conf.Configuration;

public abstract class SimpleShell {
	static final String DEBUG_ENV = "org.javenstudio.common.util.logger.debug";
	static final AtomicLong sCounter = new AtomicLong(1);
	static int sLogLevel = Log.LOG_LEVEL_D;
	
	static { 
		String debugProp = System.getenv(DEBUG_ENV);
		if (debugProp == null || debugProp.length() == 0) 
			debugProp = System.getProperty(DEBUG_ENV);
		if (debugProp == null) debugProp = "false";
		final boolean loggerDebug = debugProp.startsWith("true");
		
		Log.setLogDebug(loggerDebug);
		Log.setLogImpl(new Log.LogImpl() {
				@Override
				public void log(int level, String tag, String message, Throwable e) {
					if (loggerDebug && sLogLevel >= level) { 
						System.err.println(""+Log.toString(level)+"/"+tag+"("+sCounter.getAndIncrement()+"): "+message);
						if (e != null) 
							e.printStackTrace(System.err);
					}
				}
			});
	}
	
	static String getInstanceDir() { 
		return ContextLoader.locateHome(Constants.PROJECT);
	}
	
	static String getAppDir() { 
		return ContextLoader.locateHome(Constants.APPSERVER_NAME);
	}
	
	public static CoreAdminConfig loadConf() throws Exception { 
		final String appDir = getAppDir();
		final String homeDir = getInstanceDir();
		final ContextLoader loader = CoreFactory.createDefaultLoader(homeDir);
		
		return CoreAdminConfig.load(loader, homeDir, appDir);
	}
	
	public static Configuration getConf() throws Exception { 
		return loadConf().getConf();
	}
}
