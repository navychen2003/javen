package org.javenstudio.lightning.request;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.MultiMapParams;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.fileupload.RequestContext;

public interface RequestInput extends RequestContext {

	public void setQueryPath(String path);
	
	public String getProtocol();
	public String getScheme();
	public String getServerName();
	public int getServerPort();
	
	public String getMethod();
	public String getCharacterEncoding();
	public String getContentType();
	public String getContextPath();
	public String getQueryPath();
	public String getQueryString();
	public String getHeader(String name);
	public String getRemoteAddr();
	public String getUserAgent();
	
	public boolean isMultipartContent();
	public int getContentLength();
	
	public Map<String,String[]> getParameterMap() throws ErrorException;
	public InputStream getInputStream() throws IOException;
	
	public boolean checkAdminPath(String adminPath) throws ErrorException;
	public String getAdminHandlerName(Params params) throws ErrorException;
	public String getHandlerName(Params params) throws ErrorException;
	
	public MultiMapParams getQueryStringAsParams() throws ErrorException;
	
}
