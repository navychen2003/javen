package org.javenstudio.lightning.core.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.lightning.core.CoreContainers;
import org.javenstudio.lightning.core.CoreService;
import org.javenstudio.lightning.core.ServiceInitializer;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.dfs.server.namenode.FSNamesystem;
import org.javenstudio.raptor.dfs.server.namenode.NameNode;

public class NamenodeService extends CoreService {
	private static final Logger LOG = Logger.getLogger(NamenodeService.class);

	public static final String NAME = "namenode";
	
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
				
				Collection<File> dirs = FSNamesystem.getNamespaceDirs(conf);
				Collection<File> files = new ArrayList<File>();
				
				for (File dir : dirs) { 
					if (!dir.exists()) dir.mkdirs();
					File[] subfiles = dir.listFiles();
					if (subfiles != null) { 
						for (File f : subfiles) { 
							files.add(f);
						}
					}
				}
				
				if (files.size() == 0)
					NameNode.format(conf);
				
				return new NamenodeService(containers, conf, name);
			//} catch (ErrorException ee) { 
			//	throw ee;
			} catch (Throwable e) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		}
	}
	
	private final CoreContainers mContainers;
	private final Configuration mConf;
	private final ServiceThread mService;
	private final String mName;
	
	private NamenodeService(CoreContainers c, Configuration conf, String name) { 
		mContainers = c;
		mConf = conf;
		mName = name != null && name.length() > 0 ? name : NAME;
		mService = new ServiceThread();
		mService.start();
	}
	
	public CoreContainers getContainers() { return mContainers; }
	public String getName() { return mName; }
	
	@Override
	public void shutdown() { 
		if (LOG.isDebugEnabled())
			LOG.debug(getName() + " shutting down");
		
		mService.shutdown();
	}
	
	private class ServiceThread extends Thread { 
		private NameNode mServer = null;
		
		@Override
		public void run() { 
			try { 
				mServer = new NameNode(mConf);
				
				String uri = NameNode.getUri(mServer.getNameNodeAddress()).toString();
				mContainers.getAdminConfig().addStoreUri(uri);
				
				mServer.join();
				
				if (LOG.isInfoEnabled())
					LOG.info(getName() + " stopped");
			} catch (Throwable e) { 
				if (LOG.isErrorEnabled())
					LOG.error("run: " + getName() + " error: " + e, e);
			}
		}
		
		public void shutdown() {
			NameNode server = mServer;
			if (server != null) 
				server.stop();
		}
	}
	
}
