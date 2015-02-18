package org.javenstudio.lightning.core.service;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.cluster.HostMode;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.lightning.core.CoreCluster;
import org.javenstudio.lightning.core.CoreContainers;
import org.javenstudio.lightning.core.CoreService;
import org.javenstudio.lightning.core.ServiceInitializer;
import org.javenstudio.raptor.conf.Configuration;

public class FinderService extends CoreService {
	private static final Logger LOG = Logger.getLogger(NamenodeService.class);

	public static final String NAME = "finder";
	
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
				
				return new FinderService(containers, loader, conf, name);
			//} catch (ErrorException ee) { 
			//	throw ee;
			} catch (Throwable e) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		}
	}
	
	private final CoreContainers mContainers;
	private final ContextLoader mLoader;
	private final Configuration mConf;
	private final ServiceThread mService;
	private final String mName;
	private final int mPort;
	
	private FinderService(CoreContainers containers, ContextLoader loader, 
			Configuration conf, String name) { 
		mContainers = containers;
		mLoader = loader;
		mConf = conf;
		mName = name != null && name.length() > 0 ? name : NAME;
		mPort = conf.getInt("finder.udp.port", 10099);
		mService = new ServiceThread();
		mService.start();
	}
	
	public CoreContainers getContainers() { return mContainers; }
	public CoreCluster getCluster() { return getContainers().getCluster(); }
	public ContextLoader getLoader() { return mLoader; }
	public Configuration getConf() { return mConf; }
	
	public String getName() { return mName; }
	public int getPort() { return mPort; }
	
	@Override
	public void shutdown() { 
		if (LOG.isDebugEnabled())
			LOG.debug(getName() + " shutting down");
		
		mService.shutdown();
	}
	
	private class ServiceThread extends Thread { 
		private FinderHelper.FinderServer mServer = null;
		
		@Override
		public void run() { 
			try { 
				if (getCluster().getHostSelf().getHostMode() != HostMode.ATTACH) {
					mServer = new FinderHelper.FinderServer(getContainers());
					mServer.start(getPort());
				}
				if (LOG.isInfoEnabled())
					LOG.info(getName() + " stopped");
			} catch (Throwable e) { 
				if (LOG.isErrorEnabled())
					LOG.error("run: " + getName() + " error: " + e, e);
			}
		}
		
		public void shutdown() {
			try { 
				FinderHelper.FinderServer server = mServer;
				if (server != null) server.close();
				
			} catch (Throwable e) { 
				if (LOG.isErrorEnabled())
					LOG.error("shutdown: " + getName() + " error: " + e, e);
			}
		}
	}
	
}
