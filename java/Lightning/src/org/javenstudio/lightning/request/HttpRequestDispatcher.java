package org.javenstudio.lightning.request;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.Constants;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.FastWriter;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.core.CoreAdmin;
import org.javenstudio.lightning.core.CoreContainers;
import org.javenstudio.lightning.core.CoreInitializer;
import org.javenstudio.lightning.core.ServiceInitializer;
import org.javenstudio.lightning.response.BinaryResponseWriter;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseOutput;
import org.javenstudio.lightning.response.ResponseWriter;
import org.javenstudio.lightning.util.DebugWriter;

public abstract class HttpRequestDispatcher extends RequestDispatcher {
	static Logger LOG = Logger.getLogger(HttpRequestDispatcher.class);

	private final CoreContainers mContainers;
	
	protected HttpRequestDispatcher(ServiceInitializer[] services, 
			CoreInitializer[] initers, String home, String apphome) 
			throws ErrorException { 
		if (initers == null) throw new NullPointerException();
		mContainers = new CoreContainers(home, apphome);
		
		if (services != null) { 
			for (ServiceInitializer initer : services) { 
				mContainers.init(initer);
			}
		}
		
		mContainers.initServices();
		
		if (initers != null) {
			for (CoreInitializer initer : initers) { 
				mContainers.init(initer);
			}
		}
		
		mContainers.initCores();
		mContainers.onInited();
	}
	
	public final CoreContainers getContainers() {
		return mContainers;
	}
	
	@Override
	public void destroy() { 
		mContainers.shutdown();
	}
	
	@Override
	public boolean doFilter(RequestInput input, ResponseOutput output) { 
		Core core = null;
		String path = input.getQueryPath();
		
		try { 
			RequestAcceptor acceptor = null;
			
			CoreAdmin admin = mContainers.getAdmin();
			if (admin != null && admin.checkAdminPath(input)) {
				acceptor = admin;
				
				if (LOG.isDebugEnabled())
	        		LOG.debug("use CoreAdmin for path: " + path);
			}
			
			if (acceptor == null) {
				//otherwise, we should find a core from the path
				String coreName = "";
				
		        int idx = path.indexOf("/", 1);
		        if (idx > 1) {
		            // try to get the corename as a request parameter first
		        	coreName = path.substring(1, idx);
		            core = mContainers.getCore(coreName);
		            if (core != null) 
		            	path = path.substring(idx);
		        }
		        
		        if (core == null) 
		        	core = mContainers.getCore("");
				
		        if (core != null) { 
		        	if (LOG.isDebugEnabled())
		        		LOG.debug("found core: " + core.getName() + " for path: " + path);
		        	
		        	acceptor = core;
		        	input.setQueryPath(path);
		        }
			}
	        
			if (acceptor != null && dispatchRequest(acceptor, input, output)) 
				return true;
			
		} catch (Throwable ex) { 
			if (LOG.isDebugEnabled())
				ex.printStackTrace(System.err);
			
			if (LOG.isErrorEnabled())
				LOG.error("doFilter error: " + ex.toString(), ex);
			
		} finally { 
			if (core != null) 
				core.close();
		}
		
		if (LOG.isDebugEnabled())
        	LOG.debug("no handler retrieved for " + path + ", follow through...");
		
		return false;
	}
	
	@Override
	public void writeResponse(Request request, Response response, 
			ResponseOutput output, ResponseWriter writer) throws ErrorException { 
		try { 
			Request req = request;
			Response rsp = response;
			
			String contentType = writer.getContentType(req, rsp);
			if (contentType != null)
				output.setContentType(contentType);
			
			int qtime = (int)(rsp.getEndTime() - req.getStartTime());
			int status = 0;
			
			Throwable ex = rsp.getException();
			if (ex != null) { 
				NamedList<Object> info = new NamedMap<Object>();
				status = getErrorInfo(ex, info);
				rsp.add("error", info);
				output.setStatus(status);
				
				if (LOG.isDebugEnabled())
					LOG.debug("writeResponse: error: " + ex.toString(), ex);
			}
			
			// TODO should check that responseHeader has not been replaced by handler
			NamedList<Object> responseHeader = rsp.getResponseHeader();
			
			responseHeader.add("status", status);
		    responseHeader.add("qtime", qtime);

		    if (rsp.getToLog().size() > 0) {
		    	rsp.getToLog().add("status", status);
		    	rsp.getToLog().add("qtime", qtime);
		    }
			
			if (HttpMethod.HEAD != req.getRequestMethod()) { 
				if (writer instanceof BinaryResponseWriter) { 
					if (LOG.isDebugEnabled())
						LOG.debug("writeResponse: write binary stream to " + writer);
					
					BinaryResponseWriter binWriter = (BinaryResponseWriter)writer;
					binWriter.write(output.getOutputStream(), request, response);
					
				} else { 
					String charset = getCharsetFromContentType(contentType);
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("writeResponse: write text (type=" + contentType 
								+ ") with charset: " + charset + " to " + writer);
					}
					
					Writer out = (charset == null || charset.equalsIgnoreCase("UTF-8"))
							? new OutputStreamWriter(output.getOutputStream(), Constants.UTF_8)
							: new OutputStreamWriter(output.getOutputStream(), charset);
							
					out = new FastWriter(out);
					if (LOG.isDebugEnabled())
						out = new DebugWriter(out);
					
					writer.write(out, req, rsp);
					out.flush();
				}
			}
			//else http HEAD request, nothing to write out, 
			//waited this long just to get ContentType
			
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	protected String getCharsetFromContentType(String contentType) {
  		if (contentType != null) {
  			int idx = contentType.toLowerCase(Locale.ROOT).indexOf( "charset=" );
  			if (idx > 0) 
  				return contentType.substring(idx + "charset=".length()).trim();
  		}
  		return null;
  	}
	
	protected int getErrorInfo(Throwable ex, NamedList<Object> info) {
	    int code = 500;
	    
	    if (ex instanceof ErrorException) 
	    	code = ((ErrorException)ex).getCode();

	    for (Throwable th = ex; th != null; th = th.getCause()) {
	    	String msg = th.getMessage();
	    	if (msg != null) {
	    		info.add("msg", msg);
	    		break;
	    	}
	    }
	    
	    // For any regular code, don't include the stack trace
	    if (ifTraceError(code, ex)) { //code == 500 || code < 100) {
	    	StringWriter sw = new StringWriter();
	    	ex.printStackTrace(new PrintWriter(sw));
	    	info.add("trace", sw.toString());

	    	// non standard codes have undefined results with various servers
	    	if (code < 100) {
	    		if (LOG.isWarnEnabled())
	    			LOG.warn("invalid return code: " + code);
	    		
	    		code = 500;
	    	}
	    }
	    
	    info.add("code", new Integer(code));
	    
	    return code;
	}
	
	protected boolean ifTraceError(int code, Throwable ex) { 
		return code == 500 || code < 100 || mContainers.getAdminConfig().isResponseTrace(); 
	}
	
}
