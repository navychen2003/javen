package org.javenstudio.lightning.request;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.AdminParams;
import org.javenstudio.falcon.util.MultiMapParams;
import org.javenstudio.falcon.util.Params;

public class LocalRequestInput implements RequestInput {

	private Map<String, String[]> mParamMap;
	private String mPath;
	
	public LocalRequestInput(String path) { 
		mParamMap = new HashMap<String, String[]>();
		mPath = path;
	}
	
	@Override
	public String getMethod() { 
		return "GET";
	}
	
	@Override
	public String getQueryPath() { 
		return mPath;
	}
	
	@Override
	public void setQueryPath(String path) { 
		mPath = path;
	}
	
	@Override
	public boolean isMultipartContent() { 
		return false;
	}
	
	@Override
	public boolean checkAdminPath(String adminPath) { 
		return mPath.equals(adminPath);
	}
	
	@Override
	public String getAdminHandlerName(Params params) 
			throws ErrorException { 
		return params.get(AdminParams.ACTION);
	}
	
	@Override
	public String getHandlerName(Params params) { 
		return getQueryPath();
	}
	
	@Override
	public Map<String, String[]> getParameterMap() {
		return mParamMap;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public String getHeader(String name) {
		return null;
	}

	@Override
	public String getRemoteAddr() { 
		return null;
	}
	
	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public String getContextPath() { 
		return null;
	}
	
	@Override
	public String getQueryString() {
		return null;
	}

	@Override
	public MultiMapParams getQueryStringAsParams() 
			throws ErrorException { 
		return HttpHelper.parseQueryString(getQueryString());
	}

	@Override
	public String getCharacterEncoding() {
		return null;
	}

	@Override
	public int getContentLength() {
		return 0;
	}

	@Override
	public String getProtocol() {
		return "HTTP/1.1";
	}

	@Override
	public String getScheme() {
		return "http";
	}

	@Override
	public String getServerName() {
		return "127.0.0.1";
	}

	@Override
	public int getServerPort() {
		return 80;
	}

	@Override
	public String getUserAgent() {
		return null;
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{path=" + mPath + "}";
	}
	
}
