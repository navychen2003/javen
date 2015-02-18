package org.javenstudio.lightning.http;

public interface IHttpListener {

	public void onFetched(int statusCode, IHttpResult result, Throwable e);
	
}
