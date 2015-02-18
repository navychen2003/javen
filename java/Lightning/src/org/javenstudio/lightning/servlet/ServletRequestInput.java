package org.javenstudio.lightning.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.AdminParams;
import org.javenstudio.falcon.util.MultiMapParams;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.fileupload.FileUploadBase;
import org.javenstudio.lightning.fileupload.UploadContext;
import org.javenstudio.lightning.request.HttpHelper;
import org.javenstudio.lightning.request.RequestInput;

public class ServletRequestInput implements RequestInput, UploadContext {

	private final HttpServletRequest mRequest;
	private String mPath;
	
	public ServletRequestInput(String path, HttpServletRequest request) { 
		mRequest = request;
		mPath = path;
	}
	
	@Override
	public String getProtocol() { 
		return mRequest.getProtocol();
	}
	
	@Override
	public String getScheme() { 
		return mRequest.getScheme();
	}
	
	@Override
	public String getServerName() { 
		return mRequest.getServerName();
	}
	
	@Override
	public int getServerPort() { 
		return mRequest.getServerPort();
	}
	
	@Override
	public String getMethod() { 
		return mRequest.getMethod();
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
		return HttpHelper.isMultipartContent(this);
	}
	
	@Override
	public boolean checkAdminPath(String adminPath) { 
		return mPath.equals(adminPath);
	}
	
	@Override
	public String getAdminHandlerName(Params params) throws ErrorException { 
		return params.get(AdminParams.ACTION);
	}
	
	@Override
	public String getHandlerName(Params params) { 
		return getQueryPath();
	}
	
	@Override
	public Map<String, String[]> getParameterMap() {
		return mRequest.getParameterMap();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return mRequest.getInputStream();
	}

	@Override
	public String getHeader(String name) {
		return mRequest.getHeader(name);
	}

	@Override
	public String getUserAgent() { 
		return getHeader("User-Agent");
	}
	
	@Override
	public String getContentType() {
		return mRequest.getContentType();
	}

	@Override
	public String getContextPath() { 
		return mRequest.getContextPath();
	}
	
	@Override
	public String getQueryString() {
		return mRequest.getQueryString();
	}

	@Override
	public String getCharacterEncoding() {
		return mRequest.getCharacterEncoding();
	}
	
	@Override
	public int getContentLength() { 
		return mRequest.getContentLength();
	}
	
    /**
     * Retrieve the content length of the request.
     *
     * @return The content length of the request.
     * @since 1.3
     */
	@Override
    public long getUploadLength() {
        long size;
        try {
            size = Long.parseLong(mRequest.getHeader(FileUploadBase.CONTENT_LENGTH));
        } catch (NumberFormatException e) {
            size = mRequest.getContentLength();
        }
        return size;
    }
	
	@Override
	public MultiMapParams getQueryStringAsParams() throws ErrorException { 
		return HttpHelper.parseQueryString(getQueryString());
	}
	
	@Override
	public String getRemoteAddr() { 
		String ip = mRequest.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
	        ip = mRequest.getHeader("Proxy-Client-IP");
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
	        ip = mRequest.getHeader("WL-Proxy-Client-IP");
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
	        ip = mRequest.getRemoteAddr();
	    if (ip == null || ip.length() == 0)
	    	ip = "unknown";
	    if (ip != null && ip.equalsIgnoreCase("0:0:0:0:0:0:0:1"))
	    	ip = "127.0.0.1";
	    return ip;  
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{hostUri=" + getScheme() 
				+ "://" + getServerName() + ":" + getServerPort() 
				+ ",protocol=" + getProtocol() + ",method=" + getMethod() 
				+ ",userAgent=" + getUserAgent() + ",contentType=" + getContentType() 
				+ "}";
	}
	
}
