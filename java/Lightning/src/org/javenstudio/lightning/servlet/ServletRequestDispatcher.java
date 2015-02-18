package org.javenstudio.lightning.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.lightning.core.CoreInitializer;
import org.javenstudio.lightning.core.ServiceInitializer;
import org.javenstudio.lightning.request.HttpRequestDispatcher;
import org.javenstudio.lightning.request.RequestInput;
import org.javenstudio.lightning.response.ResponseOutput;

public class ServletRequestDispatcher extends HttpRequestDispatcher {
	private static final Logger LOG = Logger.getLogger(ServletRequestDispatcher.class);

	public ServletRequestDispatcher(ServiceInitializer[] services, 
			CoreInitializer[] initers, String home, String apphome) 
			throws ErrorException { 
		super(services, initers, home, apphome);
	}
	
	public boolean doServletFilter(HttpServletRequest request, 
			HttpServletResponse response) { 
		String path = request.getServletPath();
		
		if (request.getPathInfo() != null) {
        	// this lets you handle /update/commit when /update is a servlet
        	path += request.getPathInfo();
        }
		
		if (LOG.isDebugEnabled())
			LOG.debug("request path: " + path);
		
		final RequestInput input = new ServletRequestInput(path, request);
		final ResponseOutput output = new ServletResponseOutput(response);
		
		return doFilter(input, output);
	}
	
}
