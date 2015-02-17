package org.javenstudio.cocoka.net.http;

import org.apache.http.client.HttpClient;
import org.javenstudio.cocoka.net.http.HttpTask.Progress;

public class SimpleHttpClientTask extends HttpClientTask {

	private final HttpTask.Publisher mPublisher;
	private final HttpCallbacks mCallbacks;
	
	public SimpleHttpClientTask(HttpClient client, HttpExecutorFactory factory) { 
		this(client, factory, null);
	}
	
	public SimpleHttpClientTask(HttpClient client, HttpExecutorFactory factory, 
			HttpCallbacks callbacks) { 
		this(client, factory, callbacks, new HttpTask.Publisher() {
				@Override
				public boolean isAborted() {
					return false;
				}
				@Override
				public void publish(Progress progress) {
				}
			});
	}
	
	public SimpleHttpClientTask(HttpClient client, HttpExecutorFactory factory, 
			HttpCallbacks callbacks, HttpTask.Publisher publisher) {
		super(client, factory);
		mCallbacks = callbacks;
		mPublisher = publisher;
	}
	
	protected void doExecuteRequest(HttpJobRequest[] jobs) { 
		if (jobs == null) return;
		final HttpCallbacks callbacks = mCallbacks;
		
		for (HttpJobRequest request : jobs) { 
			if (callbacks != null) 
				callbacks.onJobStart(request);
			
			HttpTask.Result result = doExecuteJob(request, mPublisher);
			
			if (callbacks != null) 
				callbacks.onJobFinished(request, result);
		}
		
		if (callbacks != null) 
			callbacks.onAllFinished();
	}
	
}
