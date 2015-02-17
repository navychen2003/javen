package org.javenstudio.cocoka.net.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import org.javenstudio.common.util.Logger;

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
	
	public static String fetchHtml(String location) {
		try {
			HttpClient client = SimpleHttpClient.newInstance("Android", true); 
			HttpGet request = new HttpGet(location);
			HttpResponse response = client.execute(request); 
			
			int statusCode = response.getStatusLine().getStatusCode(); 
			if (statusCode == HttpStatus.SC_OK) {
				return EntityUtils.toString(response.getEntity()); 
				
			} else
				LOG.warn("fetchHtml error: "+statusCode);
			
		} catch (IOException e) {
			LOG.error("fetchHtml error", e);
		}
		
		return null; 
	}
	
	public static byte[] fetchBytes(String location) {
		try {
			HttpClient client = SimpleHttpClient.newInstance("Android", true); 
			HttpGet request = new HttpGet(location);
			HttpResponse response = client.execute(request); 
			
			int statusCode = response.getStatusLine().getStatusCode(); 
			if (statusCode == HttpStatus.SC_OK) {
				return EntityUtils.toByteArray(response.getEntity()); 
				
			} else
				LOG.warn("fetchBytes error: "+statusCode);
			
		} catch (IOException e) {
			LOG.error("fetchBytes error", e);
		}
		
		return null; 
	}
	
}
