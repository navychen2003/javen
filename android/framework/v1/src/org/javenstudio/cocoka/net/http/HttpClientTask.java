package org.javenstudio.cocoka.net.http;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import org.javenstudio.cocoka.worker.WorkerTask;
import org.javenstudio.cocoka.worker.queue.AdvancedQueueFactory;

public abstract class HttpClientTask extends SimpleCallbacks {

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
	
	private final AsyncTaskImpl mTask; 
	private final HttpClient mClient; 
	private final HttpExecutorFactory mFactory; 
	
	public HttpClientTask(HttpClient client, HttpExecutorFactory factory) {
		this(client, factory, null, null); 
	}
	
	public HttpClientTask(HttpClient client, HttpExecutorFactory factory, 
			AdvancedQueueFactory queueFactory) {
		this(client, factory, queueFactory, null); 
	}
	
	public HttpClientTask(HttpClient client, HttpExecutorFactory factory, 
			AdvancedQueueFactory queueFactory, AsyncTaskImpl.Callbacks callbacks) {
		mFactory = factory; 
		mClient = client; 
		mTask = new AsyncTaskImpl(
			queueFactory, 
			new HttpTask() {
				public HttpTask.Result execute(HttpTask.Request request, HttpTask.Publisher publisher) {
					return doExecuteJob((HttpJobRequest)request, publisher); 
				}
				public WorkerTask.WorkerInfo getWorkerInfo() { 
					return HttpClientTask.this.getWorkerInfo(); 
				}
			}, 
			(callbacks != null ? callbacks : this));
	} 
	
	public abstract WorkerTask.WorkerInfo getWorkerInfo(); 
	
	public final boolean cancel(boolean mayInterruptIfRunning) {
		return mTask.cancel(mayInterruptIfRunning); 
	}
	
	public final void executeRequest(String... locations) {
		if (locations == null || locations.length == 0) 
			return; 
		
		HttpUriRequest[] requests = new HttpUriRequest[locations.length]; 
		for (int i=0; i < requests.length; i++) {
			requests[i] = new HttpGet(locations[i]); 
		}
		
		executeRequest(requests); 
	}
	
	public final void executeRequest(HttpUriRequest... requests) {
		if (requests == null || requests.length == 0) 
			return; 
		
		HttpJobRequest[] jobs = new HttpJobRequest[requests.length]; 
		for (int i=0; i < requests.length; i++) {
			jobs[i] = createJobRequest(requests[i]); 
		}
		
		mTask.execute(jobs); 
	}
	
	protected HttpJobRequest createJobRequest(HttpUriRequest request) { 
		return new HttpJobRequest(request); 
	}
	
	private HttpTask.Result doExecuteJob(HttpJobRequest request, HttpTask.Publisher publisher) {
		if (request == null) return null; 
		
		HttpExecutor executor = mFactory.createExecutor(); 
		return executor.execute(mClient, request.mRequest, publisher); 
	}

}
