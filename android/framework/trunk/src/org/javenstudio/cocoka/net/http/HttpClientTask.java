package org.javenstudio.cocoka.net.http;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.javenstudio.common.util.Logger;

public abstract class HttpClientTask extends SimpleCallbacks {
	private static final Logger LOG = Logger.getLogger(HttpClientTask.class);
	
	public static interface RequestIniter { 
		public void initRequest(HttpUriRequest request);
	}
	
	public static class HttpJobRequest implements HttpTask.Request {
		public final HttpUriRequest mRequest; 
		public HttpJobRequest(HttpUriRequest request) {
			mRequest = request; 
		}
	}
	
	public static class HttpProgress implements HttpTask.Progress {
		public int mContentLength = -1; 
		public int mReadBytes = 0; 
		
		public HttpProgress(int contentLength) {
			mContentLength = contentLength; 
		} 
		
		public float getProgress() {
			if (mContentLength > 0 && mReadBytes >= 0) 
				return (float)mReadBytes/(float)mContentLength; 
			else 
				return 0.0f; 
		}
	}
	
	private final HttpClient mClient; 
	private final HttpExecutorFactory mFactory; 
	
	public HttpClientTask(HttpClient client, HttpExecutorFactory factory) {
		mFactory = factory; 
		mClient = client; 
	} 
	
	protected abstract void doExecuteRequest(HttpJobRequest[] jobs);
	
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}
	
	public final void executeRequest(String... locations) {
		executeRequest((RequestIniter)null, locations);
	}
	
	public final void executeRequest(RequestIniter initer, String... locations) {
		if (locations == null || locations.length == 0) 
			return; 
		
		HttpUriRequest[] requests = new HttpUriRequest[locations.length]; 
		for (int i=0; i < requests.length; i++) {
			requests[i] = createGetRequest(locations[i]); 
		}
		
		executeRequest(initer, requests); 
	}
	
	public final void executeRequest(HttpUriRequest... requests) {
		executeRequest((RequestIniter)null, requests);
	}
	
	public final void executeRequest(RequestIniter initer, HttpUriRequest... requests) {
		if (requests == null || requests.length == 0) 
			return; 
		
		HttpJobRequest[] jobs = new HttpJobRequest[requests.length]; 
		for (int i=0; i < requests.length; i++) {
			HttpUriRequest request = requests[i];
			if (initer != null && request != null) 
				initer.initRequest(request);
			
			jobs[i] = createJobRequest(request); 
		}
		
		doExecuteRequest(jobs); 
	}
	
	protected HttpGet createGetRequest(String location) { 
		return new HttpGet(location);
	}
	
	protected HttpJobRequest createJobRequest(HttpUriRequest request) { 
		return new HttpJobRequest(request); 
	}
	
	protected HttpTask.Result doExecuteJob(HttpJobRequest request, HttpTask.Publisher publisher) {
		if (request == null || request.mRequest == null) 
			return null; 
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("request URI: " + request.mRequest.getURI());
			
			Header[] headers= request.mRequest.getAllHeaders();
			for (int i=0; headers != null && i < headers.length; i++) { 
				Header header = headers[i];
				if (header == null) continue;
				LOG.debug("request header: " + header.getName() + "=" + header.getValue());
			}
		}
		
		HttpExecutor executor = mFactory.createExecutor(); 
		return executor.execute(mClient, request.mRequest, publisher); 
	}

}
