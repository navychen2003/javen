package org.javenstudio.lightning.servlet;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.lightning.Constants;
import org.javenstudio.lightning.core.CoreFactory;
import org.javenstudio.lightning.core.CoreInitializer;
import org.javenstudio.lightning.core.ServiceInitializer;
import org.javenstudio.lightning.logging.DefaultLogger;

public abstract class DispatchFilter implements Filter {
	static final Logger LOG = DefaultLogger.getLogger(DispatchFilter.class);
	
	private volatile ServletRequestDispatcher mDispatcher;
	
	protected CoreInitializer createInitializer(CoreFactory factory) { 
		return factory != null ? new CoreInitializer(factory) : null;
	}
	
	protected abstract CoreFactory[] createFactories();
	protected abstract ServiceInitializer[] createServices();
	
	protected String getInstanceDir() { 
		return ContextLoader.locateHome(Constants.PROJECT);
	}
	
	protected String getAppDir() { 
		return ContextLoader.locateHome(Constants.APPSERVER_NAME);
	}
	
	protected void onPreInit() throws ErrorException {}
	
	protected void onInited(ServletRequestDispatcher dispatcher) 
			throws ErrorException {}
	
	@Override
	public synchronized final void init(FilterConfig config) 
			throws ServletException {
		if (LOG.isDebugEnabled())
			LOG.debug("initing filter");
		
		try {
			onPreInit();
			
			final String apphome = getAppDir();
			final String home = getInstanceDir();
			final CoreFactory[] factories = createFactories();
			final ServiceInitializer[] services = createServices();
			
			ArrayList<CoreInitializer> initers = new ArrayList<CoreInitializer>();
			for (int i=0; factories != null && i < factories.length; i++) { 
				final CoreInitializer initer = createInitializer(factories[i]);
				if (initer != null) 
					initers.add(initer);
			}
			
			ServletRequestDispatcher dispatcher = new ServletRequestDispatcher(services, 
					initers.toArray(new CoreInitializer[initers.size()]), 
					home, apphome);
			
			mDispatcher = dispatcher;
			onInited(dispatcher);
		} catch (Throwable ex) { 
			ex.printStackTrace(System.err);
			
			if (LOG.isErrorEnabled())
				LOG.error("Could not start, Check home property and the logs", ex);
			
			//throw new ServletException(ex);
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("init done.");
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		final ServletRequestDispatcher dispatcher = mDispatcher;
		
		if (dispatcher != null && request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest)request;
			HttpServletResponse httpResponse = (HttpServletResponse)response;
			
			if (dispatcher.doServletFilter(httpRequest, httpResponse))
				return;
		}
		
		chain.doFilter(request, response);
	}
	
	@Override
	public synchronized void destroy() {
		if (mDispatcher != null) { 
			mDispatcher.destroy();
			mDispatcher = null;
		}
	}
	
}
