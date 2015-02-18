package org.javenstudio.lightning.core.service;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.lightning.core.CoreContainers;
import org.javenstudio.lightning.core.CoreService;
import org.javenstudio.lightning.core.ServiceInitializer;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.dfs.server.datanode.DataNode;

public class DatanodeService extends CoreService {
	private static final Logger LOG = Logger.getLogger(NamenodeService.class);

	public static final String NAME = "datanode";
	
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
				//FileSystem.setDefaultUri(conf, uri);
				
				return new DatanodeService(conf, name);
			//} catch (ErrorException ee) { 
			//	throw ee;
			} catch (Throwable e) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		}
	}
	
	private final Configuration mConf;
	private final ServiceThread mService;
	private final String mName;
	
	private DatanodeService(final Configuration conf, String name) { 
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
	
	private class ServiceThread extends Thread { 
		private DataNode mServer = null;
		
		@Override
		public void run() { 
			try { 
				mServer = DataNode.createDataNode(null, mConf);
				mServer.join();
				
				if (LOG.isInfoEnabled())
					LOG.info(getName() + " stopped");
			} catch (Throwable e) { 
				if (LOG.isErrorEnabled())
					LOG.error("run: " + getName() + " error: " + e, e);
			}
		}
		
		public void shutdown() {
			DataNode server = mServer;
			if (server != null) 
				server.shutdown();
		}
	}
	
}
