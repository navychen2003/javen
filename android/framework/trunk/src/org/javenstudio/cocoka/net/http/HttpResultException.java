package org.javenstudio.cocoka.net.http;

public class HttpResultException extends HttpException {
	
	private static final long serialVersionUID = 1L;
	
	private final HttpResult mResult;
	
	public HttpResultException(HttpResult result, int sc, String message) { 
		super(sc, message, null); 
		mResult = result;
	}
	
	public HttpResultException(HttpResult result, int sc, String message, Throwable e) { 
		super(sc, message, e);
		mResult = result;
	}
	
	@Override
	public final HttpResult getHttpResult() {
		return mResult;
	}
	
}
