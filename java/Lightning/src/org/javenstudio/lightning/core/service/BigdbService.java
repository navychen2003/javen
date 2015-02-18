package org.javenstudio.lightning.core.service;

import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.lightning.core.CoreContainers;
import org.javenstudio.lightning.core.CoreService;
import org.javenstudio.lightning.core.ServiceInitializer;
import org.javenstudio.raptor.bigdb.master.DBMaster;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.util.InputSource;

public class BigdbService extends CoreService {
	private static final Logger LOG = Logger.getLogger(NamenodeService.class);

	public static final String NAME = "bigdb";
	
	public static class Initializer extends ServiceInitializer {
		@Override
		public String getServiceName() { 
			return NAME;
		}
		
		@Override
		public CoreService initialize(CoreContainers containers, String name) 
				throws ErrorException { 
			if (LOG.isDebugEnabled())
				LOG.debug("initialize: service=" + getServiceName() + " name=" + name);
			
			try {
				Configuration conf = containers.getAdminConfig().createConf(name);
				ContextLoader loader = containers.getAdminConfig().getLoader();
				
				return new BigdbService(loader, conf, name);
			//} catch (ErrorException ee) { 
			//	throw ee;
			} catch (Throwable e) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		}
	}
	
	private final ContextLoader mLoader;
	private final Configuration mConf;
	private final ServiceThread mService;
	private final String mName;
	
	private BigdbService(ContextLoader loader, 
			Configuration conf, String name) { 
		mLoader = loader;
		mConf = conf;
		mName = name != null && name.length() > 0 ? name : NAME;
		mService = new ServiceThread();
		mService.start();
	}
	
	public String getName() { return mName; }
	
	@Override
	public void shutdown() { 
		if (LOG.isDebugEnabled())
			LOG.debug(getName() + " shutting down");
		
		mService.shutdown();
	}
	
	private class ServiceThread extends Thread implements DBMaster.Callback { 
		private DBMaster.Instance mInstance = null;
		
		@Override
		public void run() { 
			try { 
				DBMaster.doMain(mConf, new InputSource() {
						@Override
						public InputStream openStream() throws IOException {
							return mLoader.openResourceAsStream("paxos.conf");
						}
					}, this);
				
				if (LOG.isInfoEnabled())
					LOG.info(getName() + " stopped");
			} catch (Throwable e) { 
				if (LOG.isErrorEnabled())
					LOG.error("run: " + getName() + " error: " + e, e);
			}
		}
		
		public void shutdown() {
			try { 
				DBMaster.Instance instance = mInstance;
				if (instance != null)
					instance.shutdown();
			} catch (Throwable e) { 
				if (LOG.isErrorEnabled())
					LOG.error("shutdown: " + getName() + " error: " + e, e);
			}
		}

		@Override
		public void onInstanceCreated(DBMaster.Instance instance) {
			mInstance = instance;
		}
	}
	
}
