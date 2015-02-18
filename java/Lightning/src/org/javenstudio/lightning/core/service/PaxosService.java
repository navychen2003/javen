package org.javenstudio.lightning.core.service;

import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.lightning.core.CoreContainers;
import org.javenstudio.lightning.core.CoreService;
import org.javenstudio.lightning.core.ServiceInitializer;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPeerMain;
import org.javenstudio.raptor.util.InputSource;

public class PaxosService extends CoreService {
	private static final Logger LOG = Logger.getLogger(NamenodeService.class);

	public static final String NAME = "paxos";
	
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
				ContextLoader loader = containers.getAdminConfig().getLoader();
				
				return new PaxosService(loader, name);
			//} catch (ErrorException ee) { 
			//	throw ee;
			} catch (Throwable e) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		}
	}
	
	private final ContextLoader mLoader;
	private final ServiceThread mService;
	private final String mName;
	
	private PaxosService(final ContextLoader loader, String name) { 
		mLoader = loader;
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
		private QuorumPeerMain mServer = null;
		
		@Override
		public void run() { 
			try { 
				mServer = new QuorumPeerMain();
				QuorumPeerMain.doMain(mServer, new InputSource() {
						@Override
						public InputStream openStream() throws IOException {
							return mLoader.openResourceAsStream("paxos.conf");
						}
					}, null);
				
				if (LOG.isInfoEnabled())
					LOG.info(getName() + " stopped");
			} catch (Throwable e) { 
				if (LOG.isErrorEnabled())
					LOG.error("run: " + getName() + " error: " + e, e);
			}
		}
		
		public void shutdown() {
			QuorumPeerMain server = mServer;
			if (server != null) 
				server.shutdown();
		}
	}
	
}
