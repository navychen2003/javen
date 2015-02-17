package org.javenstudio.cocoka.net.http.fetch;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.ResponseChecker;
import org.javenstudio.common.util.Logger;

public class FetchResponseChecker implements ResponseChecker {
	private static final Logger LOG = Logger.getLogger(FetchResponseChecker.class);

	@Override 
	public void checkRequestBeforeExecute(HttpClient client, 
			HttpUriRequest request) throws HttpException { 
		// do nothing
	}
	
	@Override 
	public boolean checkResponseSuccess(HttpResponse response) { 
		if (response == null) return false; 
		
		int statusCode = response.getStatusLine().getStatusCode(); 
		
		switch (statusCode) { 
		case HttpStatus.SC_OK: 
		case HttpStatus.SC_ACCEPTED: 
		case HttpStatus.SC_CREATED: 
		case HttpStatus.SC_PARTIAL_CONTENT: 
			return true;
		}
		
		return false; 
	}
	
	@Override 
	public boolean checkResponseRedirect(HttpResponse response, int redirectCount) { 
		if (response == null) return false; 
		
		int statusCode = response.getStatusLine().getStatusCode(); 
		
		if (LOG.isDebugEnabled())
			LOG.debug("Response statusCode: " + statusCode);
		
		switch (statusCode) { 
		case HttpStatus.SC_MOVED_PERMANENTLY: 
		case HttpStatus.SC_MOVED_TEMPORARILY: 
		case HttpStatus.SC_SEE_OTHER: 
		case HttpStatus.SC_TEMPORARY_REDIRECT: { 
			if (redirectCount > ResponseChecker.MAX_REDIRECT_TIMES) { 
				if (LOG.isDebugEnabled()) {
					LOG.debug("Meet max rediect times: " + redirectCount + " > " 
							+ ResponseChecker.MAX_REDIRECT_TIMES);
				}
				
				return false; 
			}
			
			return true; 
		}}
		
		return false; 
	}
	
	@Override 
	public HttpUriRequest createRedirectRequest(String location) { 
		if (location != null && location.indexOf("://") > 0) 
			return new HttpGet(location); 
		
		return null;
	}
	
}
