package org.javenstudio.lightning.http;

import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;

public interface IHttpContext {

	public HttpClientConnectionManager getConnectionManager();
	public RequestConfig getDefaultRequestConfig();
	public CredentialsProvider getCredentialsProvider();
	public CookieStore getCookieStore();
	
	
}
