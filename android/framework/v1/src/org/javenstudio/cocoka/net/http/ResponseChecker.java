package org.javenstudio.cocoka.net.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

public interface ResponseChecker {

	public void checkRequestBeforeExecute(HttpClient client, HttpUriRequest request) throws HttpException; 
	
	public boolean checkResponseSuccess(HttpResponse response); 
	public boolean checkResponseRedirect(HttpResponse response, int redirectCount); 
	
	public HttpUriRequest createRedirectRequest(String location); 
	
}
