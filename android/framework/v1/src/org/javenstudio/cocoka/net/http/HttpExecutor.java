package org.javenstudio.cocoka.net.http;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

public interface HttpExecutor {

	public HttpTask.Result execute(HttpClient client, HttpUriRequest request, HttpTask.Publisher publisher); 
	
}
