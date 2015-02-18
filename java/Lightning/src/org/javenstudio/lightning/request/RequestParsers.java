package org.javenstudio.lightning.request;

import java.util.ArrayList;
import java.util.HashMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.mime.Base64Util;

public final class RequestParsers {
	private static final Logger LOG = Logger.getLogger(RequestParsers.class);
	
	private final RequestConfig mConfig;
	
	private final HashMap<String, RequestParser> mParsers = 
			new HashMap<String, RequestParser>();
	
	/**
	 * Pass in an xml configuration.  A null configuration will enable
	 * everything with maximum values.
	 */
	public RequestParsers(RequestConfig config) throws ErrorException { 
		mConfig = config;
		config.initParsers(this);
	}

	public RequestParser getDefaultParser() { 
		return getParser("");
	}
	
	public synchronized RequestParser getParser(String name) { 
		return mParsers.get(name);
	}
	
	public synchronized void registerParser(String name, RequestParser parser) { 
		mParsers.put(name, parser);
	}
	
	public Request parseRequest(RequestAcceptor acceptor, RequestInput input) 
			throws ErrorException { 
		// TODO -- in the future, we could pick a different parser based on the request
		RequestParser parser = getDefaultParser();
		
		// Pick the parser from the request...
	    ArrayList<ContentStream> streams = new ArrayList<ContentStream>(1);
	    Params params = parser.parseParamsAndFillStreams(input, streams);
	    decodeSecret(params);
	    
	    if (LOG.isDebugEnabled())
	    	LOG.debug("parseRequest: config=" + mConfig + " params=" + params);
		
	    Request request = mConfig.buildRequest(acceptor, input, params, streams);
	    
	    // Handlers and login will want to know the path. If it contains a ':'
	    // the handler could use it for RESTful URLs
	    //request.getContextMap().put("path", input.getQueryPath());
	    
		return request;
	}
	
	private void decodeSecret(Params params) throws ErrorException {
		if (params == null) return;
		if (!(params instanceof ModifiableParams)) return;
		
		ModifiableParams mp = (ModifiableParams)params;
		String[] names = mp.getParameterNames();
		
		for (int i=0; names != null && i < names.length; i++) {
			String name = names[i];
			if (name == null || !name.startsWith(SECRET_PARAMNAME)) 
				continue; 
			
			String decodeName = name.substring(SECRET_PARAMNAME.length());
			if (decodeName.length() > 0) {
				String[] values = mp.getParams(name);
				if (values != null && values.length > 0) {
					ArrayList<String> list = new ArrayList<String>();
					
					for (String value : values) {
						if (value == null || value.length() == 0)
							continue;
						
						try {
							String decodeValue = Base64Util.decodeSecret(value);
							if (decodeValue != null) {
								list.add(decodeValue);
								
								if (LOG.isDebugEnabled()) {
									LOG.debug("decodeSecret: param=" + name 
											+ " value=" + value + " decoded=" + decodeValue);
								}
							}
						} catch (Throwable e) {
							if (LOG.isWarnEnabled()) {
								LOG.warn("decodeSecret: param=" + name + " value=" + value 
										+ " error: " + e, e);
							}
						}
					}
					
					if (list.size() > 0) {
						mp.add(decodeName, list.toArray(new String[list.size()]));
					}
				}
			}
		}
	}
	
	private static final String SECRET_PARAMNAME = "secret.";
	
}
