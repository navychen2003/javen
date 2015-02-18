package org.javenstudio.lightning.http;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.lightning.Constants;

public class HttpHelper {
	private static Logger LOG = Logger.getLogger(HttpHelper.class);

	private static final long FAILED_EXPIRETIME = 30 * 60 * 1000;
	
	public static class FailedHistory { 
		private final String mSource;
		private final Throwable mException;
		private final long mFetchTime;
		
		public FailedHistory(String source, Throwable e) { 
			mSource = source;
			mException = e;
			mFetchTime = System.currentTimeMillis();
		}
		
		public String getSource() { return mSource; }
		public Throwable getException() { return mException; }
		public long getFetchTime() { return mFetchTime; }
	}
	
	private static final Map<String, FailedHistory> sFailedHistories = 
			new HashMap<String, FailedHistory>();
	
	public static void addFailed(String source, Throwable e) { 
		if (source == null || e == null) 
			return;
		
		synchronized (sFailedHistories) { 
			sFailedHistories.put(source, new FailedHistory(source, e));
		}
	}
	
	public static FailedHistory getFailed(String source) { 
		if (source == null) return null;
		
		synchronized (sFailedHistories) { 
			FailedHistory failed = sFailedHistories.get(source);
			if (failed != null) { 
				long expireTime = System.currentTimeMillis() - failed.mFetchTime;
				if (expireTime < FAILED_EXPIRETIME) 
					return failed;
				
				//sFailedHistories.remove(location);
			}
		}
		
		return null;
	}
	
	public static void removeFailed(String source) { 
		if (source == null) return;
		
		synchronized (sFailedHistories) { 
			sFailedHistories.remove(source);
		}
	}
	
	public static void removeAllFailed() { 
		synchronized (sFailedHistories) { 
			sFailedHistories.clear();
		}
	}
	
	public static void closeConnections() { 
		SimpleHttpClient.closeConnections();
	}
	
	public static String fetchHtml(String location) throws ErrorException {
		if (LOG.isDebugEnabled()) 
			LOG.debug("fetchHtml: url: " + location);
		
		URI uri = URI.create(location);
		SimpleHttpListener listener = new SimpleHttpListener();
		fetchURL(uri, listener);
		
		IHttpResult entity = listener.getResult();
		if (entity != null) {
			return entity.getContentAsString();
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Response entity is null");
		}
	}
	
	public static byte[] fetchBytes(String location) throws ErrorException {
		if (LOG.isDebugEnabled()) 
			LOG.debug("fetchBytes: url: " + location);
		
		URI uri = URI.create(location);
		SimpleHttpListener listener = new SimpleHttpListener();
		fetchURL(uri, listener);
		
		IHttpResult entity = listener.getResult();
		if (entity != null) {
			return entity.getContentAsBinary();
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Response entity is null");
		}
	}
	
	public static IHttpResult fetchURL(URI location) throws ErrorException {
		SimpleHttpListener listener = new SimpleHttpListener();
		fetchURL(location, listener);
		return listener.getResult();
	}
	
	public static void fetchURL(URI location, IHttpListener listener) 
			throws ErrorException {
		if (location == null || listener == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("fetchURL: url: " + location);
		
		SimpleHttpClient client = SimpleHttpClient.newInstance(Constants.HTTP_USER_AGENT); 
		HttpGet request = new HttpGet(location);
		IHttpResult result = null;
		int statusCode = -1;
		Throwable exception = null;
		
		try {
			HttpResponse response = client.execute(request); 
			if (response == null) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"HttpResponse is null");
			}
			
			statusCode = response.getStatusLine().getStatusCode(); 
			HttpEntity entity = response.getEntity(); 
			if (entity != null) result = new SimpleHttpResult(location, response, entity);
			
			if (statusCode == HttpStatus.SC_OK) {
				if (result == null) {
					exception = new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Response entity is null");
				}
			} else {
				//String ver = response.getStatusLine().getProtocolVersion();
				String reason = response.getStatusLine().getReasonPhrase();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("fetchURL: url: " + location 
							+ " error: " + response.getStatusLine());
				}
				
				exception = new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Response error: " + statusCode + " " + reason);
			}
		} catch (IOException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("fetchURL: url: " + location 
						+ " error: " + e, e);
			}
			
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Request error: " + e.toString());
		} finally {
			if (client != null) 
				client.close();
		}
		
		if (listener != null)
			listener.onFetched(statusCode, result, exception);
	}
	
}
