package org.javenstudio.cocoka.net.http.download;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.ResponseChecker;

public class DownloadResponseChecker implements ResponseChecker {

	@Override 
	public void checkRequestBeforeExecute(HttpClient client, HttpUriRequest request) throws HttpException { 
		// do nothing
	}
	
	@Override 
	public boolean checkResponseSuccess(HttpResponse response) { 
		if (response == null) return false; 
		
		int statusCode = response.getStatusLine().getStatusCode(); 
		if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_PARTIAL_CONTENT) 
			return true; 
		else
			return false; 
	}
	
	@Override 
	public boolean checkResponseRedirect(HttpResponse response, int redirectCount) { 
		if (response == null) return false; 
		
		int statusCode = response.getStatusLine().getStatusCode(); 
		if (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307) { 
			if (redirectCount > 2) return false; 
			return true; 
			
		} else
			return false; 
	}
	
	@Override 
	public HttpUriRequest createRedirectRequest(String location) { 
		if (location != null && location.indexOf("://") > 0) 
			return new HttpGet(location); 
		
		return null;
	}
	
}
