package org.javenstudio.lightning.request;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.MultiMapParams;

public class HttpHelper {

	/** HTTP content type header name. */
    public static final String CONTENT_TYPE = "Content-type";

    /** HTTP content disposition header name. */
    public static final String CONTENT_DISPOSITION = "Content-disposition";

    /** HTTP content length header name. */
    public static final String CONTENT_LENGTH = "Content-length";

    /** Content-disposition value for form data. */
    public static final String FORM_DATA = "form-data";

    /** Content-disposition value for file attachment. */
    public static final String ATTACHMENT = "attachment";

    /** Part of HTTP content type header. */
    public static final String MULTIPART = "multipart/";

    /** HTTP content type header for multipart forms. */
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    /** HTTP content type header for multiple uploads. */
    public static final String MULTIPART_MIXED = "multipart/mixed";

    /**
     * The maximum length of a single header line that will be parsed
     * (1024 bytes).
     * @deprecated This constant is no longer used. As of commons-fileupload
     *   1.2, the only applicable limit is the total size of a parts headers,
     *   {@link MultipartStream#HEADER_PART_SIZE_MAX}.
     */
    public static final int MAX_HEADER_SIZE = 1024;
	
    /**
     * Given a standard query string map it into params
     */
    public static MultiMapParams parseQueryString(String queryString) 
    		throws ErrorException {
    	Map<String,String[]> map = new HashMap<String, String[]>();
    	
    	if (queryString != null && queryString.length() > 0) {
    		try {
    			for (String kv : queryString.split("&")) {
    				int idx = kv.indexOf('=');
    				if (idx > 0) {
    					String name = URLDecoder.decode(kv.substring(0, idx), "UTF-8");
    					String value = URLDecoder.decode(kv.substring(idx+1), "UTF-8");
    					
    					MultiMapParams.addParam(name, value, map);
    					
    				} else {
    					String name = URLDecoder.decode(kv, "UTF-8");
    					
    					MultiMapParams.addParam(name, "", map);
    				}
    			}
    		} catch (UnsupportedEncodingException uex) {
    			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, uex);
    		}
    	}
    	
    	return new MultiMapParams(map);
    }
	
    /**
     * Utility method that determines whether the request contains multipart
     * content.
     *
     * @param request The servlet request to be evaluated. Must be non-null.
     *
     * @return <code>true</code> if the request is multipart;
     *         <code>false</code> otherwise.
     */
    public static final boolean isMultipartContent(RequestInput input) {
    	if (!"post".equals(input.getMethod().toLowerCase())) 
    		return false;
    	
    	String contentType = input.getContentType();
    	if (contentType == null) 
    		return false;
      
    	if (contentType.toLowerCase().startsWith(MULTIPART)) 
    		return true;
    	
    	return false;
    }
  
}
