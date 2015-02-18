package org.javenstudio.lightning.servlet;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.lightning.Constants;
import org.javenstudio.lightning.core.CoreContainers;
import org.javenstudio.lightning.core.CoreFactory;
import org.javenstudio.lightning.core.CoreInitializer;
import org.javenstudio.lightning.core.ServiceInitializer;
import org.javenstudio.lightning.core.datum.DatumCoreFactory;
import org.javenstudio.lightning.core.search.SearchCoreFactory;
import org.javenstudio.lightning.core.service.BigdbService;
import org.javenstudio.lightning.core.service.DatanodeService;
import org.javenstudio.lightning.core.service.FinderService;
import org.javenstudio.lightning.core.service.NamenodeService;
import org.javenstudio.lightning.core.user.UserCoreFactory;

public class LightningFilter extends DispatchFilter {
	//private static final Logger LOG = Logger.getLogger(LightningFilter.class);

	@Override
	protected CoreInitializer createInitializer(CoreFactory factory) { 
		if (factory == null) return null;
		return new CoreInitializer(factory);
	}
	
	@Override
	protected CoreFactory[] createFactories() { 
		return new CoreFactory[] { 
				new SearchCoreFactory(), 
				new DatumCoreFactory(), 
				new UserCoreFactory()
			};
	}
	
	@Override
	protected ServiceInitializer[] createServices() { 
		return new ServiceInitializer[] { 
				//new PaxosService.Initializer(), 
				new BigdbService.Initializer(),
				new NamenodeService.Initializer(), 
				new DatanodeService.Initializer(),
				new FinderService.Initializer()
			};
	}
	
	@Override
	protected String getInstanceDir() { 
		return ContextLoader.locateHome(Constants.PROJECT);
	}
	
	@Override
	protected String getAppDir() { 
		return ContextLoader.locateHome(Constants.APPSERVER_NAME);
	}
	
	@Override
	protected void onInited(ServletRequestDispatcher dispatcher) 
			throws ErrorException {
		final CoreContainers containers = dispatcher.getContainers();
		if (containers == null) return;
		
		containers.getCluster().joinHost();
	}
	
}
