package org.javenstudio.lightning.http;

public class SimpleHttpListener implements IHttpListener {

	private IHttpResult mResult = null;
	private int mStatusCode = 0;
	private Throwable mException = null;
	
	@Override
	public void onFetched(int statusCode, IHttpResult result, Throwable e) {
		mResult = result;
		mStatusCode = statusCode;
		mException = e;
	}
	
	public IHttpResult getResult() { return mResult; }
	public int getStatusCode() { return mStatusCode; }
	public Throwable getException() { return mException; }
	
}
